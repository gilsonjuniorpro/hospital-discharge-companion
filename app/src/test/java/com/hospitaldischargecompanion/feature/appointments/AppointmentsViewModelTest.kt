package com.hospitaldischargecompanion.feature.appointments

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.hospitaldischargecompanion.core.alerts.DefaultAlertRepository
import com.hospitaldischargecompanion.core.alerts.MockAlertApi
import com.hospitaldischargecompanion.core.appointments.AppointmentStatus
import com.hospitaldischargecompanion.core.appointments.DefaultAppointmentRepository
import com.hospitaldischargecompanion.core.appointments.MockAppointmentApi
import com.hospitaldischargecompanion.core.demo.ClinicalSeedFactory
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
class AppointmentsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun patientRequestsCustomSlotAndNurseApprovesInViewModel() = runTest {
        val store = MockClinicalStore()
        val sim = DemoNetworkSimulator().apply { setLatencyMs(0L) }
        val clock = FixedClinicalClock()
        val ids = DeterministicIdGenerator()
        val session = FakeDemoSessionRepository(DemoSessionState(activeRole = UserRole.PATIENT))
        val careRepo = DefaultCarePlanRepository(MockCarePlanApi(store, sim, clock, ids))
        val alertRepo = DefaultAlertRepository(MockAlertApi(store, sim, clock, ids))
        val vm = AppointmentsViewModel(
            savedStateHandle = SavedStateHandle(mapOf("episodeId" to ClinicalSeedFactory.HEART_FAILURE_EPISODE_ID)),
            sessionRepository = session,
            episodeRepository = DefaultDischargeEpisodeRepository(MockDischargeApi(store, sim)),
            appointmentRepository = DefaultAppointmentRepository(MockAppointmentApi(store, careRepo, alertRepo, sim, ids))
        )

        val job = launch { vm.uiState.collect() }
        advanceUntilIdle()

        vm.requestReschedule("appt-hf-cardio", "Next Afternoon · 14:30")
        advanceUntilIdle()
        assertThat(vm.uiState.value.appointments.first { it.id == "appt-hf-cardio" }.status)
            .isEqualTo(AppointmentStatus.RESCHEDULE_REQUESTED)

        session.setActiveRole(UserRole.NURSE)
        advanceUntilIdle()
        vm.approveReschedule("appt-hf-cardio")
        advanceUntilIdle()

        val updated = vm.uiState.value.appointments.first { it.id == "appt-hf-cardio" }
        assertThat(updated.status).isEqualTo(AppointmentStatus.SCHEDULED)
        assertThat(updated.scheduledDateTimeLabel).isEqualTo("Next Afternoon · 14:30")
        job.cancel()
    }
}
