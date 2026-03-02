package com.hospitaldischargecompanion.core.discharge

import com.google.common.truth.Truth.assertThat
import com.hospitaldischargecompanion.core.demo.ClinicalSeedFactory
import com.hospitaldischargecompanion.core.demo.DemoNetworkSimulator
import com.hospitaldischargecompanion.core.demo.DomainError
import com.hospitaldischargecompanion.core.demo.DomainResult
import com.hospitaldischargecompanion.core.demo.MockClinicalStore
import com.hospitaldischargecompanion.core.demo.UserRole
import com.hospitaldischargecompanion.testing.DeterministicIdGenerator
import com.hospitaldischargecompanion.testing.FixedClinicalClock
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class CarePlanRepositoryTest {

    private lateinit var store: MockClinicalStore
    private lateinit var repository: CarePlanRepository

    @Before
    fun setUp() {
        store = MockClinicalStore()
        val simulator = DemoNetworkSimulator().apply { setLatencyMs(0L) }
        repository = DefaultCarePlanRepository(
            MockCarePlanApi(
                store = store,
                networkSimulator = simulator,
                clock = FixedClinicalClock(),
                idGenerator = DeterministicIdGenerator()
            )
        )
    }

    @Test
    fun completesTaskAndRejectsDuplicateCompletion() = runTest {
        val first = repository.completeTask("task-hf-2", "João Silva")
        assertThat(first).isInstanceOf(DomainResult.Success::class.java)
        assertThat((first as DomainResult.Success).data.status).isEqualTo(CarePlanTaskStatus.COMPLETED)

        val second = repository.completeTask("task-hf-2", "João Silva")
        assertThat(second).isEqualTo(DomainResult.Failure(DomainError.TaskAlreadyCompleted("task-hf-2")))
    }

    @Test
    fun requiresReasonOnSkipAndRestrictsReopenToClinicians() = runTest {
        val blankSkip = repository.skipTask("task-hf-4", "   ", "João Silva")
        assertThat(blankSkip).isInstanceOf(DomainResult.Failure::class.java)

        val validSkip = repository.skipTask("task-hf-4", "Feeling nauseated", "João Silva")
        assertThat((validSkip as DomainResult.Success).data.status).isEqualTo(CarePlanTaskStatus.SKIPPED)

        val patientReopen = repository.reopenTask("task-hf-4", UserRole.PATIENT)
        assertThat(patientReopen).isEqualTo(
            DomainResult.Failure(DomainError.UnauthorizedRoleAction(UserRole.PATIENT, "reopen_care_plan_task"))
        )

        val nurseReopen = repository.reopenTask("task-hf-4", UserRole.NURSE, "Family Caregiver")
        assertThat((nurseReopen as DomainResult.Success).data.status).isEqualTo(CarePlanTaskStatus.DUE)
        assertThat(nurseReopen.data.assignedToLabel).isEqualTo("Family Caregiver")
    }

    @Test
    fun physicianAmendmentIncrementsRevisionAndAddsNewTask() = runTest {
        val nurseAttempt = repository.amendCarePlan(
            episodeId = ClinicalSeedFactory.HEART_FAILURE_EPISODE_ID,
            actorRole = UserRole.NURSE,
            authorName = "RN Clara",
            changeSummary = "Unauthorized edit"
        )
        assertThat(nurseAttempt).isInstanceOf(DomainResult.Failure::class.java)

        val result = repository.amendCarePlan(
            episodeId = ClinicalSeedFactory.HEART_FAILURE_EPISODE_ID,
            actorRole = UserRole.PHYSICIAN,
            authorName = "Dr. Elena Vance, MD",
            changeSummary = "Add evening weight recheck due to elevated BP",
            newTaskTitle = "Evening weight recheck",
            newTaskInstructions = "Reweigh at 20:00 and report gain > 1 kg",
            newTaskType = CarePlanTaskType.VITAL_CHECK
        )

        val revision = (result as DomainResult.Success).data
        assertThat(revision.revisionNumber).isEqualTo(2)
        assertThat(revision.signedOff).isTrue()

        val tasks = repository.observeTasks(ClinicalSeedFactory.HEART_FAILURE_EPISODE_ID).first()
        assertThat(tasks.any { it.title == "Evening weight recheck" && it.addedInRevision == 2 }).isTrue()
        assertThat(store.getEpisode(ClinicalSeedFactory.HEART_FAILURE_EPISODE_ID)?.carePlanRevision).isEqualTo(2)
    }
}
