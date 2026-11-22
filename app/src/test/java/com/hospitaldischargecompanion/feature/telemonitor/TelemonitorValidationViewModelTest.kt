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
import com.hospitaldischargecompanion.core.demo.TemperatureUnit
import com.hospitaldischargecompanion.core.discharge.DefaultDischargeEpisodeRepository
import com.hospitaldischargecompanion.core.discharge.MockDischargeApi
import com.hospitaldischargecompanion.core.i18n.BloodPressureCategory
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
class TelemonitorValidationViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun convertsFahrenheitToCelsiusOnSubmitAndClassifiesDraftBp() = runTest {
        val store = MockClinicalStore()
        val sim = DemoNetworkSimulator().apply { setLatencyMs(0L) }
        val clock = FixedClinicalClock()
        val ids = DeterministicIdGenerator()
        val alertRepo = DefaultAlertRepository(MockAlertApi(store, sim, clock, ids))
        val vm = TelemonitoringViewModel(
            savedStateHandle = SavedStateHandle(mapOf("episodeId" to ClinicalSeedFactory.HEART_FAILURE_EPISODE_ID)),
            sessionRepository = FakeDemoSessionRepository(DemoSessionState(temperatureUnit = TemperatureUnit.FAHRENHEIT)),
            episodeRepository = DefaultDischargeEpisodeRepository(MockDischargeApi(store, sim)),
            telemonitoringRepository = DefaultTelemonitoringRepository(
                MockTelemonitoringApi(store, sim, clock, ids),
                MockWearableIntegrationHook()
            ),
            evaluateAlertRules = EvaluateAlertRulesUseCase(alertRepo),
            idGenerator = ids
        )

        val job = launch { vm.uiState.collect() }
        advanceUntilIdle()

        vm.fillNormalBaselinePreset()
        advanceUntilIdle()
        assertThat(vm.uiState.value.draftBpCategory).isEqualTo(BloodPressureCategory.NORMAL)

        vm.submitCheckIn()
        advanceUntilIdle()

        val saved = vm.uiState.value.checkIns.last()
        assertThat(saved.temperatureC).isWithin(0.15).of(36.7)
        assertThat(vm.uiState.value.triggeredAlerts).isEmpty()
        assertThat(vm.uiState.value.errorMessage).isNull()
        job.cancel()
    }
}
