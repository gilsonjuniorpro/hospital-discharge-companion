package com.hospitaldischargecompanion.feature.physician

import com.google.common.truth.Truth.assertThat
import com.hospitaldischargecompanion.core.alerts.DefaultAlertRepository
import com.hospitaldischargecompanion.core.alerts.MockAlertApi
import com.hospitaldischargecompanion.core.demo.ClinicalSeedFactory
import com.hospitaldischargecompanion.core.demo.DemoNetworkSimulator
import com.hospitaldischargecompanion.core.demo.DemoSessionState
import com.hospitaldischargecompanion.core.demo.MockClinicalStore
import com.hospitaldischargecompanion.core.demo.UserRole
import com.hospitaldischargecompanion.core.discharge.DefaultCarePlanRepository
import com.hospitaldischargecompanion.core.discharge.DefaultDischargeEpisodeRepository
import com.hospitaldischargecompanion.core.discharge.MockCarePlanApi
import com.hospitaldischargecompanion.core.discharge.MockDischargeApi
import com.hospitaldischargecompanion.core.medication.DefaultMedicationRepository
import com.hospitaldischargecompanion.core.medication.MockMedicationApi
import com.hospitaldischargecompanion.core.messaging.DefaultMessagingRepository
import com.hospitaldischargecompanion.core.messaging.MockMessagingApi
import com.hospitaldischargecompanion.core.notifications.MockNotificationCenterRepository
import com.hospitaldischargecompanion.core.telemonitor.DefaultTelemonitoringRepository
import com.hospitaldischargecompanion.core.telemonitor.MockTelemonitoringApi
import com.hospitaldischargecompanion.core.telemonitor.MockWearableIntegrationHook
import com.hospitaldischargecompanion.feature.nurse.NurseCensusFilter
import com.hospitaldischargecompanion.feature.nurse.NurseWorkstationViewModel
import com.hospitaldischargecompanion.testing.DeterministicIdGenerator
import com.hospitaldischargecompanion.testing.FakeDemoSessionRepository
import com.hospitaldischargecompanion.testing.FixedClinicalClock
import com.hospitaldischargecompanion.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ClinicianWorkstationsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun nurseEscalatesToPhysicianAndPhysicianAmendsCarePlanWithSignOff() = runTest {
        val store = MockClinicalStore()
        val sim = DemoNetworkSimulator().apply { setLatencyMs(0L) }
        val clock = FixedClinicalClock()
        val ids = DeterministicIdGenerator()
        val session = FakeDemoSessionRepository(DemoSessionState(activeRole = UserRole.NURSE))
        val epRepo = DefaultDischargeEpisodeRepository(MockDischargeApi(store, sim))
        val careRepo = DefaultCarePlanRepository(MockCarePlanApi(store, sim, clock, ids))
        val medRepo = DefaultMedicationRepository(MockMedicationApi(store, sim, clock, ids))
        val teleRepo = DefaultTelemonitoringRepository(
            MockTelemonitoringApi(store, sim, clock, ids),
            MockWearableIntegrationHook()
        )
        val alertRepo = DefaultAlertRepository(MockAlertApi(store, sim, clock, ids))
        val msgRepo = DefaultMessagingRepository(MockMessagingApi(store, sim, clock, ids))
        val notifRepo = MockNotificationCenterRepository(store, clock, ids)

        val nurseVm = NurseWorkstationViewModel(epRepo, careRepo, medRepo, teleRepo, alertRepo, msgRepo, notifRepo)
        val physicianVm = PhysicianWorkstationViewModel(session, epRepo, careRepo, medRepo, teleRepo, alertRepo)

        val j1 = launch { nurseVm.uiState.collect() }
        val j2 = launch { physicianVm.uiState.collect() }
        advanceUntilIdle()

        nurseVm.setFilter(NurseCensusFilter.HIGH_RISK)
        nurseVm.escalateToPhysician("alert-hf-weight")
        nurseVm.requestRecheckAndDocumentNote(ClinicalSeedFactory.HEART_FAILURE_EPISODE_ID, "João Silva")
        advanceUntilIdle()

        assertThat(nurseVm.uiState.value.census).hasSize(1)
        assertThat(physicianVm.uiState.value.escalatedQueue.any { it.id == "alert-hf-weight" }).isTrue()

        physicianVm.selectDisposition(ClinicalDisposition.EARLIER_FOLLOW_UP)
        physicianVm.amendAndSignOffCarePlan(
            changeSummary = "Add evening dry weight and move cardiology clinic earlier",
            newTaskTitle = "Evening dry-weight check",
            newRedFlag = "Gain >1.5 kg overnight"
        )
        advanceUntilIdle()

        assertThat(physicianVm.uiState.value.selectedEpisode?.carePlanRevision).isEqualTo(2)
        assertThat(careRepo.observeTasks(ClinicalSeedFactory.HEART_FAILURE_EPISODE_ID).first().any {
            it.title == "Evening dry-weight check"
        }).isTrue()

        j1.cancel()
        j2.cancel()
    }
}
