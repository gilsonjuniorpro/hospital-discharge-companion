package com.hospitaldischargecompanion.feature.nurse

import com.google.common.truth.Truth.assertThat
import com.hospitaldischargecompanion.core.alerts.DefaultAlertRepository
import com.hospitaldischargecompanion.core.alerts.MockAlertApi
import com.hospitaldischargecompanion.core.demo.ClinicalSeedFactory
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
class NurseNoteTemplatesTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun postsStructuredClinicalNoteTemplateToEpisodeThread() = runTest {
        val store = MockClinicalStore()
        val sim = DemoNetworkSimulator().apply { setLatencyMs(0L) }
        val clock = FixedClinicalClock()
        val ids = DeterministicIdGenerator()
        val msgRepo = DefaultMessagingRepository(MockMessagingApi(store, sim, clock, ids))
        val vm = NurseWorkstationViewModel(
            episodeRepository = DefaultDischargeEpisodeRepository(MockDischargeApi(store, sim)),
            carePlanRepository = DefaultCarePlanRepository(MockCarePlanApi(store, sim, clock, ids)),
            medicationRepository = DefaultMedicationRepository(MockMedicationApi(store, sim, clock, ids)),
            telemonitoringRepository = DefaultTelemonitoringRepository(
                MockTelemonitoringApi(store, sim, clock, ids),
                MockWearableIntegrationHook()
            ),
            alertRepository = DefaultAlertRepository(MockAlertApi(store, sim, clock, ids)),
            messagingRepository = msgRepo,
            notificationRepository = MockNotificationCenterRepository(store, clock, ids)
        )

        val job = launch { vm.uiState.collect() }
        advanceUntilIdle()

        val template = vm.uiState.value.noteTemplates.first()
        vm.postNursingTemplateNote(ClinicalSeedFactory.HEART_FAILURE_EPISODE_ID, "João Silva", template)
        advanceUntilIdle()

        val thread = msgRepo.observeThread(ClinicalSeedFactory.HEART_FAILURE_EPISODE_ID).first()
        assertThat(thread.last().isClinicalNote).isTrue()
        assertThat(thread.last().body).contains(template.body)
        assertThat(vm.uiState.value.feedbackBanner).contains(template.title)
        job.cancel()
    }
}
