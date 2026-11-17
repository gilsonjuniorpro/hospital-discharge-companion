package com.hospitaldischargecompanion.feature.careplan

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
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
class CarePlanReassignTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun nurseReopensAndReassignsCompletedTaskWhilePatientIsDenied() = runTest {
        val store = MockClinicalStore()
        val sim = DemoNetworkSimulator().apply { setLatencyMs(0L) }
        val session = FakeDemoSessionRepository(DemoSessionState(activeRole = UserRole.PATIENT))
        val vm = CarePlanViewModel(
            savedStateHandle = SavedStateHandle(mapOf("episodeId" to ClinicalSeedFactory.HEART_FAILURE_EPISODE_ID)),
            sessionRepository = session,
            episodeRepository = DefaultDischargeEpisodeRepository(MockDischargeApi(store, sim)),
            carePlanRepository = DefaultCarePlanRepository(
                MockCarePlanApi(store, sim, FixedClinicalClock(), DeterministicIdGenerator())
            )
        )

        val job = launch { vm.uiState.collect() }
        advanceUntilIdle()

        vm.reopenTask("task-hf-1", "Family Caregiver")
        advanceUntilIdle()
        assertThat(vm.uiState.value.errorMessage).contains("not permitted")

        session.setActiveRole(UserRole.NURSE)
        advanceUntilIdle()
        vm.reopenTask("task-hf-1", "Home Health Nurse")
        advanceUntilIdle()

        val reopened = vm.uiState.value.tasks.first { it.id == "task-hf-1" }
        assertThat(reopened.status).isEqualTo(CarePlanTaskStatus.DUE)
        assertThat(reopened.assignedToLabel).isEqualTo("Home Health Nurse")
        assertThat(vm.uiState.value.errorMessage).isNull()
        job.cancel()
    }
}
