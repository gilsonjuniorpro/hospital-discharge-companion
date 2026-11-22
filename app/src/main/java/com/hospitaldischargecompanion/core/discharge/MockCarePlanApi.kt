package com.hospitaldischargecompanion.core.discharge

import com.hospitaldischargecompanion.core.demo.ClinicalClock
import com.hospitaldischargecompanion.core.demo.DemoNetworkSimulator
import com.hospitaldischargecompanion.core.demo.DomainError
import com.hospitaldischargecompanion.core.demo.DomainResult
import com.hospitaldischargecompanion.core.demo.IdGenerator
import com.hospitaldischargecompanion.core.demo.MockClinicalStore
import com.hospitaldischargecompanion.core.demo.UserRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

interface CarePlanDataSource {
    fun observeAllTasks(): Flow<List<CarePlanTask>>
    fun observeAllRevisions(): Flow<List<CarePlanRevision>>
    suspend fun completeTask(taskId: String, actorLabel: String): DomainResult<CarePlanTask>
    suspend fun skipTask(taskId: String, reason: String, actorLabel: String): DomainResult<CarePlanTask>
    suspend fun reopenTask(taskId: String, actorRole: UserRole, reassignTo: String?): DomainResult<CarePlanTask>
    suspend fun addTask(task: CarePlanTask): DomainResult<CarePlanTask>
    suspend fun amendCarePlan(
        episodeId: String,
        actorRole: UserRole,
        authorName: String,
        changeSummary: String,
        newTaskTitle: String?,
        newTaskInstructions: String?,
        newTaskType: CarePlanTaskType,
        newRedFlag: String?,
        dispositionNote: String?
    ): DomainResult<CarePlanRevision>
}

