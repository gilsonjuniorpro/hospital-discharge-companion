package com.hospitaldischargecompanion.core.alerts

import com.hospitaldischargecompanion.core.demo.UserRole

enum class AlertType {
    MISSED_MEDICATION,
    ABNORMAL_VITAL,
    RED_FLAG_SYMPTOM,
    MISSED_APPOINTMENT,
    CARE_PLAN_OVERDUE,
    EHR_SYNC_CONFLICT
}

enum class AlertSeverity {
    INFO,
    WARNING,
    URGENT,
    CRITICAL
}

enum class AlertStatus {
    OPEN,
    ACKNOWLEDGED,
    SNOOZED,
    ESCALATED,
    RESOLVED
}

enum class EscalationStage {
    PATIENT_FAMILY,
    NURSE_TRIAGE,
    PHYSICIAN_REVIEW,
    ADVISE_ER_COPY_ONLY;

    fun nextStage(): EscalationStage = when (this) {
        PATIENT_FAMILY -> NURSE_TRIAGE
        NURSE_TRIAGE -> PHYSICIAN_REVIEW
        PHYSICIAN_REVIEW -> ADVISE_ER_COPY_ONLY
        ADVISE_ER_COPY_ONLY -> ADVISE_ER_COPY_ONLY
    }
}

data class AlertAuditEntry(
    val timestampIso: String,
    val actorName: String,
    val actorRole: UserRole,
    val actionSummary: String,
    val reasonCode: String? = null
)

data class ClinicalAlert(
    val id: String,
    val episodeId: String,
    val type: AlertType,
    val severity: AlertSeverity,
    val status: AlertStatus,
    val escalationStage: EscalationStage,
    val title: String,
    val summary: String,
    val patientGuidance: String,
    val createdAtIso: String,
    val auditTrail: List<AlertAuditEntry>
) {
    val isActive: Boolean
        get() = status != AlertStatus.RESOLVED
}
