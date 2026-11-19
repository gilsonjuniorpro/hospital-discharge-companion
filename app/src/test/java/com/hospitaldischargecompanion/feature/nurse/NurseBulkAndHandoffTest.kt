package com.hospitaldischargecompanion.feature.nurse

import com.google.common.truth.Truth.assertThat
import com.hospitaldischargecompanion.core.alerts.DefaultAlertRepository
import com.hospitaldischargecompanion.core.alerts.MockAlertApi
import com.hospitaldischargecompanion.core.demo.DemoNetworkSimulator
import com.hospitaldischargecompanion.core.demo.MockClinicalStore
import com.hospitaldischargecompanion.core.discharge.DefaultCarePlanRepository
import com.hospitaldischargecompanion.core.discharge.DefaultDischargeEpisodeRepository
import com.hospitaldischargecompanion.core.discharge.MockCarePlanApi
import com.hospitaldischargecompanion.core.discharge.MockDischargeApi
import com.hospitaldischargecompanion.core.medication.DefaultMedicationRepository
import com.hospitaldischargecompanion.core.medication.MockMedicationApi
import com.hospitaldischargecompanion.core.messaging.DefaultMessagingRepository
import com.hospitaldischargecompanion.core.messaging.MockMessagingApi
import com.hospitaldischargecompanion.core.notifications.MockNotificationCenterRepository
import com.hospitaldischargecompanion.core.telemonitor.DefaultTelemonitoringRepository
import com.hospitaldischargecompanion.core.telemonitor.MockTelemonitoringApi
import com.hospitaldischargecompanion.core.telemonitor.MockWearableIntegrationHook
import com.hospitaldischargecompanion.testing.DeterministicIdGenerator
import com.hospitaldischargecompanion.testing.FixedClinicalClock
import com.hospitaldischargecompanion.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NurseBulkAndHandoffTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun buildsShiftHandoffReportAndSendsBulkRemindersToAllVisibleEpisodes() = runTest {
        val store = MockClinicalStore()
        val sim = DemoNetworkSimulator().apply { setLatencyMs(0L) }
        val clock = FixedClinicalClock()
        val ids = DeterministicIdGenerator()
        val notifRepo = MockNotificationCenterRepository(store, clock, ids)
        val vm = NurseWorkstationViewModel(
            episodeRepository = DefaultDischargeEpisodeRepository(MockDischargeApi(store, sim)),
            carePlanRepository = DefaultCarePlanRepository(MockCarePlanApi(store, sim, clock, ids)),
            medicationRepository = DefaultMedicationRepository(MockMedicationApi(store, sim, clock, ids)),
            telemonitoringRepository = DefaultTelemonitoringRepository(
                MockTelemonitoringApi(store, sim, clock, ids),
                MockWearableIntegrationHook()
            ),
            alertRepository = DefaultAlertRepository(MockAlertApi(store, sim, clock, ids)),
            messagingRepository = DefaultMessagingRepository(MockMessagingApi(store, sim, clock, ids)),
            notificationRepository = notifRepo
        )

        val job = launch { vm.uiState.collect() }
        advanceUntilIdle()

        val report = vm.buildShiftHandoffReport()
        assertThat(report).contains("NURSING SHIFT HANDOFF REPORT")
        assertThat(report).contains("João Silva")
        assertThat(report).contains("OPEN TRIAGE ALERTS")

        val beforeCount = notifRepo.observeNotifications().first().size
        vm.bulkSendReminders()
        advanceUntilIdle()
        assertThat(notifRepo.observeNotifications().first().size).isEqualTo(beforeCount + 3)
        job.cancel()
    }
}
