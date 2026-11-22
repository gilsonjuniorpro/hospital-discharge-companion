package com.hospitaldischargecompanion.core.ehr

import com.hospitaldischargecompanion.core.alerts.AlertRepository
import com.hospitaldischargecompanion.core.alerts.AlertSeverity
import com.hospitaldischargecompanion.core.alerts.AlertType
import com.hospitaldischargecompanion.core.alerts.EscalationStage
import com.hospitaldischargecompanion.core.demo.ClinicalClock
import com.hospitaldischargecompanion.core.demo.DemoNetworkSimulator
import com.hospitaldischargecompanion.core.demo.DomainError
import com.hospitaldischargecompanion.core.demo.DomainResult
import com.hospitaldischargecompanion.core.demo.IdGenerator
import com.hospitaldischargecompanion.core.demo.MockClinicalStore
import com.hospitaldischargecompanion.core.demo.SimulatedFailureMode
import com.hospitaldischargecompanion.core.demo.UserRole
import com.hospitaldischargecompanion.core.discharge.EhrSyncStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

interface EhrSyncDataSource {
    fun observeSyncLogs(): Flow<List<EhrSyncLogEntry>>
    fun observeConflicts(): Flow<List<EhrConflictRecord>>
    suspend fun pullDischargeSummary(episodeId: String): DomainResult<EhrSyncLogEntry>
    suspend fun pushObservationsAndAdherence(episodeId: String): DomainResult<EhrSyncLogEntry>
    suspend fun resolveConflict(conflictId: String, choice: ConflictResolutionChoice): DomainResult<EhrConflictRecord>
}

