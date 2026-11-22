package com.hospitaldischargecompanion.core.notifications

import com.hospitaldischargecompanion.core.demo.ClinicalClock
import com.hospitaldischargecompanion.core.demo.ClinicalSeedFactory
import com.hospitaldischargecompanion.core.demo.IdGenerator
import com.hospitaldischargecompanion.core.demo.MockClinicalStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

interface NotificationCenterRepository {
    fun observeNotifications(): Flow<List<DemoNotificationItem>>
    suspend fun pushLocalReminder(
        episodeId: String,
        category: NotificationCategory,
        title: String,
        message: String,
        deepLinkUri: String
    ): DemoNotificationItem
    suspend fun markRead(notificationId: String)
    suspend fun markAllRead()
}

@Singleton
class MockNotificationCenterRepository @Inject constructor(
    store: MockClinicalStore,
    private val clock: ClinicalClock,
    private val idGenerator: IdGenerator
) : NotificationCenterRepository {

    private fun seedNotifications(): List<DemoNotificationItem> = listOf(
        DemoNotificationItem(
            id = "notif-1",
            episodeId = ClinicalSeedFactory.HEART_FAILURE_EPISODE_ID,
            category = NotificationCategory.MEDICATION_DUE,
            title = "Morning Furosemide 40 mg due",
            message = "Confirm morning loop diuretic dose for João Silva.",
            deepLinkUri = "hdc://dose/med-hf-furosemide",
            timestampIso = "2026-06-15T08:00:00Z"
        ),
        DemoNotificationItem(
            id = "notif-2",
            episodeId = ClinicalSeedFactory.HEART_FAILURE_EPISODE_ID,
            category = NotificationCategory.ALERT_ESCALATION,
            title = "Weight trend warning (+1.1 kg)",
            message = "Open alert in care plan to review fluid restriction guidance.",
            deepLinkUri = "hdc://alert/alert-hf-weight",
            timestampIso = "2026-06-14T08:26:00Z"
        ),
        DemoNotificationItem(
            id = "notif-3",
            episodeId = ClinicalSeedFactory.HEART_FAILURE_EPISODE_ID,
            category = NotificationCategory.APPOINTMENT_REMINDER,
            title = "Day-5 BMP Lab tomorrow at 09:00",
            message = "St. Jude Outpatient Lab — bring discharge requisition.",
            deepLinkUri = "hdc://episode/${ClinicalSeedFactory.HEART_FAILURE_EPISODE_ID}",
            timestampIso = "2026-06-15T12:00:00Z",
            isRead = true
        )
    )

    private val _notifications = MutableStateFlow(seedNotifications())

    init {
        store.registerResetCallback { _notifications.value = seedNotifications() }
    }

    override fun observeNotifications(): Flow<List<DemoNotificationItem>> = _notifications.asStateFlow()

    override suspend fun pushLocalReminder(
        episodeId: String,
        category: NotificationCategory,
        title: String,
        message: String,
        deepLinkUri: String
    ): DemoNotificationItem {
        val item = DemoNotificationItem(
            id = idGenerator.nextId("notif"),
            episodeId = episodeId,
            category = category,
            title = title,
            message = message,
            deepLinkUri = deepLinkUri,
            timestampIso = clock.now().toString(),
            isRead = false
        )
        _notifications.update { listOf(item) + it }
        return item
    }

    override suspend fun markRead(notificationId: String) {
        _notifications.update { list ->
            list.map { if (it.id == notificationId) it.copy(isRead = true) else it }
        }
    }

    override suspend fun markAllRead() {
        _notifications.update { list -> list.map { it.copy(isRead = true) } }
    }
}
