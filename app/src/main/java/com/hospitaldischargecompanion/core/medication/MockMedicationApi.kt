package com.hospitaldischargecompanion.core.medication

import com.hospitaldischargecompanion.core.demo.ClinicalClock
import com.hospitaldischargecompanion.core.demo.DemoNetworkSimulator
import com.hospitaldischargecompanion.core.demo.DomainError
import com.hospitaldischargecompanion.core.demo.DomainResult
import com.hospitaldischargecompanion.core.demo.IdGenerator
import com.hospitaldischargecompanion.core.demo.MockClinicalStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

interface MedicationDataSource {
    fun observeAllMedications(): Flow<List<MedicationItem>>
    fun observeDoseLogs(): Flow<List<DoseLogEntry>>
    fun observeWarnings(): Flow<List<MedicationInteractionWarning>>
    suspend fun recordDoseAction(
        medicationId: String,
        action: DoseActionType,
        attributionLabel: String,
        reasonOrNote: String?,
        idempotencyKey: String
    ): DomainResult<DoseLogEntry>
    suspend fun attachMockPillBoxPhoto(medicationId: String, label: String): DomainResult<MedicationItem>
    suspend fun requestRefill(medicationId: String): DomainResult<MedicationItem>
}

@Singleton
class MockMedicationApi @Inject constructor(
    store: MockClinicalStore,
    private val networkSimulator: DemoNetworkSimulator,
    private val clock: ClinicalClock,
    private val idGenerator: IdGenerator
) : MedicationDataSource {

    private val _medications = MutableStateFlow(MedicationSeedFactory.createMedications())
    private val _logs = MutableStateFlow<List<DoseLogEntry>>(emptyList())
    private val _warnings = MutableStateFlow(MedicationSeedFactory.createInteractionWarnings())

    init {
        store.registerResetCallback {
            _medications.value = MedicationSeedFactory.createMedications()
            _logs.value = emptyList()
            _warnings.value = MedicationSeedFactory.createInteractionWarnings()
        }
    }

    override fun observeAllMedications(): Flow<List<MedicationItem>> = _medications.asStateFlow()
    override fun observeDoseLogs(): Flow<List<DoseLogEntry>> = _logs.asStateFlow()
    override fun observeWarnings(): Flow<List<MedicationInteractionWarning>> = _warnings.asStateFlow()

    override suspend fun recordDoseAction(
        medicationId: String,
        action: DoseActionType,
        attributionLabel: String,
        reasonOrNote: String?,
        idempotencyKey: String
    ): DomainResult<DoseLogEntry> = networkSimulator.execute {
        val existingByKey = _logs.value.firstOrNull { it.idempotencyKey == idempotencyKey }
        if (existingByKey != null) {
            return@execute DomainResult.Success(existingByKey)
        }
        val med = _medications.value.firstOrNull { it.id == medicationId }
            ?: return@execute DomainResult.Failure(
                DomainError.ValidationFailed("medicationId", "Medication $medicationId not found")
            )
        if (med.changeStatus == MedicationChangeStatus.STOPPED || med.doseWindowClosed) {
            return@execute DomainResult.Failure(DomainError.MedicationDoseWindowClosed(medicationId))
        }

        val nowIso = clock.now().toString()
        val entry = DoseLogEntry(
            id = idGenerator.nextId("dose"),
            medicationId = medicationId,
            episodeId = med.episodeId,
            action = action,
            attributionLabel = attributionLabel,
            reasonOrNote = reasonOrNote?.takeIf { it.isNotBlank() },
            timestampIso = nowIso,
            idempotencyKey = idempotencyKey
        )
        _logs.update { listOf(entry) + it }
        _medications.update { list ->
            list.map {
                if (it.id == medicationId) {
                    it.copy(
                        todayDoseStatus = action,
                        lastActionAttribution = attributionLabel,
                        lastActionTimeIso = nowIso
                    )
                } else {
                    it
                }
            }
        }
        DomainResult.Success(entry)
    }

    override suspend fun attachMockPillBoxPhoto(
        medicationId: String,
        label: String
    ): DomainResult<MedicationItem> = networkSimulator.execute {
        var updated: MedicationItem? = null
        _medications.update { list ->
            list.map {
                if (it.id == medicationId) {
                    it.copy(
                        hasPillBoxAttachment = true,
                        pillBoxAttachmentLabel = label
                    ).also { item -> updated = item }
                } else {
                    it
                }
            }
        }
        updated?.let { DomainResult.Success(it) }
            ?: DomainResult.Failure(DomainError.ValidationFailed("medicationId", "Medication not found"))
    }

    override suspend fun requestRefill(medicationId: String): DomainResult<MedicationItem> =
        networkSimulator.execute {
            var updated: MedicationItem? = null
            _medications.update { list ->
                list.map {
                    if (it.id == medicationId) {
                        it.copy(
                            refillStatus = RefillStatus.IN_STOCK,
                            daysSupplyRemaining = 30
                        ).also { item -> updated = item }
                    } else {
                        it
                    }
                }
            }
            updated?.let { DomainResult.Success(it) }
                ?: DomainResult.Failure(DomainError.ValidationFailed("medicationId", "Medication not found"))
        }
}
