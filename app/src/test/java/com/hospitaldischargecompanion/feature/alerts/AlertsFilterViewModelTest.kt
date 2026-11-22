package com.hospitaldischargecompanion.feature.alerts

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.hospitaldischargecompanion.core.alerts.AlertStatus
import com.hospitaldischargecompanion.core.alerts.DefaultAlertRepository
import com.hospitaldischargecompanion.core.alerts.EscalationStage
import com.hospitaldischargecompanion.core.alerts.MockAlertApi
import com.hospitaldischargecompanion.core.demo.ClinicalSeedFactory
import com.hospitaldischargecompanion.core.demo.DemoNetworkSimulator
import com.hospitaldischargecompanion.core.demo.DemoSessionState
import com.hospitaldischargecompanion.core.demo.MockClinicalStore
import com.hospitaldischargecompanion.core.demo.UserRole
import com.hospitaldischargecompanion.core.discharge.DefaultDischargeEpisodeRepository
import com.hospitaldischargecompanion.core.discharge.MockDischargeApi
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
class AlertsFilterViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun filtersByAlertStatusAndEscalatesAllTheWayToAdviseErCopyOnly() = runTest {
        val store = MockClinicalStore()
        val sim = DemoNetworkSimulator().apply { setLatencyMs(0L) }
        val vm = AlertsViewModel(
            savedStateHandle = SavedStateHandle(mapOf("episodeId" to ClinicalSeedFactory.HEART_FAILURE_EPISODE_ID)),
            sessionRepository = FakeDemoSessionRepository(DemoSessionState(activeRole = UserRole.NURSE)),
            episodeRepository = DefaultDischargeEpisodeRepository(MockDischargeApi(store, sim)),
            alertRepository = DefaultAlertRepository(
                MockAlertApi(store, sim, FixedClinicalClock(), DeterministicIdGenerator())
            )
        )

        val job = launch { vm.uiState.collect() }
        advanceUntilIdle()

        vm.escalate("alert-hf-weight") // NURSE -> PHYSICIAN
        vm.escalate("alert-hf-weight") // PHYSICIAN -> ADVISE_ER_COPY_ONLY
        advanceUntilIdle()

        vm.setFilter(AlertListFilter.ESCALATED)
        advanceUntilIdle()
        assertThat(vm.uiState.value.alerts).hasSize(1)
        assertThat(vm.uiState.value.alerts.first().escalationStage).isEqualTo(EscalationStage.ADVISE_ER_COPY_ONLY)

        vm.resolve("alert-hf-weight")
        vm.setFilter(AlertListFilter.RESOLVED)
        advanceUntilIdle()
        assertThat(vm.uiState.value.alerts).hasSize(1)
        assertThat(vm.uiState.value.alerts.first().status).isEqualTo(AlertStatus.RESOLVED)
        job.cancel()
    }
}
