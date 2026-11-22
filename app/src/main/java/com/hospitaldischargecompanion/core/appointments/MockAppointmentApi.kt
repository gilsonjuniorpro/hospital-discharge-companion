package com.hospitaldischargecompanion.core.appointments

import com.hospitaldischargecompanion.core.alerts.AlertRepository
import com.hospitaldischargecompanion.core.alerts.AlertSeverity
import com.hospitaldischargecompanion.core.alerts.AlertType
import com.hospitaldischargecompanion.core.alerts.EscalationStage
import com.hospitaldischargecompanion.core.demo.ClinicalSeedFactory
import com.hospitaldischargecompanion.core.demo.DemoNetworkSimulator
import com.hospitaldischargecompanion.core.demo.DomainError
import com.hospitaldischargecompanion.core.demo.DomainResult
import com.hospitaldischargecompanion.core.demo.IdGenerator
import com.hospitaldischargecompanion.core.demo.MockClinicalStore
import com.hospitaldischargecompanion.core.demo.UserRole
import com.hospitaldischargecompanion.core.discharge.CarePlanRepository
import com.hospitaldischargecompanion.core.discharge.CarePlanTask
import com.hospitaldischargecompanion.core.discharge.CarePlanTaskStatus
import com.hospitaldischargecompanion.core.discharge.CarePlanTaskType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

interface AppointmentDataSource {
    fun observeAllAppointments(): Flow<List<AppointmentItem>>
    suspend fun markAttended(appointmentId: String): DomainResult<AppointmentItem>
    suspend fun markMissed(appointmentId: String, actorName: String): DomainResult<AppointmentItem>
    suspend fun requestReschedule(appointmentId: String, preferredSlot: String): DomainResult<AppointmentItem>
    suspend fun approveReschedule(appointmentId: String, actorRole: UserRole): DomainResult<AppointmentItem>
}

