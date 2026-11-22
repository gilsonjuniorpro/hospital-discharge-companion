package com.hospitaldischargecompanion.feature.demo

import com.google.common.truth.Truth.assertThat
import com.hospitaldischargecompanion.core.alerts.DefaultAlertRepository
import com.hospitaldischargecompanion.core.alerts.EvaluateAlertRulesUseCase
import com.hospitaldischargecompanion.core.alerts.MockAlertApi
import com.hospitaldischargecompanion.core.demo.ClinicalSeedFactory
import com.hospitaldischargecompanion.core.demo.DemoNetworkSimulator
import com.hospitaldischargecompanion.core.demo.DomainResult
import com.hospitaldischargecompanion.core.demo.MockClinicalStore
import com.hospitaldischargecompanion.core.demo.SimulatedFailureMode
import com.hospitaldischargecompanion.core.demo.UserRole
import com.hospitaldischargecompanion.core.discharge.CarePlanTaskType
import com.hospitaldischargecompanion.core.discharge.DefaultCarePlanRepository
import com.hospitaldischargecompanion.core.discharge.EhrSyncStatus
import com.hospitaldischargecompanion.core.discharge.MockCarePlanApi
import com.hospitaldischargecompanion.core.ehr.ConflictResolutionChoice
import com.hospitaldischargecompanion.core.ehr.DefaultEhrSyncRepository
import com.hospitaldischargecompanion.core.ehr.MockEhrSyncApi
import com.hospitaldischargecompanion.core.medication.DefaultMedicationRepository
import com.hospitaldischargecompanion.core.medication.DoseActionType
import com.hospitaldischargecompanion.core.medication.MockMedicationApi
import com.hospitaldischargecompanion.core.messaging.DefaultMessagingRepository
import com.hospitaldischargecompanion.core.messaging.MockMessagingApi
import com.hospitaldischargecompanion.core.notifications.MockNotificationCenterRepository
import com.hospitaldischargecompanion.core.telemonitor.DefaultTelemonitoringRepository
import com.hospitaldischargecompanion.core.telemonitor.MockTelemonitoringApi
import com.hospitaldischargecompanion.core.telemonitor.MockWearableIntegrationHook
import com.hospitaldischargecompanion.core.telemonitor.NewCheckInRequest
import com.hospitaldischargecompanion.testing.DeterministicIdGenerator
import com.hospitaldischargecompanion.testing.FakeDemoSessionRepository
import com.hospitaldischargecompanion.testing.FixedClinicalClock
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * Verifies the full multi-role live presentation loop on the shared offline mock backend:
 * Patient meds & abnormal BP -> Alert -> Family escalation -> Nurse triage -> Physician care-plan sign-off -> EHR 409 conflict & reconcile -> Demo reset.
 */
class FullClinicalLoopTest {

