package com.hospitaldischargecompanion.feature.telemonitor

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.hospitaldischargecompanion.core.alerts.DefaultAlertRepository
import com.hospitaldischargecompanion.core.alerts.EvaluateAlertRulesUseCase
import com.hospitaldischargecompanion.core.alerts.MockAlertApi
import com.hospitaldischargecompanion.core.demo.ClinicalSeedFactory
import com.hospitaldischargecompanion.core.demo.DemoNetworkSimulator
import com.hospitaldischargecompanion.core.demo.DemoSessionState
import com.hospitaldischargecompanion.core.demo.MockClinicalStore
import com.hospitaldischargecompanion.core.demo.UserRole
import com.hospitaldischargecompanion.core.discharge.CarePlanTaskStatus
import com.hospitaldischargecompanion.core.discharge.DefaultCarePlanRepository
import com.hospitaldischargecompanion.core.discharge.DefaultDischargeEpisodeRepository
import com.hospitaldischargecompanion.core.discharge.MockCarePlanApi
import com.hospitaldischargecompanion.core.discharge.MockDischargeApi
import com.hospitaldischargecompanion.core.medication.DefaultMedicationRepository
import com.hospitaldischargecompanion.core.medication.DoseActionType
import com.hospitaldischargecompanion.core.medication.MockMedicationApi
import com.hospitaldischargecompanion.core.telemonitor.DefaultTelemonitoringRepository
import com.hospitaldischargecompanion.core.telemonitor.MockTelemonitoringApi
import com.hospitaldischargecompanion.core.telemonitor.MockWearableIntegrationHook
import com.hospitaldischargecompanion.feature.careplan.CarePlanViewModel
import com.hospitaldischargecompanion.feature.medications.MedicationViewModel
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
class ClinicalFeatureViewModelsTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun abnormalBpPresetInTelemonitoringTriggersClinicalAlerts() = runTest {
        val store = MockClinicalStore()
        val sim = DemoNetworkSimulator().apply { setLatencyMs(0L) }
        val clock = FixedClinicalClock()
        val ids = DeterministicIdGenerator()
        val alertRepo = DefaultAlertRepository(MockAlertApi(store, sim, clock, ids))
        val evalUseCase = EvaluateAlertRulesUseCase(alertRepo)
        val teleRepo = DefaultTelemonitoringRepository(
            MockTelemonitoringApi(store, sim, clock, ids),
            MockWearableIntegrationHook()
        )
        val vm = TelemonitoringViewModel(
            savedStateHandle = SavedStateHandle(mapOf("episodeId" to ClinicalSeedFactory.HEART_FAILURE_EPISODE_ID)),
            sessionRepository = FakeDemoSessionRepository(DemoSessionState(activeRole = UserRole.PATIENT)),
            episodeRepository = DefaultDischargeEpisodeRepository(MockDischargeApi(store, sim)),
            telemonitoringRepository = teleRepo,
            evaluateAlertRules = evalUseCase,
            idGenerator = ids
        )

        val job = launch { vm.uiState.collect() }
        advanceUntilIdle()

        vm.fillOutOfRangeBpPreset()
        vm.submitCheckIn()
        advanceUntilIdle()

        assertThat(vm.uiState.value.triggeredAlerts).isNotEmpty()
        assertThat(vm.uiState.value.errorMessage).isNull()
        assertThat(alertRepo.observeEpisodeAlerts(ClinicalSeedFactory.HEART_FAILURE_EPISODE_ID).first().size)
            .isGreaterThan(2)
        job.cancel()
    }

    @Test
    fun medicationSkipCreatesMissedDoseAlertAndCarePlanCompletesTask() = runTest {
        val store = MockClinicalStore()
        val sim = DemoNetworkSimulator().apply { setLatencyMs(0L) }
        val clock = FixedClinicalClock()
        val ids = DeterministicIdGenerator()
        val alertRepo = DefaultAlertRepository(MockAlertApi(store, sim, clock, ids))
        val medRepo = DefaultMedicationRepository(MockMedicationApi(store, sim, clock, ids))
        val careRepo = DefaultCarePlanRepository(MockCarePlanApi(store, sim, clock, ids))
        val epRepo = DefaultDischargeEpisodeRepository(MockDischargeApi(store, sim))
        val session = FakeDemoSessionRepository(DemoSessionState(activeRole = UserRole.FAMILY))

        val medVm = MedicationViewModel(
            savedStateHandle = SavedStateHandle(mapOf("episodeId" to ClinicalSeedFactory.HEART_FAILURE_EPISODE_ID)),
            sessionRepository = session,
            episodeRepository = epRepo,
            medicationRepository = medRepo,
            evaluateAlertRules = EvaluateAlertRulesUseCase(alertRepo),
            idGenerator = ids
        )
        val careVm = CarePlanViewModel(
            savedStateHandle = SavedStateHandle(mapOf("episodeId" to ClinicalSeedFactory.HEART_FAILURE_EPISODE_ID)),
            sessionRepository = session,
            episodeRepository = epRepo,
            carePlanRepository = careRepo
        )

        val j1 = launch { medVm.uiState.collect() }
        val j2 = launch { careVm.uiState.collect() }
        advanceUntilIdle()

        val furosemide = medVm.uiState.value.medications.first { it.id == "med-hf-furosemide" }
        medVm.recordDose(furosemide, DoseActionType.SKIPPED, "Dizziness")
        careVm.completeTask("task-hf-2")
        careVm.exportSummary()
        advanceUntilIdle()

        assertThat(medVm.uiState.value.doseLogs.first().attributionLabel).contains("Maria Silva")
        assertThat(alertRepo.observeEpisodeAlerts(ClinicalSeedFactory.HEART_FAILURE_EPISODE_ID).first().any {
            it.title.contains("Furosemide")
        }).isTrue()
        assertThat(careVm.uiState.value.tasks.first { it.id == "task-hf-2" }.status)
            .isEqualTo(CarePlanTaskStatus.COMPLETED)
        assertThat(careVm.uiState.value.exportedSummary).contains("CARE PLAN SUMMARY")

        j1.cancel()
        j2.cancel()
    }
}
