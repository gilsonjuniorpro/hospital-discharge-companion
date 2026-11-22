package com.hospitaldischargecompanion.core.demo

import com.hospitaldischargecompanion.core.discharge.DischargeEpisode
import com.hospitaldischargecompanion.core.discharge.EhrSyncStatus
import com.hospitaldischargecompanion.core.discharge.EpisodeCaseType
import com.hospitaldischargecompanion.core.discharge.RiskTier

object ClinicalSeedFactory {
    const val HEART_FAILURE_EPISODE_ID = "ep-hf-001"
    const val POST_SURGERY_EPISODE_ID = "ep-ps-002"
    const val PNEUMONIA_EPISODE_ID = "ep-pn-003"

    fun createEpisodes(): List<DischargeEpisode> = listOf(
        DischargeEpisode(
            id = HEART_FAILURE_EPISODE_ID,
            caseType = EpisodeCaseType.HEART_FAILURE,
            patientId = "pat-joao-01",
            patientName = "João Silva",
            patientAge = 68,
            mrnMasked = "MRN-••4821",
            diagnosisTitle = "Heart Failure (HFrEF) Exacerbation",
            diagnosisSummary = "Discharged after 4-day IV diuresis for volume overload. Dry weight target 76.5 kg; daily weight, blood pressure, and loop diuretic adherence required.",
            dischargeDateIso = "2026-06-12",
            dischargeDayNumber = 3,
            hospitalName = "St. Jude Regional Medical Center",
            hospitalUnit = "Cardiology Step-Down 4B",
            attendingPhysician = "Dr. Elena Vance, MD",
            primaryNurse = "RN Clara Mendes",
            riskTier = RiskTier.HIGH,
            riskRationale = "Recent IV diuresis, high 30-day readmission score (LACE 13), diuretic dose uptitration.",
            consentSigned = true,
            careCircleInviteCode = "HDC-HF-402",
            redFlags = listOf(
                "Weight gain of 2 kg (4.4 lb) or more in 2 days",
                "Systolic blood pressure above 160 mmHg or below 90 mmHg",
                "Worsening shortness of breath at rest or needing extra pillows to sleep",
                "Chest pain, fainting, or SpO2 below 92%"
            ),
            ehrSyncStatus = EhrSyncStatus.SYNCED,
            lastEhrSyncIso = "2026-06-15T08:15:00Z",
            carePlanRevision = 1,
            dispositionNote = "Continue home telemonitoring; cardiology clinic follow-up in 4 days."
        ),
        DischargeEpisode(
            id = POST_SURGERY_EPISODE_ID,
            caseType = EpisodeCaseType.POST_SURGERY,
            patientId = "pat-maria-02",
            patientName = "Maria Oliveira",
            patientAge = 54,
            mrnMasked = "MRN-••7390",
            diagnosisTitle = "Post-Operative Laparoscopic Cholecystectomy",
            diagnosisSummary = "Uncomplicated gallbladder removal with 4 laparoscopic port incisions. Monitor surgical sites, pain control, early ambulation, and low-fat diet.",
            dischargeDateIso = "2026-06-13",
            dischargeDayNumber = 2,
            hospitalName = "St. Jude Regional Medical Center",
            hospitalUnit = "Surgical Recovery 2A",
            attendingPhysician = "Dr. Marcus Sterling, MD",
            primaryNurse = "RN David Park",
            riskTier = RiskTier.MEDIUM,
            riskRationale = "Post-surgical infection watch window and multimodal analgesic tapering.",
            consentSigned = true,
            careCircleInviteCode = "HDC-PS-719",
            redFlags = listOf(
                "Fever above 38.0 °C (100.4 °F) or chills",
                "Spreading redness, warmth, or purulent drainage at any incision",
                "Severe uncontrolled abdominal pain (7/10 or higher)",
                "Persistent nausea, vomiting, or yellowing of skin/eyes"
            ),
            ehrSyncStatus = EhrSyncStatus.SYNCED,
            lastEhrSyncIso = "2026-06-15T07:50:00Z",
            carePlanRevision = 1,
            dispositionNote = "Daily incision check and surgical outpatient review on day 10."
        ),
        DischargeEpisode(
            id = PNEUMONIA_EPISODE_ID,
            caseType = EpisodeCaseType.PNEUMONIA,
            patientId = "pat-arthur-03",
            patientName = "Arthur Bennett",
            patientAge = 72,
            mrnMasked = "MRN-••1904",
            diagnosisTitle = "Community-Acquired Pneumonia (Recovery)",
            diagnosisSummary = "Transitioned from inpatient IV antibiotics to oral amoxicillin-clavulanate. Track SpO2, temperature, hydration, and breathing exercises.",
            dischargeDateIso = "2026-06-11",
            dischargeDayNumber = 4,
            hospitalName = "St. Jude Regional Medical Center",
            hospitalUnit = "Internal Medicine 3C",
            attendingPhysician = "Dr. Elena Vance, MD",
            primaryNurse = "RN Clara Mendes",
            riskTier = RiskTier.LOW,
            riskRationale = "Afebrile for 48 hours on room air; completing oral antibiotic course at home.",
            consentSigned = true,
            careCircleInviteCode = "HDC-PN-285",
            redFlags = listOf(
                "Oxygen saturation (SpO2) dropping below 93%",
                "Recurrent fever above 38.0 °C or worsening productive cough",
                "New confusion, severe fatigue, or rapid breathing",
                "Inability to keep fluids or oral antibiotics down"
            ),
            ehrSyncStatus = EhrSyncStatus.PENDING,
            lastEhrSyncIso = "2026-06-14T19:40:00Z",
            carePlanRevision = 1,
            dispositionNote = "Complete 5-day oral antibiotic regimen; primary care chest follow-up in 2 weeks."
        )
    )
}