@Singleton
class MockEhrSyncApi @Inject constructor(
    private val store: MockClinicalStore,
    private val alertRepository: AlertRepository,
    private val networkSimulator: DemoNetworkSimulator,
    private val clock: ClinicalClock,
    private val idGenerator: IdGenerator
) : EhrSyncDataSource {

    private val _logs = MutableStateFlow(
        listOf(
            EhrSyncLogEntry(
                id = "sync-seed-1",
                episodeId = "ep-hf-001",
                direction = "PULL",
                outcomeStatus = EhrSyncStatus.SYNCED,
                detail = "Pulled FHIR Patient, Encounter, CarePlan v1, and 4 MedicationRequests",
                timestampIso = "2026-06-15T08:15:00Z"
            )
        )
    )
    private val _conflicts = MutableStateFlow<List<EhrConflictRecord>>(emptyList())

    init {
        store.registerResetCallback {
            _logs.value = listOf(
                EhrSyncLogEntry(
                    id = "sync-seed-1",
                    episodeId = "ep-hf-001",
                    direction = "PULL",
                    outcomeStatus = EhrSyncStatus.SYNCED,
                    detail = "Pulled FHIR Patient, Encounter, CarePlan v1, and 4 MedicationRequests",
                    timestampIso = "2026-06-15T08:15:00Z"
                )
            )
            _conflicts.value = emptyList()
        }
    }

    override fun observeSyncLogs(): Flow<List<EhrSyncLogEntry>> = _logs.asStateFlow()
    override fun observeConflicts(): Flow<List<EhrConflictRecord>> = _conflicts.asStateFlow()

    override suspend fun pullDischargeSummary(episodeId: String): DomainResult<EhrSyncLogEntry> =
        executeEhrOperation(
            episodeId = episodeId,
            direction = "PULL",
            successDetail = "Pulled latest FHIR Encounter, CarePlan, and MedicationRequest bundle"
        )

    override suspend fun pushObservationsAndAdherence(episodeId: String): DomainResult<EhrSyncLogEntry> =
        executeEhrOperation(
            episodeId = episodeId,
            direction = "PUSH",
            successDetail = "Pushed home Observation vitals, MedicationAdministration logs, and CarePlan updates"
        )

    private suspend fun executeEhrOperation(
        episodeId: String,
        direction: String,
        successDetail: String
    ): DomainResult<EhrSyncLogEntry> {
        val now = clock.now().toString()
        val result = networkSimulator.execute(isEhrOperation = true, resourceId = "Observation/$episodeId") {
            if (store.getEpisode(episodeId) == null) {
                DomainResult.Failure(DomainError.EpisodeNotFound(episodeId))
            } else {
                DomainResult.Success(Unit)
            }
        }

        return when (result) {
            is DomainResult.Success -> {
                store.updateEpisode(episodeId) {
                    it.copy(ehrSyncStatus = EhrSyncStatus.SYNCED, lastEhrSyncIso = now)
                }
                val entry = EhrSyncLogEntry(
                    id = idGenerator.nextId("sync"),
                    episodeId = episodeId,
                    direction = direction,
                    outcomeStatus = EhrSyncStatus.SYNCED,
                    detail = successDetail,
                    timestampIso = now
                )
                _logs.update { listOf(entry) + it }
                DomainResult.Success(entry)
            }

            is DomainResult.Failure -> {
                val status = if (result.error is DomainError.EhrSyncConflict) {
                    EhrSyncStatus.CONFLICT
                } else {
                    EhrSyncStatus.FAILED
                }
                store.updateEpisode(episodeId) { it.copy(ehrSyncStatus = status) }
                val entry = EhrSyncLogEntry(
                    id = idGenerator.nextId("sync"),
                    episodeId = episodeId,
                    direction = direction,
                    outcomeStatus = status,
                    detail = "${result.error.code}: ${result.error.detail}",
                    timestampIso = now
                )
                _logs.update { listOf(entry) + it }

                if (result.error is DomainError.EhrSyncConflict) {
                    val conflict = EhrConflictRecord(
                        id = idGenerator.nextId("conf"),
                        episodeId = episodeId,
                        resourceType = "Observation (Blood Pressure & Weight)",
                        localSummary = "Home check-in recorded locally: BP 172/104 mmHg, Weight 78.8 kg",
                        ehrUpstreamSummary = "Hospital EHR clinic triage entry: BP 148/88 mmHg, Weight 77.6 kg",
                        detectedAtIso = now
                    )
                    _conflicts.update { listOf(conflict) + it }
                    alertRepository.raiseAlert(
                        episodeId = episodeId,
                        type = AlertType.EHR_SYNC_CONFLICT,
                        severity = AlertSeverity.WARNING,
                        escalationStage = EscalationStage.NURSE_TRIAGE,
                        title = "EHR sync conflict (HTTP 409) on home Observation",
                        summary = "Local home vitals differ from upstream EHR observation version. Review in EHR Sync panel.",
                        patientGuidance = "Your readings are safely saved on this device while the care team reconciles records.",
                        actorName = "EHR Sync Adapter",
                        actorRole = UserRole.NURSE,
                        reasonCode = "EHR_CONFLICT_409"
                    )
                }
                result
            }
        }
    }

    override suspend fun resolveConflict(
        conflictId: String,
        choice: ConflictResolutionChoice
    ): DomainResult<EhrConflictRecord> {
        val current = _conflicts.value.firstOrNull { it.id == conflictId }
            ?: return DomainResult.Failure(DomainError.ValidationFailed("conflictId", "Conflict not found"))
        val resolved = current.copy(resolvedChoice = choice)
        _conflicts.update { list -> list.map { if (it.id == conflictId) resolved else it } }

        // Clear simulated 409 after user explicitly resolves so subsequent sync succeeds cleanly
        if (networkSimulator.config.value.failureMode == SimulatedFailureMode.EHR_CONFLICT_409) {
            networkSimulator.setFailureMode(SimulatedFailureMode.NONE)
        }
        val now = clock.now().toString()
        store.updateEpisode(current.episodeId) {
            it.copy(ehrSyncStatus = EhrSyncStatus.SYNCED, lastEhrSyncIso = now)
        }
        _logs.update {
            listOf(
                EhrSyncLogEntry(
                    id = idGenerator.nextId("sync"),
                    episodeId = current.episodeId,
                    direction = "RECONCILE",
                    outcomeStatus = EhrSyncStatus.SYNCED,
                    detail = "Resolved ${current.resourceType} conflict using $choice",
                    timestampIso = now
                )
            ) + it
        }
        return DomainResult.Success(resolved)
    }
}
