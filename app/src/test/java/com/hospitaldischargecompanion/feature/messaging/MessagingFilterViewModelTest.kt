package com.hospitaldischargecompanion.feature.messaging

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.hospitaldischargecompanion.core.alerts.DefaultAlertRepository
import com.hospitaldischargecompanion.core.alerts.MockAlertApi
import com.hospitaldischargecompanion.core.demo.ClinicalSeedFactory
import com.hospitaldischargecompanion.core.demo.DemoNetworkSimulator
import com.hospitaldischargecompanion.core.demo.DemoSessionState
import com.hospitaldischargecompanion.core.demo.MockClinicalStore
import com.hospitaldischargecompanion.core.demo.UserRole
import com.hospitaldischargecompanion.core.discharge.DefaultDischargeEpisodeRepository
import com.hospitaldischargecompanion.core.discharge.MockDischargeApi
import com.hospitaldischargecompanion.core.messaging.DefaultMessagingRepository
import com.hospitaldischargecompanion.core.messaging.MockMessagingApi
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
class MessagingFilterViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun filtersThreadByClinicalNoteFlagAndSearchQuery() = runTest {
        val store = MockClinicalStore()
        val sim = DemoNetworkSimulator().apply { setLatencyMs(0L) }
        val clock = FixedClinicalClock()
        val ids = DeterministicIdGenerator()
        val vm = MessagingViewModel(
            savedStateHandle = SavedStateHandle(mapOf("episodeId" to ClinicalSeedFactory.HEART_FAILURE_EPISODE_ID)),
            sessionRepository = FakeDemoSessionRepository(DemoSessionState(activeRole = UserRole.NURSE)),
            episodeRepository = DefaultDischargeEpisodeRepository(MockDischargeApi(store, sim)),
            alertRepository = DefaultAlertRepository(MockAlertApi(store, sim, clock, ids)),
            messagingRepository = DefaultMessagingRepository(MockMessagingApi(store, sim, clock, ids))
        )

        val job = launch { vm.uiState.collect() }
        advanceUntilIdle()

        assertThat(vm.uiState.value.messages).hasSize(3)

        vm.toggleClinicalNotesOnly()
        advanceUntilIdle()
        assertThat(vm.uiState.value.messages).hasSize(1)
        assertThat(vm.uiState.value.messages.first().isClinicalNote).isTrue()

        vm.toggleClinicalNotesOnly()
        vm.updateSearchQuery("pill box")
        advanceUntilIdle()
        assertThat(vm.uiState.value.messages).hasSize(1)
        assertThat(vm.uiState.value.messages.first().senderName).contains("Maria")
        job.cancel()
    }
}
