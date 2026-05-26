package com.hospitaldischargecompanion.core.ehr

import com.hospitaldischargecompanion.core.demo.DomainResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

interface EhrSyncRepository {
    fun observeLogs(episodeId: String): Flow<List<EhrSyncLogEntry>>
    fun observeOpenConflicts(episodeId: String): Flow<List<EhrConflictRecord>>
    suspend fun pullDischargeSummary(episodeId: String): DomainResult<EhrSyncLogEntry>
    suspend fun pushObservationsAndAdherence(episodeId: String): DomainResult<EhrSyncLogEntry>
    suspend fun resolveConflict(conflictId: String, choice: ConflictResolutionChoice): DomainResult<EhrConflictRecord>
}

@Singleton
class DefaultEhrSyncRepository @Inject constructor(
    private val dataSource: EhrSyncDataSource
) : EhrSyncRepository {

    override fun observeLogs(episodeId: String): Flow<List<EhrSyncLogEntry>> =
        dataSource.observeSyncLogs().map { list -> list.filter { it.episodeId == episodeId } }

    override fun observeOpenConflicts(episodeId: String): Flow<List<EhrConflictRecord>> =
        dataSource.observeConflicts().map { list ->
            list.filter { it.episodeId == episodeId && it.resolvedChoice == null }
        }

    override suspend fun pullDischargeSummary(episodeId: String): DomainResult<EhrSyncLogEntry> =
        dataSource.pullDischargeSummary(episodeId)

    override suspend fun pushObservationsAndAdherence(episodeId: String): DomainResult<EhrSyncLogEntry> =
        dataSource.pushObservationsAndAdherence(episodeId)

    override suspend fun resolveConflict(
        conflictId: String,
        choice: ConflictResolutionChoice
    ): DomainResult<EhrConflictRecord> = dataSource.resolveConflict(conflictId, choice)
}
