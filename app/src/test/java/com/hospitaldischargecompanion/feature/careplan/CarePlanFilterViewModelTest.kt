package com.hospitaldischargecompanion.feature.careplan

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.hospitaldischargecompanion.core.demo.ClinicalSeedFactory
import com.hospitaldischargecompanion.core.demo.DemoNetworkSimulator
import com.hospitaldischargecompanion.core.demo.MockClinicalStore
import com.hospitaldischargecompanion.core.discharge.CarePlanTaskStatus
import com.hospitaldischargecompanion.core.discharge.DefaultCarePlanRepository
import com.hospitaldischargecompanion.core.discharge.DefaultDischargeEpisodeRepository
import com.hospitaldischargecompanion.core.discharge.MockCarePlanApi
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
class CarePlanFilterViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun filtersTasksByStatusAndComputesCompletionPercentage() = runTest {
        val store = MockClinicalStore()
        val sim = DemoNetworkSimulator().apply { setLatencyMs(0L) }
        val vm = CarePlanViewModel(
            savedStateHandle = SavedStateHandle(mapOf("episodeId" to ClinicalSeedFactory.HEART_FAILURE_EPISODE_ID)),
            sessionRepository = FakeDemoSessionRepository(),
            episodeRepository = DefaultDischargeEpisodeRepository(MockDischargeApi(store, sim)),
            carePlanRepository = DefaultCarePlanRepository(
                MockCarePlanApi(store, sim, FixedClinicalClock(), DeterministicIdGenerator())
            )
        )

        val job = launch { vm.uiState.collect() }
        advanceUntilIdle()

        assertThat(vm.uiState.value.completedCount).isEqualTo(1)
        assertThat(vm.uiState.value.totalCount).isEqualTo(7)
        assertThat(vm.uiState.value.completionPercent).isEqualTo(14)

        vm.selectStatusFilter(CarePlanTaskStatus.OVERDUE)
        advanceUntilIdle()
        assertThat(vm.uiState.value.tasks).hasSize(1)
        assertThat(vm.uiState.value.tasks.first().status).isEqualTo(CarePlanTaskStatus.OVERDUE)

        vm.selectStatusFilter(null)
        vm.completeTask("task-hf-2")
        advanceUntilIdle()
        assertThat(vm.uiState.value.completedCount).isEqualTo(2)
        assertThat(vm.uiState.value.completionPercent).isEqualTo(28)
        job.cancel()
    }
}
