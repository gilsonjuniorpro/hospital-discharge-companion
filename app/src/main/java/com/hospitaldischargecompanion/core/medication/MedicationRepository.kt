package com.hospitaldischargecompanion.core.medication

import com.hospitaldischargecompanion.core.demo.DomainResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

interface MedicationRepository {
    fun observeMedications(episodeId: String): Flow<List<MedicationItem>>
    fun observeAllMedications(): Flow<List<MedicationItem>>
    fun observeDoseLogs(episodeId: String): Flow<List<DoseLogEntry>>
    fun observeWarnings(episodeId: String): Flow<List<MedicationInteractionWarning>>
    fun observeAdherence(episodeId: String): Flow<AdherenceSummary>
    suspend fun recordDose(
        medicationId: String,
        action: DoseActionType,
        attributionLabel: String,
        reasonOrNote: String? = null,
        idempotencyKey: String = "$medicationId-${action.name}"
    ): DomainResult<DoseLogEntry>
    suspend fun attachMockPillBoxPhoto(medicationId: String, label: String): DomainResult<MedicationItem>
    suspend fun requestRefill(medicationId: String): DomainResult<MedicationItem>
}

@Singleton
class DefaultMedicationRepository @Inject constructor(
    private val dataSource: MedicationDataSource
) : MedicationRepository {

    override fun observeMedications(episodeId: String): Flow<List<MedicationItem>> =
        dataSource.observeAllMedications().map { list -> list.filter { it.episodeId == episodeId } }

    override fun observeAllMedications(): Flow<List<MedicationItem>> =
        dataSource.observeAllMedications()

    override fun observeDoseLogs(episodeId: String): Flow<List<DoseLogEntry>> =
        dataSource.observeDoseLogs().map { list -> list.filter { it.episodeId == episodeId } }

    override fun observeWarnings(episodeId: String): Flow<List<MedicationInteractionWarning>> =
        dataSource.observeWarnings().map { list -> list.filter { it.episodeId == episodeId } }

    override fun observeAdherence(episodeId: String): Flow<AdherenceSummary> =
        observeMedications(episodeId).map { meds ->
            val active = meds.filter { it.changeStatus != MedicationChangeStatus.STOPPED }
            val taken = active.count { it.todayDoseStatus == DoseActionType.TAKEN }
            val skipped = active.count { it.todayDoseStatus == DoseActionType.SKIPPED }
            val snoozed = active.count { it.todayDoseStatus == DoseActionType.SNOOZED }
            val pct = if (active.isEmpty()) 100 else (taken * 100) / active.size
            AdherenceSummary(
                totalScheduledActive = active.size,
                takenCount = taken,
                skippedCount = skipped,
                snoozedCount = snoozed,
                adherencePercent = pct
            )
        }

    override suspend fun recordDose(
        medicationId: String,
        action: DoseActionType,
        attributionLabel: String,
        reasonOrNote: String?,
        idempotencyKey: String
    ): DomainResult<DoseLogEntry> = dataSource.recordDoseAction(
        medicationId = medicationId,
        action = action,
        attributionLabel = attributionLabel,
        reasonOrNote = reasonOrNote,
        idempotencyKey = idempotencyKey
    )

    override suspend fun attachMockPillBoxPhoto(
        medicationId: String,
        label: String
    ): DomainResult<MedicationItem> = dataSource.attachMockPillBoxPhoto(medicationId, label)

    override suspend fun requestRefill(medicationId: String): DomainResult<MedicationItem> =
        dataSource.requestRefill(medicationId)
}
