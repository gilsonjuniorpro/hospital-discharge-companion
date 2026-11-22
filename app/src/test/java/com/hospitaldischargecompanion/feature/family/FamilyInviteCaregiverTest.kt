package com.hospitaldischargecompanion.feature.family

import com.google.common.truth.Truth.assertThat
import com.hospitaldischargecompanion.core.alerts.DefaultAlertRepository
import com.hospitaldischargecompanion.core.alerts.MockAlertApi
import com.hospitaldischargecompanion.core.carecircle.CareCircleMemberStatus
import com.hospitaldischargecompanion.core.carecircle.CaregiverPermission
import com.hospitaldischargecompanion.core.carecircle.DefaultCareCircleRepository
import com.hospitaldischargecompanion.core.carecircle.MockCareCircleApi
import com.hospitaldischargecompanion.core.demo.DemoNetworkSimulator
import com.hospitaldischargecompanion.core.demo.DemoSessionState
import com.hospitaldischargecompanion.core.demo.MockClinicalStore
import com.hospitaldischargecompanion.core.demo.UserRole
import com.hospitaldischargecompanion.core.discharge.DefaultCarePlanRepository
import com.hospitaldischargecompanion.core.discharge.DefaultDischargeEpisodeRepository
import com.hospitaldischargecompanion.core.discharge.MockCarePlanApi
import com.hospitaldischargecompanion.core.discharge.MockDischargeApi
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
class FamilyInviteCaregiverTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun invitesCaregiverAndRejectsBlankName() = runTest {
        val store = MockClinicalStore()
        val sim = DemoNetworkSimulator().apply { setLatencyMs(0L) }
        val clock = FixedClinicalClock()
        val ids = DeterministicIdGenerator()
        val vm = FamilyHomeViewModel(
            sessionRepository = FakeDemoSessionRepository(DemoSessionState(activeRole = UserRole.FAMILY)),
            episodeRepository = DefaultDischargeEpisodeRepository(MockDischargeApi(store, sim)),
            carePlanRepository = DefaultCarePlanRepository(MockCarePlanApi(store, sim, clock, ids)),
            alertRepository = DefaultAlertRepository(MockAlertApi(store, sim, clock, ids)),
            careCircleRepository = DefaultCareCircleRepository(MockCareCircleApi(store, sim, ids))
        )

        val job = launch { vm.uiState.collect() }
        advanceUntilIdle()

        vm.inviteCaregiver("", "Sister", CaregiverPermission.VIEW_ONLY)
        advanceUntilIdle()
        assertThat(vm.uiState.value.errorMessage).contains("Caregiver name required")

        vm.inviteCaregiver("Beatriz Silva", "Sister", CaregiverPermission.ASSIST_MEDS_AND_CHECKINS)
        advanceUntilIdle()
        assertThat(vm.uiState.value.errorMessage).isNull()
        assertThat(vm.uiState.value.careCircle.any {
            it.name == "Beatriz Silva" && it.status == CareCircleMemberStatus.INVITED
        }).isTrue()
        job.cancel()
    }
}
