package com.hospitaldischargecompanion.core.appointments

enum class AppointmentCategory {
    FOLLOW_UP_VISIT,
    LAB_WORK,
    IMAGING,
    HOME_HEALTH
}

enum class AppointmentStatus {
    SCHEDULED,
    ATTENDED,
    MISSED,
    RESCHEDULE_REQUESTED
}

data class AppointmentItem(
    val id: String,
    val episodeId: String,
    val title: String,
    val category: AppointmentCategory,
    val scheduledDateTimeLabel: String,
    val providerName: String,
    val locationDisplay: String,
    val directionsNote: String,
    val status: AppointmentStatus,
    val requestedNewSlot: String? = null,
    val postVisitTaskTitle: String? = null
)
