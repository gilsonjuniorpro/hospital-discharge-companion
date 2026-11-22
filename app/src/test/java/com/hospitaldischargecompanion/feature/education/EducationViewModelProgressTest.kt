package com.hospitaldischargecompanion.feature.education

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.hospitaldischargecompanion.core.demo.ClinicalSeedFactory
import com.hospitaldischargecompanion.core.demo.DemoNetworkSimulator
import com.hospitaldischargecompanion.core.demo.MockClinicalStore
import com.hospitaldischargecompanion.core.education.DefaultEducationRepository
import com.hospitaldischargecompanion.core.education.MockEducationApi
import com.hospitaldischargecompanion.testing.FakeDemoSessionRepository
import com.hospitaldischargecompanion.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EducationViewModelProgressTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun updatesEducationCompletionPercentageAcrossQuizAndManualCompletion() = runTest {
        val store = MockClinicalStore()
        val sim = DemoNetworkSimulator().apply { setLatencyMs(0L) }
        val vm = EducationViewModel(
            savedStateHandle = SavedStateHandle(mapOf("episodeId" to ClinicalSeedFactory.HEART_FAILURE_EPISODE_ID)),
            sessionRepository = FakeDemoSessionRepository(),
            educationRepository = DefaultEducationRepository(MockEducationApi(store, sim))
        )

        val job = launch { vm.uiState.collect() }
        advanceUntilIdle()

        assertThat(vm.uiState.value.completionPercent).isEqualTo(0)

        vm.answerQuiz("edu-hf-weight", 0)
        advanceUntilIdle()
        assertThat(vm.uiState.value.completedCount).isEqualTo(1)
        assertThat(vm.uiState.value.completionPercent).isEqualTo(50)

        vm.markCompleted("edu-hf-sodium")
        advanceUntilIdle()
        assertThat(vm.uiState.value.completionPercent).isEqualTo(100)
        job.cancel()
    }
}
