package com.hospitaldischargecompanion.core.notifications

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.hospitaldischargecompanion.core.demo.DemoSessionState
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalReminderDispatcher @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    companion object {
        const val CHANNEL_ID = "hdc_clinical_reminders"
    }

    fun ensureChannel() {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Discharge Care Reminders",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Demo reminders for discharge medications, check-ins, and alert escalations."
        }
        manager.createNotificationChannel(channel)
    }

    @SuppressLint("MissingPermission")
    fun dispatchIfAllowed(
        item: DemoNotificationItem,
        session: DemoSessionState,
        localTime: LocalTime = LocalTime.now()
    ): Boolean {
        if (!NotificationPolicy.shouldDeliverBanner(item.category, session, localTime)) {
            return false
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return false
        }
        ensureChannel()
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(item.title)
            .setContentText(item.message)
            .setAutoCancel(true)
            .build()
        return runCatching {
            NotificationManagerCompat.from(context).notify(item.id.hashCode(), notification)
            true
        }.getOrDefault(false)
    }
}
