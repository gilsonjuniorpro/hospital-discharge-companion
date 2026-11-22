package com.hospitaldischargecompanion.core.alerts

import com.hospitaldischargecompanion.core.demo.DomainResult
import com.hospitaldischargecompanion.core.demo.UserRole
import com.hospitaldischargecompanion.core.telemonitor.VitalBaseline
import com.hospitaldischargecompanion.core.telemonitor.VitalCheckIn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EvaluateAlertRulesUseCase @Inject constructor(
    private val alertRepository: AlertRepository
) {
    suspend fun evaluateCheckIn(
        checkIn: VitalCheckIn,
        baseline: VitalBaseline?
    ): List<ClinicalAlert> {
        val created = mutableListOf<ClinicalAlert>()
        val weightGain = baseline?.let { checkIn.weightKg - it.dryWeightKg } ?: 0.0

        if (checkIn.isBloodPressureAbnormal || checkIn.isHypoxic || checkIn.isFebrile) {
            val isCritical = checkIn.systolicMmHg >= 180 || checkIn.spo2Percent < 90 || checkIn.temperatureC >= 39.0
            val result = alertRepository.raiseAlert(
                episodeId = checkIn.episodeId,
                type = AlertType.ABNORMAL_VITAL,
                severity = if (isCritical) AlertSeverity.CRITICAL else AlertSeverity.URGENT,
                escalationStage = if (isCritical) EscalationStage.PHYSICIAN_REVIEW else EscalationStage.NURSE_TRIAGE,
                title = buildString {
                    append("Abnormal vitals: BP ${checkIn.systolicMmHg}/${checkIn.diastolicMmHg} mmHg")
                    if (checkIn.isHypoxic) append(", SpO2 ${checkIn.spo2Percent}%")
                    if (checkIn.isFebrile) append(", Temp ${checkIn.temperatureC} °C")
                },
                summary = "Logged by ${checkIn.recordedByLabel}. HR ${checkIn.heartRateBpm} bpm, Weight ${checkIn.weightKg} kg. ${checkIn.freeTextNotes}".trim(),
                patientGuidance = "Sit quietly for 5 minutes and recheck your blood pressure. Your care team has been notified. If chest pain or severe breathlessness occurs, seek emergency care (demo copy only).",
                actorName = checkIn.recordedByLabel,
                actorRole = UserRole.PATIENT,
                reasonCode = if (isCritical) "CRITICAL_VITAL_THRESHOLD" else "ABNORMAL_VITAL_THRESHOLD"
            )
            if (result is DomainResult.Success) created.add(result.data)
        }

        if (weightGain >= 2.0) {
            val result = alertRepository.raiseAlert(
                episodeId = checkIn.episodeId,
                type = AlertType.ABNORMAL_VITAL,
                severity = AlertSeverity.URGENT,
                escalationStage = EscalationStage.NURSE_TRIAGE,
                title = "Rapid weight gain (+${String.format("%.1f", weightGain)} kg above dry weight)",
                summary = "Current weight ${checkIn.weightKg} kg vs baseline ${baseline?.dryWeightKg} kg.",
                patientGuidance = "Hold extra fluids, avoid added sodium, and await nurse diuretic review.",
                actorName = checkIn.recordedByLabel,
                actorRole = UserRole.PATIENT,
                reasonCode = "WEIGHT_GAIN_THRESHOLD"
            )
            if (result is DomainResult.Success) created.add(result.data)
        }

        if (checkIn.dyspneaSeverity >= 4 || checkIn.painScore >= 7) {
            val result = alertRepository.raiseAlert(
                episodeId = checkIn.episodeId,
                type = AlertType.RED_FLAG_SYMPTOM,
                severity = AlertSeverity.CRITICAL,
                escalationStage = EscalationStage.PHYSICIAN_REVIEW,
                title = "Red-flag symptom reported (Dyspnea ${checkIn.dyspneaSeverity}/5, Pain ${checkIn.painScore}/10)",
                summary = "Patient/caregiver reported high symptom burden: ${checkIn.freeTextNotes.ifBlank { "No note provided" }}",
                patientGuidance = "Rest upright and contact your care team immediately. If symptoms worsen rapidly, seek urgent in-person care (demo copy only).",
                actorName = checkIn.recordedByLabel,
                actorRole = UserRole.PATIENT,
                reasonCode = "RED_FLAG_SYMPTOM_SCORE"
            )
            if (result is DomainResult.Success) created.add(result.data)
        }

        return created
    }

    suspend fun evaluateMissedMedication(
        episodeId: String,
        medicationName: String,
        attributionLabel: String,
        reason: String?
    ): DomainResult<ClinicalAlert> = alertRepository.raiseAlert(
        episodeId = episodeId,
        type = AlertType.MISSED_MEDICATION,
        severity = AlertSeverity.WARNING,
        escalationStage = EscalationStage.NURSE_TRIAGE,
        title = "Skipped medication: $medicationName",
        summary = "Marked skipped by $attributionLabel. Reason: ${reason?.ifBlank { "Not specified" } ?: "Not specified"}",
        patientGuidance = "Do not double the next dose unless instructed. A nurse will check in on side effects or supply issues.",
        actorName = attributionLabel,
        actorRole = UserRole.PATIENT,
        reasonCode = "MISSED_MEDICATION_DOSE"
    )
}
