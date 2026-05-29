package com.hospitaldischargecompanion.core.ehr

import com.google.common.truth.Truth.assertThat
import com.hospitaldischargecompanion.core.alerts.DefaultAlertRepository
import com.hospitaldischargecompanion.core.alerts.MockAlertApi
import com.hospitaldischargecompanion.core.demo.ClinicalSeedFactory
import com.hospitaldischargecompanion.core.demo.DemoNetworkSimulator
import com.hospitaldischargecompanion.core.demo.DomainError
import com.hospitaldischargecompanion.core.demo.DomainResult
import com.hospitaldischargecompanion.core.demo.MockClinicalStore
import com.hospitaldischargecompanion.core.demo.SimulatedFailureMode
import com.hospitaldischargecompanion.core.discharge.EhrSyncStatus
import com.hospitaldischargecompanion.core.notifications.MockNotificationCenterRepository
import com.hospitaldischargecompanion.core.notifications.NotificationCategory
import com.hospitaldischargecompanion.testing.DeterministicIdGenerator
import com.hospitaldischargecompanion.testing.FixedClinicalClock
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class EhrSyncAndNotificationRepositoryTest {

    private lateinit var store: MockClinicalStore
    private lateinit var simulator: DemoNetworkSimulator
    private lateinit var ehrRepo: EhrSyncRepository
    private lateinit var notificationRepo: MockNotificationCenterRepository

    @Before
    fun setUp() {
        store = MockClinicalStore()
        simulator = DemoNetworkSimulator().apply { setLatencyMs(0L) }
        val clock = FixedClinicalClock()
        val ids = DeterministicIdGenerator()
        val alertRepo = DefaultAlertRepository(MockAlertApi(store, simulator, clock, ids))
        ehrRepo = DefaultEhrSyncRepository(MockEhrSyncApi(store, alertRepo, simulator, clock, ids))
        notificationRepo = MockNotificationCenterRepository(store, clock, ids)
    }

    @Test
    fun simulates409ConflictCreatesRecordAndResolvesBackToSynced() = runTest {
        simulator.setFailureMode(SimulatedFailureMode.EHR_CONFLICT_409)

        val pushResult = ehrRepo.pushObservationsAndAdherence(ClinicalSeedFactory.HEART_FAILURE_EPISODE_ID)
        assertThat(pushResult).isInstanceOf(DomainResult.Failure::class.java)
        assertThat((pushResult as DomainResult.Failure).error).isInstanceOf(DomainError.EhrSyncConflict::class.java)
        assertThat(store.getEpisode(ClinicalSeedFactory.HEART_FAILURE_EPISODE_ID)?.ehrSyncStatus)
            .isEqualTo(EhrSyncStatus.CONFLICT)

        val openConflicts = ehrRepo.observeOpenConflicts(ClinicalSeedFactory.HEART_FAILURE_EPISODE_ID).first()
        assertThat(openConflicts).hasSize(1)

        val resolved = ehrRepo.resolveConflict(openConflicts.first().id, ConflictResolutionChoice.KEEP_LOCAL_HOME)
        assertThat(resolved).isInstanceOf(DomainResult.Success::class.java)
        assertThat(store.getEpisode(ClinicalSeedFactory.HEART_FAILURE_EPISODE_ID)?.ehrSyncStatus)
            .isEqualTo(EhrSyncStatus.SYNCED)
        assertThat(ehrRepo.observeOpenConflicts(ClinicalSeedFactory.HEART_FAILURE_EPISODE_ID).first()).isEmpty()
    }

    @Test
    fun notificationCenterPushesLocalReminderAndMarksRead() = runTest {
        val added = notificationRepo.pushLocalReminder(
            episodeId = ClinicalSeedFactory.HEART_FAILURE_EPISODE_ID,
            category = NotificationCategory.CHECKIN_DUE,
            title = "Evening weight check due",
            message = "Physician added an evening dry-weight check.",
            deepLinkUri = "hdc://episode/ep-hf-001"
        )
        assertThat(added.isRead).isFalse()
        notificationRepo.markAllRead()
        assertThat(notificationRepo.observeNotifications().first().all { it.isRead }).isTrue()
    }
}
