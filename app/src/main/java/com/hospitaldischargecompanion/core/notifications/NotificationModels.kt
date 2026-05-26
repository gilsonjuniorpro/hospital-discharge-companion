package com.hospitaldischargecompanion.core.notifications

enum class NotificationCategory {
    MEDICATION_DUE,
    CHECKIN_DUE,
    APPOINTMENT_REMINDER,
    ALERT_ESCALATION
}

data class DemoNotificationItem(
    val id: String,
    val episodeId: String,
    val category: NotificationCategory,
    val title: String,
    val message: String,
    val deepLinkUri: String,
    val timestampIso: String,
    val isRead: Boolean = false
)
