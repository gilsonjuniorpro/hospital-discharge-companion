package com.hospitaldischargecompanion.core.ui

object HdcTestTags {
    const val SCREEN_PROFILE = "screen_profile"
    const val SCREEN_PATIENT = "screen_patient"
    const val SCREEN_FAMILY = "screen_family"
    const val SCREEN_NURSE = "screen_nurse"
    const val SCREEN_PHYSICIAN = "screen_physician"
    const val SCREEN_CARE_PLAN = "screen_care_plan"
    const val SCREEN_MEDICATIONS = "screen_medications"
    const val SCREEN_TELEMONITORING = "screen_telemonitoring"
    const val SCREEN_ALERTS = "screen_alerts"
    const val SCREEN_MESSAGING = "screen_messaging"
    const val SCREEN_APPOINTMENTS = "screen_appointments"
    const val SCREEN_EDUCATION = "screen_education"
    const val SCREEN_EHR_SYNC = "screen_ehr_sync"
    const val SCREEN_DEMO_CONTROLS = "screen_demo_controls"

    fun allScreenTags(): List<String> = listOf(
        SCREEN_PROFILE,
        SCREEN_PATIENT,
        SCREEN_FAMILY,
        SCREEN_NURSE,
        SCREEN_PHYSICIAN,
        SCREEN_CARE_PLAN,
        SCREEN_MEDICATIONS,
        SCREEN_TELEMONITORING,
        SCREEN_ALERTS,
        SCREEN_MESSAGING,
        SCREEN_APPOINTMENTS,
        SCREEN_EDUCATION,
        SCREEN_EHR_SYNC,
        SCREEN_DEMO_CONTROLS
    )
}
