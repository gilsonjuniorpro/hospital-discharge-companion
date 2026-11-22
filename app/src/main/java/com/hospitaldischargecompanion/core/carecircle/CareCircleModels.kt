package com.hospitaldischargecompanion.core.carecircle

enum class CaregiverPermission {
    VIEW_ONLY,
    ASSIST_MEDS_AND_CHECKINS,
    ESCALATE
}

enum class CareCircleMemberStatus {
    INVITED,
    ACTIVE,
    REVOKED
}

data class CareCircleMember(
    val id: String,
    val episodeId: String,
    val name: String,
    val relationship: String,
    val permission: CaregiverPermission,
    val status: CareCircleMemberStatus,
    val inviteCodeUsed: String
) {
    fun dualAttribution(patientFirstName: String): String =
        "$name ($relationship) on behalf of $patientFirstName"
}
