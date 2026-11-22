package com.hospitaldischargecompanion.core.discharge

import com.hospitaldischargecompanion.core.demo.DomainResult
import com.hospitaldischargecompanion.core.demo.UserRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

interface CarePlanRepository {
    fun observeTasks(episodeId: String): Flow<List<CarePlanTask>>
    fun observeAllTasks(): Flow<List<CarePlanTask>>
    fun observeRevisions(episodeId: String): Flow<List<CarePlanRevision>>
    suspend fun completeTask(taskId: String, actorLabel: String): DomainResult<CarePlanTask>
    suspend fun skipTask(taskId: String, reason: String, actorLabel: String): DomainResult<CarePlanTask>
    suspend fun reopenTask(taskId: String, actorRole: UserRole, reassignTo: String? = null): DomainResult<CarePlanTask>
    suspend fun addFollowUpTask(task: CarePlanTask): DomainResult<CarePlanTask>
    suspend fun amendCarePlan(
        episodeId: String,
        actorRole: UserRole,
        authorName: String,
        changeSummary: String,
        newTaskTitle: String? = null,
        newTaskInstructions: String? = null,
        newTaskType: CarePlanTaskType = CarePlanTaskType.VITAL_CHECK,
        newRedFlag: String? = null,
        dispositionNote: String? = null
    ): DomainResult<CarePlanRevision>
    fun buildExportSummary(episode: DischargeEpisode, tasks: List<CarePlanTask>, revisions: List<CarePlanRevision>): String
}

@Singleton
class DefaultCarePlanRepository @Inject constructor(
    private val dataSource: CarePlanDataSource
) : CarePlanRepository {

    override fun observeTasks(episodeId: String): Flow<List<CarePlanTask>> =
        dataSource.observeAllTasks().map { list ->
            list.filter { it.episodeId == episodeId }.sortedBy { it.dayNumber }
        }

    override fun observeAllTasks(): Flow<List<CarePlanTask>> = dataSource.observeAllTasks()

    override fun observeRevisions(episodeId: String): Flow<List<CarePlanRevision>> =
        dataSource.observeAllRevisions().map { list ->
            list.filter { it.episodeId == episodeId }.sortedByDescending { it.revisionNumber }
        }

    override suspend fun completeTask(taskId: String, actorLabel: String): DomainResult<CarePlanTask> =
        dataSource.completeTask(taskId, actorLabel)

    override suspend fun skipTask(taskId: String, reason: String, actorLabel: String): DomainResult<CarePlanTask> =
        dataSource.skipTask(taskId, reason, actorLabel)

    override suspend fun reopenTask(taskId: String, actorRole: UserRole, reassignTo: String?): DomainResult<CarePlanTask> =
        dataSource.reopenTask(taskId, actorRole, reassignTo)

    override suspend fun addFollowUpTask(task: CarePlanTask): DomainResult<CarePlanTask> =
        dataSource.addTask(task)

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
    ): DomainResult<CarePlanRevision> = dataSource.amendCarePlan(
        episodeId = episodeId,
        actorRole = actorRole,
        authorName = authorName,
        changeSummary = changeSummary,
        newTaskTitle = newTaskTitle,
        newTaskInstructions = newTaskInstructions,
        newTaskType = newTaskType,
        newRedFlag = newRedFlag,
        dispositionNote = dispositionNote
    )

    override fun buildExportSummary(
        episode: DischargeEpisode,
        tasks: List<CarePlanTask>,
        revisions: List<CarePlanRevision>
    ): String = buildString {
        appendLine("HOSPITAL DISCHARGE COMPANION — CARE PLAN SUMMARY (DEMO)")
        appendLine("Patient: ${episode.patientName} (${episode.mrnMasked})")
        appendLine("Diagnosis: ${episode.diagnosisTitle}")
        appendLine("Discharge Date: ${episode.dischargeDateIso} · Unit: ${episode.hospitalUnit}")
        appendLine("Attending: ${episode.attendingPhysician} · Primary Nurse: ${episode.primaryNurse}")
        appendLine("Care Plan Revision: v${episode.carePlanRevision} · Risk Tier: ${episode.riskTier}")
        appendLine()
        appendLine("TASKS:")
        tasks.forEach { task ->
            appendLine("- [Day ${task.dayNumber}] (${task.status}) ${task.title}: ${task.instructions}")
        }
        appendLine()
        appendLine("RED FLAGS:")
        episode.redFlags.forEach { flag -> appendLine("- $flag") }
        appendLine()
        appendLine("REVISIONS:")
        revisions.forEach { rev ->
            appendLine("- v${rev.revisionNumber} by ${rev.authorName}: ${rev.changeSummary}")
        }
    }
}
