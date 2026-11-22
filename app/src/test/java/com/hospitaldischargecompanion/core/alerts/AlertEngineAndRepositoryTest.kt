package com.hospitaldischargecompanion.core.alerts

import com.google.common.truth.Truth.assertThat
import com.hospitaldischargecompanion.core.demo.ClinicalSeedFactory
import com.hospitaldischargecompanion.core.demo.DemoNetworkSimulator
import com.hospitaldischargecompanion.core.demo.DomainError
import com.hospitaldischargecompanion.core.demo.DomainResult
import com.hospitaldischargecompanion.core.demo.MockClinicalStore
import com.hospitaldischargecompanion.core.demo.UserRole
import com.hospitaldischargecompanion.core.telemonitor.VitalBaseline
import com.hospitaldischargecompanion.core.telemonitor.VitalCheckIn
import com.hospitaldischargecompanion.testing.DeterministicIdGenerator
import com.hospitaldischargecompanion.testing.FixedClinicalClock
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class AlertEngineAndRepositoryTest {

    private lateinit var store: MockClinicalStore
    private lateinit var repository: AlertRepository
    private lateinit var evaluateUseCase: EvaluateAlertRulesUseCase

    @Before
    fun setUp() {
        store = MockClinicalStore()
        val simulator = DemoNetworkSimulator().apply { setLatencyMs(0L) }
        repository = DefaultAlertRepository(
            MockAlertApi(
                store = store,
                networkSimulator = simulator,
                clock = FixedClinicalClock(),
                idGenerator = DeterministicIdGenerator()
            )
        )
        evaluateUseCase = EvaluateAlertRulesUseCase(repository)
    }

    @Test
    fun abnormalBloodPressureAndWeightGainTriggerAlertsIntoQueues() = runTest {
        val checkIn = VitalCheckIn(
            id = "chk-99",
            episodeId = ClinicalSeedFactory.HEART_FAILURE_EPISODE_ID,
            dayNumber = 3,
            timestampIso = "2026-06-15T09:00:00Z",
            systolicMmHg = 184,
            diastolicMmHg = 108,
            heartRateBpm = 98,
            spo2Percent = 91,
            weightKg = 79.0,
            temperatureC = 36.8,
            painScore = 2,
            dyspneaSeverity = 4,
            swellingSeverity = 3,
            freeTextNotes = "Difficulty catching breath",
            recordedByLabel = "João Silva",
            idempotencyKey = "k-bp-high"
        )
        val baseline = VitalBaseline(
            episodeId = ClinicalSeedFactory.HEART_FAILURE_EPISODE_ID,
            dryWeightKg = 76.5,
            targetSystolicMaxMmHg = 140,
            targetSystolicMinMmHg = 95,
            targetSpo2MinPercent = 94,
            targetTempMaxC = 37.8
        )

        val generated = evaluateUseCase.evaluateCheckIn(checkIn, baseline)
        assertThat(generated).hasSize(3)
        assertThat(generated.any { it.severity == AlertSeverity.CRITICAL }).isTrue()

        val physicianQueue = repository.observePhysicianEscalatedQueue().first()
        assertThat(physicianQueue.any { it.episodeId == ClinicalSeedFactory.HEART_FAILURE_EPISODE_ID }).isTrue()
    }

    @Test
    fun escalatesThroughLadderAndRejectsEditsAfterResolve() = runTest {
        val escalated = repository.escalate(
            alertId = "alert-hf-weight",
            actorName = "RN Clara Mendes",
            actorRole = UserRole.NURSE,
            reasonCode = "PERSISTENT_FLUID_GAIN"
        )
        val alert = (escalated as DomainResult.Success).data
        assertThat(alert.escalationStage).isEqualTo(EscalationStage.PHYSICIAN_REVIEW)
        assertThat(alert.auditTrail).hasSize(2)

        val resolved = repository.resolve(
            alertId = "alert-hf-weight",
            actorName = "Dr. Elena Vance, MD",
            actorRole = UserRole.PHYSICIAN,
            resolutionNote = "Added evening weight check"
        )
        assertThat((resolved as DomainResult.Success).data.status).isEqualTo(AlertStatus.RESOLVED)

        val retryAck = repository.acknowledge("alert-hf-weight", "RN Clara Mendes", UserRole.NURSE)
        assertThat(retryAck).isEqualTo(DomainResult.Failure(DomainError.AlertAlreadyResolved("alert-hf-weight")))
    }
}
