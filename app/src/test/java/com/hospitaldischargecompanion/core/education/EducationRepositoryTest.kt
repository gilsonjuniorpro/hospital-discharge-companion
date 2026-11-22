package com.hospitaldischargecompanion.core.education

import com.google.common.truth.Truth.assertThat
import com.hospitaldischargecompanion.core.demo.ClinicalSeedFactory
import com.hospitaldischargecompanion.core.demo.DemoNetworkSimulator
import com.hospitaldischargecompanion.core.demo.DomainResult
import com.hospitaldischargecompanion.core.demo.MockClinicalStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class EducationRepositoryTest {

    private lateinit var repository: EducationRepository

    @Before
    fun setUp() {
        val store = MockClinicalStore()
        val simulator = DemoNetworkSimulator().apply { setLatencyMs(0L) }
        repository = DefaultEducationRepository(MockEducationApi(store, simulator))
    }

    @Test
    fun tracksProgressAndCompletesModuleWhenTeachBackQuizPasses() = runTest {
        val wrong = repository.submitQuizAnswer("edu-hf-weight", 1)
        val (afterWrong, passedWrong) = (wrong as DomainResult.Success).data
        assertThat(passedWrong).isFalse()
        assertThat(afterWrong.quizPassed).isFalse()

        val right = repository.submitQuizAnswer("edu-hf-weight", 0)
        val (afterRight, passedRight) = (right as DomainResult.Success).data
        assertThat(passedRight).isTrue()
        assertThat(afterRight.quizPassed).isTrue()
        assertThat(afterRight.progress).isEqualTo(ModuleProgress.COMPLETED)

        val modules = repository.observeModules(ClinicalSeedFactory.HEART_FAILURE_EPISODE_ID).first()
        assertThat(modules).hasSize(2)
    }
}
