package com.hospitaldischargecompanion.feature.physician

import com.google.common.truth.Truth.assertThat
import com.hospitaldischargecompanion.core.alerts.DefaultAlertRepository
import com.hospitaldischargecompanion.core.alerts.MockAlertApi
import com.hospitaldischargecompanion.core.demo.DemoNetworkSimulator
import com.hospitaldischargecompanion.core.demo.DemoSessionState
import com.hospitaldischargecompanion.core.demo.MockClinicalStore
import com.hospitaldischargecompanion.core.demo.UserRole
import com.hospitaldischargecompanion.core.discharge.DefaultCarePlanRepository
import com.hospitaldischargecompanion.core.discharge.DefaultDischargeEpisodeRepository
import com.hospitaldischargecompanion.core.discharge.MockCarePlanApi
import com.hospitaldischargecompanion.core.discharge.MockDischargeApi
import com.hospitaldischargecompanion.core.medication.DefaultMedicationRepository
import com.hospitaldischargecompanion.core.medication.MockMedicationApi
import com.hospitaldischargecompanion.core.telemonitor.DefaultTelemonitoringRepository
import com.hospitaldischargecompanion.core.telemonitor.MockTelemonitoringApi
import com.hospitaldischargecompanion.core.telemonitor.MockWearableIntegrationHook
import com.hospitaldischargecompanion.testing.DeterministicIdGenerator
import com.hospitaldischargecompanion.testing.FakeDemoSessionRepository
import com.hospitaldischargecompanion.testing.FixedClinicalClock
import com.hospitaldischargecompanion.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PhysicianMultiRevisionTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun tracksSequentialCarePlanRevisionsAndRedFlagAdditions() = runTest {
        val store = MockClinicalStore()
        val sim = DemoNetworkSimulator().apply { setLatencyMs(0L) }
        val clock = FixedClinicalClock()
        val ids = DeterministicIdGenerator()
        val vm = PhysicianWorkstationViewModel(
            sessionRepository = FakeDemoSessionRepository(DemoSessionState(activeRole = UserRole.PHYSICIAN)),
            episodeRepository = DefaultDischargeEpisodeRepository(MockDischargeApi(store, sim)),
            carePlanRepository = DefaultCarePlanRepository(MockCarePlanApi(store, sim, clock, ids)),
            medicationRepository = DefaultMedicationRepository(MockMedicationApi(store, sim, clock, ids)),
            telemonitoringRepository = DefaultTelemonitoringRepository(
                MockTelemonitoringApi(store, sim, clock, ids),
                MockWearableIntegrationHook()
            ),
            alertRepository = DefaultAlertRepository(MockAlertApi(store, sim, clock, ids))
        )

        val job = launch { vm.uiState.collect() }
        advanceUntilIdle()

        vm.amendAndSignOffCarePlan("Add evening weight", "Evening weight", "Orthopnea >3 pillows")
        advanceUntilIdle()
        vm.selectDisposition(ClinicalDisposition.ADVISE_ER_COPY_ONLY)
        vm.amendAndSignOffCarePlan("Escalate red flag guidance", "Immediate triage call", "")
        advanceUntilIdle()

        assertThat(vm.uiState.value.selectedEpisode?.carePlanRevision).isEqualTo(3)
        assertThat(vm.uiState.value.revisions).hasSize(3)
        assertThat(vm.uiState.value.revisions.first().revisionNumber).isEqualTo(3)
        assertThat(vm.uiState.value.selectedEpisode?.redFlags).contains("Orthopnea >3 pillows")
        assertThat(vm.uiState.value.medications).hasSize(4)
        job.cancel()
    }
}
