package com.hospitaldischargecompanion.core.ehr

import kotlinx.serialization.Serializable

@Serializable
data class PatientDto(
    val resourceType: String = "Patient",
    val id: String,
    val fullName: String,
    val ageYears: Int,
    val mrnMasked: String
)

@Serializable
data class EncounterDto(
    val resourceType: String = "Encounter",
    val id: String,
    val patientRef: String,
    val caseCode: String,
    val diagnosisDisplay: String,
    val clinicalSummary: String,
    val dischargeDateIso: String,
    val hospitalName: String,
    val unitDisplay: String,
    val attendingDisplay: String,
    val primaryNurseDisplay: String,
    val riskCode: String,
    val riskRationale: String
)

@Serializable
data class MedicationRequestDto(
    val resourceType: String = "MedicationRequest",
    val id: String,
    val encounterRef: String,
    val medicationName: String,
    val dosageInstruction: String,
    val changeCategory: String,
    val status: String
)

@Serializable
data class ObservationDto(
    val resourceType: String = "Observation",
    val id: String,
    val encounterRef: String,
    val code: String,
    val valueDisplay: String,
    val effectiveDateTimeIso: String,
    val performerDisplay: String,
    val versionId: Int = 1
)

@Serializable
data class CarePlanDto(
    val resourceType: String = "CarePlan",
    val id: String,
    val encounterRef: String,
    val revision: Int,
    val authorDisplay: String,
    val summary: String,
    val signedOff: Boolean
)

@Serializable
data class AppointmentDto(
    val resourceType: String = "Appointment",
    val id: String,
    val encounterRef: String,
    val serviceType: String,
    val startIso: String,
    val locationDisplay: String,
    val status: String
)

@Serializable
data class CommunicationDto(
    val resourceType: String = "Communication",
    val id: String,
    val encounterRef: String,
    val senderRole: String,
    val senderName: String,
    val payload: String,
    val isClinicalNote: Boolean,
    val sentIso: String
)