@Singleton
class MockCarePlanApi @Inject constructor(
    private val store: MockClinicalStore,
    private val networkSimulator: DemoNetworkSimulator,
    private val clock: ClinicalClock,
    private val idGenerator: IdGenerator
) : CarePlanDataSource {

    private val _tasks = MutableStateFlow(CarePlanSeedFactory.createTasks())
    private val _revisions = MutableStateFlow(CarePlanSeedFactory.createRevisions())

    init {
        store.registerResetCallback {
            _tasks.value = CarePlanSeedFactory.createTasks()
            _revisions.value = CarePlanSeedFactory.createRevisions()
        }
    }

    override fun observeAllTasks(): Flow<List<CarePlanTask>> = _tasks.asStateFlow()
    override fun observeAllRevisions(): Flow<List<CarePlanRevision>> = _revisions.asStateFlow()

    override suspend fun completeTask(taskId: String, actorLabel: String): DomainResult<CarePlanTask> =
        networkSimulator.execute {
            val current = _tasks.value.firstOrNull { it.id == taskId }
                ?: return@execute DomainResult.Failure(
                    DomainError.InvalidCarePlanTransition(taskId, "MISSING", "COMPLETED")
                )
            if (current.status == CarePlanTaskStatus.COMPLETED) {
                return@execute DomainResult.Failure(DomainError.TaskAlreadyCompleted(taskId))
            }
            val updated = current.copy(
                status = CarePlanTaskStatus.COMPLETED,
                completedByLabel = actorLabel,
                skipReason = null
            )
            _tasks.update { list -> list.map { if (it.id == taskId) updated else it } }
            DomainResult.Success(updated)
        }

    override suspend fun skipTask(
        taskId: String,
        reason: String,
        actorLabel: String
    ): DomainResult<CarePlanTask> = networkSimulator.execute {
        if (reason.isBlank()) {
            return@execute DomainResult.Failure(
                DomainError.ValidationFailed("skipReason", "Reason is required when skipping a task")
            )
        }
        val current = _tasks.value.firstOrNull { it.id == taskId }
            ?: return@execute DomainResult.Failure(
                DomainError.InvalidCarePlanTransition(taskId, "MISSING", "SKIPPED")
            )
        if (current.status == CarePlanTaskStatus.COMPLETED) {
            return@execute DomainResult.Failure(DomainError.TaskAlreadyCompleted(taskId))
        }
        val updated = current.copy(
            status = CarePlanTaskStatus.SKIPPED,
            completedByLabel = actorLabel,
            skipReason = reason.trim()
        )
        _tasks.update { list -> list.map { if (it.id == taskId) updated else it } }
        DomainResult.Success(updated)
    }

    override suspend fun reopenTask(
        taskId: String,
        actorRole: UserRole,
        reassignTo: String?
    ): DomainResult<CarePlanTask> = networkSimulator.execute {
        if (!actorRole.canReopenTasks) {
            return@execute DomainResult.Failure(
                DomainError.UnauthorizedRoleAction(actorRole, "reopen_care_plan_task")
            )
        }
        val current = _tasks.value.firstOrNull { it.id == taskId }
            ?: return@execute DomainResult.Failure(
                DomainError.InvalidCarePlanTransition(taskId, "MISSING", "DUE")
            )
        val updated = current.copy(
            status = CarePlanTaskStatus.DUE,
            completedByLabel = null,
            skipReason = null,
            assignedToLabel = reassignTo?.takeIf { it.isNotBlank() } ?: current.assignedToLabel
        )
        _tasks.update { list -> list.map { if (it.id == taskId) updated else it } }
        DomainResult.Success(updated)
    }

    override suspend fun addTask(task: CarePlanTask): DomainResult<CarePlanTask> =
        networkSimulator.execute {
            _tasks.update { it + task }
            DomainResult.Success(task)
        }

    override suspend fun amendCarePlan(
        episodeId: String,
        actorRole: UserRole,
        authorName: String,
        changeSummary: String,
        newTaskTitle: String?,
        newTaskInstructions: String?,
        newTaskType: CarePlanTaskType,
        newRedFlag: String?,
        dispositionNote: String?
    ): DomainResult<CarePlanRevision> = networkSimulator.execute {
        if (!actorRole.canModifyCarePlan) {
            return@execute DomainResult.Failure(
                DomainError.UnauthorizedRoleAction(actorRole, "amend_care_plan")
            )
        }
        val episode = store.getEpisode(episodeId)
            ?: return@execute DomainResult.Failure(DomainError.EpisodeNotFound(episodeId))
        if (changeSummary.isBlank()) {
            return@execute DomainResult.Failure(
                DomainError.ValidationFailed("changeSummary", "Amendment summary is required")
            )
        }

        val nextRev = episode.carePlanRevision + 1
        val revision = CarePlanRevision(
            revisionNumber = nextRev,
            episodeId = episodeId,
            authorName = authorName,
            authorRole = actorRole,
            timestampIso = clock.now().toString(),
            changeSummary = changeSummary.trim(),
            signedOff = true
        )

        store.updateEpisode(episodeId) { ep ->
            ep.copy(
                carePlanRevision = nextRev,
                redFlags = if (!newRedFlag.isNullOrBlank()) ep.redFlags + newRedFlag.trim() else ep.redFlags,
                dispositionNote = dispositionNote?.takeIf { it.isNotBlank() } ?: ep.dispositionNote,
                ehrSyncStatus = EhrSyncStatus.PENDING
            )
        }

        if (!newTaskTitle.isNullOrBlank()) {
            val addedTask = CarePlanTask(
                id = idGenerator.nextId("task"),
                episodeId = episodeId,
                dayNumber = episode.dischargeDayNumber,
                scheduledTimeLabel = "Day ${episode.dischargeDayNumber} · Added Rev $nextRev",
                title = newTaskTitle.trim(),
                instructions = newTaskInstructions?.takeIf { it.isNotBlank() } ?: changeSummary.trim(),
                type = newTaskType,
                status = CarePlanTaskStatus.DUE,
                addedInRevision = nextRev
            )
            _tasks.update { listOf(addedTask) + it }
        }

        _revisions.update { listOf(revision) + it }
        DomainResult.Success(revision)
    }
}