@Singleton
class MockAppointmentApi @Inject constructor(
    private val store: MockClinicalStore,
    private val carePlanRepository: CarePlanRepository,
    private val alertRepository: AlertRepository,
    private val networkSimulator: DemoNetworkSimulator,
    private val idGenerator: IdGenerator
) : AppointmentDataSource {

    private fun seedAppointments(): List<AppointmentItem> = listOf(
        AppointmentItem(
            id = "appt-hf-lab",
            episodeId = ClinicalSeedFactory.HEART_FAILURE_EPISODE_ID,
            title = "Basic Metabolic Panel (K+ / Creatinine)",
            category = AppointmentCategory.LAB_WORK,
            scheduledDateTimeLabel = "Day 5 · Jun 17, 09:00",
            providerName = "St. Jude Outpatient Lab",
            locationDisplay = "Diagnostic Pavilion, Floor 1",
            directionsNote = "Fast for 4 hours if requested; bring discharge lab requisition.",
            status = AppointmentStatus.SCHEDULED,
            postVisitTaskTitle = "Review BMP lab potassium result with nurse"
        ),
        AppointmentItem(
            id = "appt-hf-cardio",
            episodeId = ClinicalSeedFactory.HEART_FAILURE_EPISODE_ID,
            title = "Heart Failure Transition Clinic",
            category = AppointmentCategory.FOLLOW_UP_VISIT,
            scheduledDateTimeLabel = "Day 7 · Jun 19, 10:30",
            providerName = "Dr. Elena Vance, MD",
            locationDisplay = "Heart & Vascular Center, Suite 410",
            directionsNote = "Parking Garage B, Elevator 2. Bring home BP cuff and dry-weight log.",
            status = AppointmentStatus.SCHEDULED,
            postVisitTaskTitle = "Confirm post-clinic GDMT dose adjustments"
        ),
        AppointmentItem(
            id = "appt-hf-home",
            episodeId = ClinicalSeedFactory.HEART_FAILURE_EPISODE_ID,
            title = "Home Health Nursing Assessment",
            category = AppointmentCategory.HOME_HEALTH,
            scheduledDateTimeLabel = "Day 4 · Jun 16, 14:00",
            providerName = "RN Clara Mendes (Home Health)",
            locationDisplay = "Patient Home Residence",
            directionsNote = "Home visit to review pill box, ankle edema, and scale calibration.",
            status = AppointmentStatus.SCHEDULED
        ),
        AppointmentItem(
            id = "appt-ps-surg",
            episodeId = ClinicalSeedFactory.POST_SURGERY_EPISODE_ID,
            title = "Post-Operative Incision Check",
            category = AppointmentCategory.FOLLOW_UP_VISIT,
            scheduledDateTimeLabel = "Day 10 · Jun 23, 14:00",
            providerName = "Dr. Marcus Sterling, MD",
            locationDisplay = "Surgical Outpatient Clinic, Floor 2",
            directionsNote = "Wear loose comfortable clothing around abdominal incision sites.",
            status = AppointmentStatus.SCHEDULED,
            postVisitTaskTitle = "Resume full light exercise if cleared by surgeon"
        ),
        AppointmentItem(
            id = "appt-pn-xray",
            episodeId = ClinicalSeedFactory.PNEUMONIA_EPISODE_ID,
            title = "Follow-Up Chest X-Ray & Primary Care",
            category = AppointmentCategory.IMAGING,
            scheduledDateTimeLabel = "Day 14 · Jun 25, 11:00",
            providerName = "Imaging Center & Internal Medicine",
            locationDisplay = "Radiology Wing B",
            directionsNote = "Bring pulse oximetry log from oral antibiotic completion.",
            status = AppointmentStatus.SCHEDULED
        )
    )

    private val _appointments = MutableStateFlow(seedAppointments())

    init {
        store.registerResetCallback { _appointments.value = seedAppointments() }
    }

    override fun observeAllAppointments(): Flow<List<AppointmentItem>> = _appointments.asStateFlow()

    override suspend fun markAttended(appointmentId: String): DomainResult<AppointmentItem> =
        networkSimulator.execute {
            val existing = _appointments.value.firstOrNull { it.id == appointmentId }
                ?: return@execute DomainResult.Failure(DomainError.ValidationFailed("appointmentId", "Appointment not found"))
            val updated = existing.copy(status = AppointmentStatus.ATTENDED)
            _appointments.update { list -> list.map { if (it.id == appointmentId) updated else it } }

            existing.postVisitTaskTitle?.let { followUpTitle ->
                val ep = store.getEpisode(existing.episodeId)
                carePlanRepository.addFollowUpTask(
                    CarePlanTask(
                        id = idGenerator.nextId("task-post"),
                        episodeId = existing.episodeId,
                        dayNumber = (ep?.dischargeDayNumber ?: 3) + 1,
                        scheduledTimeLabel = "Post-Visit Auto-Task",
                        title = followUpTitle,
                        instructions = "Auto-added after attending ${existing.title}.",
                        type = CarePlanTaskType.EDUCATION,
                        status = CarePlanTaskStatus.DUE
                    )
                )
            }
            DomainResult.Success(updated)
        }

    override suspend fun markMissed(appointmentId: String, actorName: String): DomainResult<AppointmentItem> =
        networkSimulator.execute {
            val existing = _appointments.value.firstOrNull { it.id == appointmentId }
                ?: return@execute DomainResult.Failure(DomainError.ValidationFailed("appointmentId", "Appointment not found"))
            val updated = existing.copy(status = AppointmentStatus.MISSED)
            _appointments.update { list -> list.map { if (it.id == appointmentId) updated else it } }
            alertRepository.raiseAlert(
                episodeId = existing.episodeId,
                type = AlertType.MISSED_APPOINTMENT,
                severity = AlertSeverity.WARNING,
                escalationStage = EscalationStage.NURSE_TRIAGE,
                title = "Missed appointment: ${existing.title}",
                summary = "Scheduled for ${existing.scheduledDateTimeLabel} at ${existing.locationDisplay}.",
                patientGuidance = "Please request a reschedule slot so your care team can keep follow-up on track.",
                actorName = actorName,
                actorRole = UserRole.PATIENT,
                reasonCode = "MISSED_FOLLOW_UP"
            )
            DomainResult.Success(updated)
        }

    override suspend fun requestReschedule(
        appointmentId: String,
        preferredSlot: String
    ): DomainResult<AppointmentItem> = networkSimulator.execute {
        if (preferredSlot.isBlank()) {
            return@execute DomainResult.Failure(DomainError.ValidationFailed("preferredSlot", "Preferred time is required"))
        }
        val existing = _appointments.value.firstOrNull { it.id == appointmentId }
            ?: return@execute DomainResult.Failure(DomainError.ValidationFailed("appointmentId", "Appointment not found"))
        val updated = existing.copy(
            status = AppointmentStatus.RESCHEDULE_REQUESTED,
            requestedNewSlot = preferredSlot.trim()
        )
        _appointments.update { list -> list.map { if (it.id == appointmentId) updated else it } }
        DomainResult.Success(updated)
    }

    override suspend fun approveReschedule(
        appointmentId: String,
        actorRole: UserRole
    ): DomainResult<AppointmentItem> = networkSimulator.execute {
        if (!actorRole.isClinician) {
            return@execute DomainResult.Failure(
                DomainError.UnauthorizedRoleAction(actorRole, "approve_appointment_reschedule")
            )
        }
        val existing = _appointments.value.firstOrNull { it.id == appointmentId }
            ?: return@execute DomainResult.Failure(DomainError.ValidationFailed("appointmentId", "Appointment not found"))
        val newSlot = existing.requestedNewSlot ?: existing.scheduledDateTimeLabel
        val updated = existing.copy(
            status = AppointmentStatus.SCHEDULED,
            scheduledDateTimeLabel = newSlot,
            requestedNewSlot = null
        )
        _appointments.update { list -> list.map { if (it.id == appointmentId) updated else it } }
        DomainResult.Success(updated)
    }
}
