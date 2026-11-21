package com.hospitaldischargecompanion.core.ehr

import com.hospitaldischargecompanion.core.discharge.DischargeEpisode
import com.hospitaldischargecompanion.core.medication.MedicationItem
import com.hospitaldischargecompanion.core.telemonitor.VitalCheckIn
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class MockFhirBundleDto(
    val resourceType: String = "Bundle",
    val type: String = "collection",
    val patient: PatientDto,
    val encounter: EncounterDto,
    val carePlan: CarePlanDto,
    val medications: List<MedicationRequestDto>,
    val latestObservation: ObservationDto?
)

object FhirBundleSerializer {
    private val prettyJson = Json {
        prettyPrint = true
        encodeDefaults = true
    }

    fun buildBundle(
        episode: DischargeEpisode,
        medications: List<MedicationItem>,
        latestCheckIn: VitalCheckIn?
    ): MockFhirBundleDto = MockFhirBundleDto(
        patient = PatientDto(
            id = episode.patientId,
            fullName = episode.patientName,
            ageYears = episode.patientAge,
            mrnMasked = episode.mrnMasked
        ),
        encounter = EncounterDto(
            id = episode.id,
            patientRef = "Patient/${episode.patientId}",
            caseCode = episode.caseType.name,
            diagnosisDisplay = episode.diagnosisTitle,
            clinicalSummary = episode.diagnosisSummary,
            dischargeDateIso = episode.dischargeDateIso,
            hospitalName = episode.hospitalName,
            unitDisplay = episode.hospitalUnit,
            attendingDisplay = episode.attendingPhysician,
            primaryNurseDisplay = episode.primaryNurse,
            riskCode = episode.riskTier.name,
            riskRationale = episode.riskRationale
        ),
        carePlan = CarePlanDto(
            id = "cp-${episode.id}-v${episode.carePlanRevision}",
            encounterRef = "Encounter/${episode.id}",
            revision = episode.carePlanRevision,
            authorDisplay = episode.attendingPhysician,
            summary = episode.dispositionNote,
            signedOff = true
        ),
        medications = medications.map { med ->
            MedicationRequestDto(
                id = med.id,
                encounterRef = "Encounter/${episode.id}",
                medicationName = "${med.name} ${med.strengthAndForm}",
                dosageInstruction = med.scheduleLabel,
                changeCategory = med.changeStatus.name,
                status = med.todayDoseStatus.name
            )
        },
        latestObservation = latestCheckIn?.let { v ->
            ObservationDto(
                id = v.id,
                encounterRef = "Encounter/${episode.id}",
                code = "vital-signs-panel",
                valueDisplay = "BP ${v.systolicMmHg}/${v.diastolicMmHg} mmHg, HR ${v.heartRateBpm} bpm, SpO2 ${v.spo2Percent}%, Wt ${v.weightKg} kg",
                effectiveDateTimeIso = v.timestampIso,
                performerDisplay = v.recordedByLabel
            )
        }
    )

    fun toPrettyJson(
        episode: DischargeEpisode,
        medications: List<MedicationItem>,
        latestCheckIn: VitalCheckIn?
    ): String = prettyJson.encodeToString(buildBundle(episode, medications, latestCheckIn))
}
