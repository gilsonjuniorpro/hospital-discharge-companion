package com.hospitaldischargecompanion.core.alerts

import com.hospitaldischargecompanion.core.demo.ClinicalSeedFactory
import com.hospitaldischargecompanion.core.demo.UserRole

object AlertSeedFactory {
    fun createAlerts(): List<ClinicalAlert> = listOf(
        ClinicalAlert(
            id = "alert-hf-weight",
            episodeId = ClinicalSeedFactory.HEART_FAILURE_EPISODE_ID,
            type = AlertType.ABNORMAL_VITAL,
            severity = AlertSeverity.WARNING,
            status = AlertStatus.OPEN,
            escalationStage = EscalationStage.NURSE_TRIAGE,
            title = "Dry weight trending up (+1.1 kg in 48h)",
            summary = "Weight increased from 76.5 kg at discharge to 77.6 kg with mild evening ankle swelling.",
            patientGuidance = "Limit daily fluids to 1.5 L, avoid salty foods, confirm morning furosemide, and repeat weight tomorrow morning.",
            createdAtIso = "2026-06-14T08:25:00Z",
            auditTrail = listOf(
                AlertAuditEntry(
                    timestampIso = "2026-06-14T08:25:00Z",
                    actorName = "Telemonitoring Rules Engine",
                    actorRole = UserRole.NURSE,
                    actionSummary = "Opened warning alert routed to Nurse Triage queue",
                    reasonCode = "WEIGHT_TREND_UP"
                )
            )
        ),
        ClinicalAlert(
            id = "alert-hf-overdue",
            episodeId = ClinicalSeedFactory.HEART_FAILURE_EPISODE_ID,
            type = AlertType.CARE_PLAN_OVERDUE,
            severity = AlertSeverity.INFO,
            status = AlertStatus.OPEN,
            escalationStage = EscalationStage.PATIENT_FAMILY,
            title = "Afternoon indoor walk overdue",
            summary = "Day 3 light ambulation task has not yet been confirmed.",
            patientGuidance = "Take a gentle 10-minute indoor walk if feeling steady, or skip with reason if short of breath.",
            createdAtIso = "2026-06-15T16:30:00Z",
            auditTrail = listOf(
                AlertAuditEntry(
                    timestampIso = "2026-06-15T16:30:00Z",
                    actorName = "Care Plan Scheduler",
                    actorRole = UserRole.NURSE,
                    actionSummary = "Created overdue care-plan reminder for patient and family",
                    reasonCode = "TASK_OVERDUE"
                )
            )
        ),
        ClinicalAlert(
            id = "alert-ps-pain",
            episodeId = ClinicalSeedFactory.POST_SURGERY_EPISODE_ID,
            type = AlertType.RED_FLAG_SYMPTOM,
            severity = AlertSeverity.URGENT,
            status = AlertStatus.ESCALATED,
            escalationStage = EscalationStage.PHYSICIAN_REVIEW,
            title = "Post-op nausea medication out of stock + pain 4/10",
            summary = "Patient reported pharmacy stockout for Ondansetron ODT on day 2 post-cholecystectomy.",
            patientGuidance = "Stay hydrated with small sips; care team is reviewing an alternate pharmacy or antiemetic.",
            createdAtIso = "2026-06-15T10:10:00Z",
            auditTrail = listOf(
                AlertAuditEntry(
                    timestampIso = "2026-06-15T10:10:00Z",
                    actorName = "RN David Park",
                    actorRole = UserRole.NURSE,
                    actionSummary = "Escalated pharmacy stockout and symptom management to Dr. Marcus Sterling",
                    reasonCode = "MED_OUT_OF_STOCK"
                )
            )
        )
    )
}