    @Test
    fun executesCompleteFourRoleHeartFailureLoopAndResetsCleanly() = runTest {
        val store = MockClinicalStore()
        val simulator = DemoNetworkSimulator().apply { setLatencyMs(0L) }
        val clock = FixedClinicalClock()
        val ids = DeterministicIdGenerator()
        val session = FakeDemoSessionRepository()

        val carePlanRepo = DefaultCarePlanRepository(MockCarePlanApi(store, simulator, clock, ids))
        val medRepo = DefaultMedicationRepository(MockMedicationApi(store, simulator, clock, ids))
        val teleRepo = DefaultTelemonitoringRepository(
            MockTelemonitoringApi(store, simulator, clock, ids),
            MockWearableIntegrationHook()
        )
        val alertRepo = DefaultAlertRepository(MockAlertApi(store, simulator, clock, ids))
        val evalUseCase = EvaluateAlertRulesUseCase(alertRepo)
        val msgRepo = DefaultMessagingRepository(MockMessagingApi(store, simulator, clock, ids))
        val ehrRepo = DefaultEhrSyncRepository(MockEhrSyncApi(store, alertRepo, simulator, clock, ids))
        val notifRepo = MockNotificationCenterRepository(store, clock, ids)

        val epId = ClinicalSeedFactory.HEART_FAILURE_EPISODE_ID

        // 1. Patient confirms morning meds & logs abnormal BP + weight gain
        val doseRes = medRepo.recordDose("med-hf-furosemide", DoseActionType.TAKEN, "João Silva", idempotencyKey = "k-1")
        assertThat(doseRes).isInstanceOf(DomainResult.Success::class.java)

        val checkInRes = teleRepo.submitCheckIn(
            NewCheckInRequest(
                episodeId = epId,
                systolicMmHg = 172,
                diastolicMmHg = 104,
                heartRateBpm = 94,
                spo2Percent = 93,
                weightKg = 78.8,
                temperatureC = 36.8,
                painScore = 2,
                dyspneaSeverity = 3,
                swellingSeverity = 3,
                freeTextNotes = "Elevated BP and weight up",
                recordedByLabel = "João Silva",
                idempotencyKey = "k-bp-1"
            )
        )
        val checkIn = (checkInRes as DomainResult.Success).data
        val baseline = teleRepo.observeBaseline(epId).first()
        val generatedAlerts = evalUseCase.evaluateCheckIn(checkIn, baseline)
        assertThat(generatedAlerts).isNotEmpty()
        val bpAlertId = generatedAlerts.first().id

        // 2. Family sees alert and escalates
        val familyEsc = alertRepo.escalate(bpAlertId, "Maria Silva", UserRole.FAMILY, "FAMILY_CONCERN")
        assertThat(familyEsc).isInstanceOf(DomainResult.Success::class.java)

        // 3. Nurse documents clinical note and verifies queue
        val noteRes = msgRepo.sendMessage(
            episodeId = epId,
            senderName = "RN Clara Mendes",
            senderRole = UserRole.NURSE,
            body = "Clinical Note: Escalated elevated BP 172/104 to attending physician.",
            isClinicalNote = true
        )
        assertThat(noteRes).isInstanceOf(DomainResult.Success::class.java)
        assertThat(alertRepo.observePhysicianEscalatedQueue().first().any { it.id == bpAlertId }).isTrue()

        // 4. Physician amends care plan (extra evening weight check) and signs off v2
        val revRes = carePlanRepo.amendCarePlan(
            episodeId = epId,
            actorRole = UserRole.PHYSICIAN,
            authorName = "Dr. Elena Vance, MD",
            changeSummary = "Add evening dry weight check",
            newTaskTitle = "Evening dry weight check",
            newTaskType = CarePlanTaskType.VITAL_CHECK
        )
        assertThat((revRes as DomainResult.Success).data.revisionNumber).isEqualTo(2)
        assertThat(store.getEpisode(epId)?.carePlanRevision).isEqualTo(2)

        // 5. EHR sync simulates 409 conflict, reconciles, and returns to SYNCED
        simulator.setFailureMode(SimulatedFailureMode.EHR_CONFLICT_409)
        val conflictPush = ehrRepo.pushObservationsAndAdherence(epId)
        assertThat(conflictPush).isInstanceOf(DomainResult.Failure::class.java)
        assertThat(store.getEpisode(epId)?.ehrSyncStatus).isEqualTo(EhrSyncStatus.CONFLICT)

        val conflict = ehrRepo.observeOpenConflicts(epId).first().first()
        ehrRepo.resolveConflict(conflict.id, ConflictResolutionChoice.KEEP_LOCAL_HOME)
        assertThat(store.getEpisode(epId)?.ehrSyncStatus).isEqualTo(EhrSyncStatus.SYNCED)

        // 6. Demo reset clears mutations across every repository
        store.resetAll()
        simulator.reset()
        session.resetSession()
        assertThat(store.getEpisode(epId)?.carePlanRevision).isEqualTo(1)
        assertThat(medRepo.observeDoseLogs(epId).first()).isEmpty()
        assertThat(ehrRepo.observeOpenConflicts(epId).first()).isEmpty()
        assertThat(notifRepo.observeNotifications().first()).hasSize(3)
    }
}
