package com.hospitaldischargecompanion.core.appointments

import com.google.common.truth.Truth.assertThat
import com.hospitaldischargecompanion.core.alerts.DefaultAlertRepository
import com.hospitaldischargecompanion.core.alerts.MockAlertApi
import com.hospitaldischargecompanion.core.demo.ClinicalSeedFactory
import com.hospitaldischargecompanion.core.demo.DemoNetworkSimulator
import com.hospitaldischargecompanion.core.demo.DomainResult
import com.hospitaldischargecompanion.core.demo.MockClinicalStore
import com.hospitaldischargecompanion.core.demo.UserRole
import com.hospitaldischargecompanion.core.discharge.DefaultCarePlanRepository
import com.hospitaldischargecompanion.core.discharge.MockCarePlanApi
import com.hospitaldischargecompanion.testing.DeterministicIdGenerator
import com.hospitaldischargecompanion.testing.FixedClinicalClock
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class AppointmentRepositoryTest {

    private lateinit var carePlanRepo: DefaultCarePlanRepository
    private lateinit var alertRepo: DefaultAlertRepository
    private lateinit var repository: AppointmentRepository

    @Before
    fun setUp() {
        val store = MockClinicalStore()
        val sim = DemoNetworkSimulator().apply { setLatencyMs(0L) }
        val clock = FixedClinicalClock()
        val ids = DeterministicIdGenerator()
        carePlanRepo = DefaultCarePlanRepository(MockCarePlanApi(store, sim, clock, ids))
        alertRepo = DefaultAlertRepository(MockAlertApi(store, sim, clock, ids))
        repository = DefaultAppointmentRepository(MockAppointmentApi(store, carePlanRepo, alertRepo, sim, ids))
    }

    @Test
    fun attendingAppointmentAutoAddsPostVisitCarePlanTask() = runTest {
        val beforeCount = carePlanRepo.observeTasks(ClinicalSeedFactory.HEART_FAILURE_EPISODE_ID).first().size
        val result = repository.markAttended("appt-hf-lab")
        assertThat((result as DomainResult.Success).data.status).isEqualTo(AppointmentStatus.ATTENDED)

        val afterTasks = carePlanRepo.observeTasks(ClinicalSeedFactory.HEART_FAILURE_EPISODE_ID).first()
        assertThat(afterTasks.size).isEqualTo(beforeCount + 1)
        assertThat(afterTasks.any { it.title.contains("BMP lab potassium") }).isTrue()
    }

    @Test
    fun rescheduleRequestRequiresClinicianApprovalAndMissedRaisesAlert() = runTest {
        val req = repository.requestReschedule("appt-hf-cardio", "Day 6 · Jun 18, 09:00")
        assertThat((req as DomainResult.Success).data.status).isEqualTo(AppointmentStatus.RESCHEDULE_REQUESTED)

        val patientApprove = repository.approveReschedule("appt-hf-cardio", UserRole.PATIENT)
        assertThat(patientApprove).isInstanceOf(DomainResult.Failure::class.java)

        val nurseApprove = repository.approveReschedule("appt-hf-cardio", UserRole.NURSE)
        assertThat((nurseApprove as DomainResult.Success).data.scheduledDateTimeLabel).isEqualTo("Day 6 · Jun 18, 09:00")

        val missed = repository.markMissed("appt-hf-home", "João Silva")
        assertThat((missed as DomainResult.Success).data.status).isEqualTo(AppointmentStatus.MISSED)
        assertThat(alertRepo.observeEpisodeAlerts(ClinicalSeedFactory.HEART_FAILURE_EPISODE_ID).first().any {
            it.title.contains("Missed appointment")
        }).isTrue()
    }
}
