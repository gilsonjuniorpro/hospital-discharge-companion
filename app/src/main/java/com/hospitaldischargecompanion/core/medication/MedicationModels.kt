package com.hospitaldischargecompanion.core.medication

enum class MedicationChangeStatus {
    NEW,
    CONTINUED,
    CHANGED,
    STOPPED
}

enum class DoseActionType {
    PENDING,
    TAKEN,
    SKIPPED,
    SNOOZED
}

enum class RefillStatus {
    IN_STOCK,
    REFILL_SOON,
    OUT_OF_STOCK
}

data class MedicationItem(
    val id: String,
    val episodeId: String,
    val name: String,
    val strengthAndForm: String,
    val scheduleLabel: String,
    val instructions: String,
    val foodInteraction: String?,
    val allergenNote: String?,
    val changeStatus: MedicationChangeStatus,
    val changeNote: String?,
    val refillStatus: RefillStatus,
    val daysSupplyRemaining: Int,
    val todayDoseStatus: DoseActionType = DoseActionType.PENDING,
    val lastActionAttribution: String? = null,
    val lastActionTimeIso: String? = null,
    val hasPillBoxAttachment: Boolean = false,
    val pillBoxAttachmentLabel: String? = null,
    val doseWindowClosed: Boolean = false
)

data class DoseLogEntry(
    val id: String,
    val medicationId: String,
    val episodeId: String,
    val action: DoseActionType,
    val attributionLabel: String,
    val reasonOrNote: String?,
    val timestampIso: String,
    val idempotencyKey: String
)

data class MedicationInteractionWarning(
    val id: String,
    val episodeId: String,
    val severityLabel: String,
    val title: String,
    val detail: String
)

data class AdherenceSummary(
    val totalScheduledActive: Int,
    val takenCount: Int,
    val skippedCount: Int,
    val snoozedCount: Int,
    val adherencePercent: Int
)
