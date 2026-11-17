package com.hospitaldischargecompanion.feature.medications

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.hospitaldischargecompanion.core.alerts.DefaultAlertRepository
import com.hospitaldischargecompanion.core.alerts.EvaluateAlertRulesUseCase
import com.hospitaldischargecompanion.core.alerts.MockAlertApi
import com.hospitaldischargecompanion.core.demo.ClinicalSeedFactory
import com.hospitaldischargecompanion.core.demo.DemoNetworkSimulator
import com.hospitaldischargecompanion.core.demo.MockClinicalStore
import com.hospitaldischargecompanion.core.discharge.DefaultDischargeEpisodeRepository
import com.hospitaldischargecompanion.core.discharge.MockDischargeApi
import com.hospitaldischargecompanion.core.medication.DefaultMedicationRepository
import com.hospitaldischargecompanion.core.medication.DoseActionType
import com.hospitaldischargecompanion.core.medication.MockMedicationApi
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
class MedicationSkipReasonTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun skipReasonPropagatesIntoDoseLogAndMissedMedicationAlert() = runTest {
        val store = MockClinicalStore()
        val sim = DemoNetworkSimulator().apply { setLatencyMs(0L) }
        val clock = FixedClinicalClock()
        val ids = DeterministicIdGenerator()
        val alertRepo = DefaultAlertRepository(MockAlertApi(store, sim, clock, ids))
        val vm = MedicationViewModel(
            savedStateHandle = SavedStateHandle(mapOf("episodeId" to ClinicalSeedFactory.HEART_FAILURE_EPISODE_ID)),
            sessionRepository = FakeDemoSessionRepository(),
            episodeRepository = DefaultDischargeEpisodeRepository(MockDischargeApi(store, sim)),
            medicationRepository = DefaultMedicationRepository(MockMedicationApi(store, sim, clock, ids)),
            evaluateAlertRules = EvaluateAlertRulesUseCase(alertRepo),
            idGenerator = ids
        )

        val job = launch { vm.uiState.collect() }
        advanceUntilIdle()

        val med = vm.uiState.value.medications.first { it.id == "med-hf-entresto" }
        vm.recordDose(med, DoseActionType.SKIPPED, "Dizziness / lightheaded")
        advanceUntilIdle()

        assertThat(vm.uiState.value.doseLogs.first().reasonOrNote).isEqualTo("Dizziness / lightheaded")
        val alerts = alertRepo.observeEpisodeAlerts(ClinicalSeedFactory.HEART_FAILURE_EPISODE_ID).first()
        assertThat(alerts.any { it.summary.contains("Dizziness / lightheaded") }).isTrue()
        job.cancel()
    }
}
