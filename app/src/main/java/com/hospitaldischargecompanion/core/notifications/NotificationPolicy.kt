package com.hospitaldischargecompanion.core.notifications

import com.hospitaldischargecompanion.core.demo.DemoSessionState
import java.time.LocalTime

object NotificationPolicy {
    private val quietStart = LocalTime.of(22, 0)
    private val quietEnd = LocalTime.of(7, 0)

    fun isQuietWindow(time: LocalTime): Boolean =
        time >= quietStart || time < quietEnd

    fun shouldDeliverBanner(
        category: NotificationCategory,
        session: DemoSessionState,
        localTime: LocalTime,
        isCaregiverRecipient: Boolean = false
    ): Boolean {
        if (category == NotificationCategory.ALERT_ESCALATION) {
            if (isCaregiverRecipient && !session.caregiverAlertsEnabled) return false
            return true
        }
        if (session.quietHoursEnabled && isQuietWindow(localTime)) {
            return false
        }
        return true
    }
}
