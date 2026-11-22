package com.hospitaldischargecompanion.core.discharge

import com.hospitaldischargecompanion.core.demo.UserRole

enum class CarePlanTaskType {
    MEDICATION,
    VITAL_CHECK,
    WOUND_CARE,
    DIET,
    ACTIVITY,
    EDUCATION,
    APPOINTMENT,
    LAB
}

enum class CarePlanTaskStatus {
    UPCOMING,
    DUE,
    COMPLETED,
    SKIPPED,
    OVERDUE,
    ESCALATED
}

data class CarePlanTask(
    val id: String,
    val episodeId: String,
    val dayNumber: Int,
    val scheduledTimeLabel: String,
    val title: String,
    val instructions: String,
    val type: CarePlanTaskType,
    val status: CarePlanTaskStatus,
    val assignedToLabel: String = "Patient / Caregiver",
    val completedByLabel: String? = null,
    val skipReason: String? = null,
    val addedInRevision: Int = 1
)

data class CarePlanRevision(
    val revisionNumber: Int,
    val episodeId: String,
    val authorName: String,
    val authorRole: UserRole,
    val timestampIso: String,
    val changeSummary: String,
    val signedOff: Boolean
)
