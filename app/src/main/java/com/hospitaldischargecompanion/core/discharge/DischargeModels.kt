package com.hospitaldischargecompanion.core.discharge

enum class RiskTier {
    LOW,
    MEDIUM,
    HIGH
}

enum class EpisodeCaseType {
    HEART_FAILURE,
    POST_SURGERY,
    PNEUMONIA
}

enum class EhrSyncStatus {
    SYNCED,
    PENDING,
    CONFLICT,
    FAILED
}

data class DischargeEpisode(
    val id: String,
    val caseType: EpisodeCaseType,
    val patientId: String,
    val patientName: String,
    val patientAge: Int,
    val mrnMasked: String,
    val diagnosisTitle: String,
    val diagnosisSummary: String,
    val dischargeDateIso: String,
    val dischargeDayNumber: Int,
    val hospitalName: String,
    val hospitalUnit: String,
    val attendingPhysician: String,
    val primaryNurse: String,
    val riskTier: RiskTier,
    val riskRationale: String,
    val consentSigned: Boolean,
    val careCircleInviteCode: String,
    val redFlags: List<String>,
    val ehrSyncStatus: EhrSyncStatus,
    val lastEhrSyncIso: String,
    val carePlanRevision: Int = 1,
    val dispositionNote: String = "Continue home telemonitoring and day-7 clinic follow-up."
)
