package com.hospitaldischargecompanion.core.notifications

import com.google.common.truth.Truth.assertThat
import com.hospitaldischargecompanion.core.demo.DemoSessionState
import org.junit.Test
import java.time.LocalTime

class NotificationPolicyTest {

    @Test
    fun enforcesQuietHoursForRoutineRemindersWhileAllowingEscalations() {
        val quietSession = DemoSessionState(quietHoursEnabled = true, caregiverAlertsEnabled = true)
        val lateNight = LocalTime.of(23, 30)
        val morning = LocalTime.of(9, 0)

        assertThat(
            NotificationPolicy.shouldDeliverBanner(NotificationCategory.MEDICATION_DUE, quietSession, lateNight)
        ).isFalse()
        assertThat(
            NotificationPolicy.shouldDeliverBanner(NotificationCategory.MEDICATION_DUE, quietSession, morning)
        ).isTrue()
        assertThat(
            NotificationPolicy.shouldDeliverBanner(NotificationCategory.ALERT_ESCALATION, quietSession, lateNight)
        ).isTrue()
    }

    @Test
    fun respectsCaregiverEscalationPreferenceFlag() {
        val mutedCaregiver = DemoSessionState(caregiverAlertsEnabled = false)
        assertThat(
            NotificationPolicy.shouldDeliverBanner(
                category = NotificationCategory.ALERT_ESCALATION,
                session = mutedCaregiver,
                localTime = LocalTime.of(14, 0),
                isCaregiverRecipient = true
            )
        ).isFalse()
    }
}
