package com.hospitaldischargecompanion.feature.patient

import com.google.common.truth.Truth.assertThat
import com.hospitaldischargecompanion.core.alerts.DefaultAlertRepository
import com.hospitaldischargecompanion.core.alerts.MockAlertApi
import com.hospitaldischargecompanion.core.demo.ClinicalSeedFactory
import com.hospitaldischargecompanion.core.demo.DemoNetworkSimulator
import com.hospitaldischargecompanion.core.demo.MockClinicalStore
import com.hospitaldischargecompanion.core.discharge.DefaultCarePlanRepository
import com.hospitaldischargecompanion.core.discharge.DefaultDischargeEpisodeRepository
import com.hospitaldischargecompanion.core.discharge.MockCarePlanApi
import com.hospitaldischargecompanion.core.discharge.MockDischargeApi
import com.hospitaldischargecompanion.core.medication.DefaultMedicationRepository
import com.hospitaldischargecompanion.core.medication.MockMedicationApi
import com.hospitaldischargecompanion.core.telemonitor.DefaultTelemonitoringRepository
import com.hospitaldischargecompanion.core.telemonitor.MockTelemonitoringApi
import com.hospitaldischargecompanion.core.telemonitor.MockWearableIntegrationHook
import com.hospitaldischargecompanion.testing.DeterministicIdGenerator
import com.hospitaldischargecompanion.testing.FakeDemoSessionRepository
import com.hospitaldischargecompanion.testing.FixedClinicalClock
import com.hospitaldischargecompanion.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PatientHomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun aggregatesEpisodeTasksAdherenceVitalsAndAlertsAndSwitchesEpisode() = runTest {
        val store = MockClinicalStore()
        val sim = DemoNetworkSimulator().apply { setLatencyMs(0L) }
        val clock = FixedClinicalClock()
        val ids = DeterministicIdGenerator()
        val session = FakeDemoSessionRepository()

        val vm = PatientHomeViewModel(
            sessionRepository = session,
            episodeRepository = DefaultDischargeEpisodeRepository(MockDischargeApi(store, sim)),
            carePlanRepository = DefaultCarePlanRepository(MockCarePlanApi(store, sim, clock, ids)),
            medicationRepository = DefaultMedicationRepository(MockMedicationApi(store, sim, clock, ids)),
            telemonitoringRepository = DefaultTelemonitoringRepository(
                MockTelemonitoringApi(store, sim, clock, ids),
                MockWearableIntegrationHook()
            ),
            alertRepository = DefaultAlertRepository(MockAlertApi(store, sim, clock, ids))
        )

        val job = launch { vm.uiState.collect() }
        advanceUntilIdle()

        assertThat(vm.uiState.value.episode?.id).isEqualTo(ClinicalSeedFactory.HEART_FAILURE_EPISODE_ID)
        assertThat(vm.uiState.value.totalTasksCount).isEqualTo(7)
        assertThat(vm.uiState.value.openAlerts).isNotEmpty()
        assertThat(vm.uiState.value.latestCheckIn?.weightKg).isEqualTo(77.6)

        vm.selectEpisode(ClinicalSeedFactory.PNEUMONIA_EPISODE_ID)
        advanceUntilIdle()
        assertThat(vm.uiState.value.episode?.id).isEqualTo(ClinicalSeedFactory.PNEUMONIA_EPISODE_ID)
        job.cancel()
    }
}
