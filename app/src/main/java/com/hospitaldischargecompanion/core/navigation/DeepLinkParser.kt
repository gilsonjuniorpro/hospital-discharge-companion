package com.hospitaldischargecompanion.core.navigation

import com.hospitaldischargecompanion.core.demo.ClinicalSeedFactory

object DeepLinkParser {
    fun parse(uriString: String?): HdcRoute? {
        val raw = uriString?.trim()?.takeIf { it.startsWith("hdc://", ignoreCase = true) } ?: return null
        val withoutScheme = raw.substringAfter("://")
        val segments = withoutScheme.split("/").filter { it.isNotBlank() }
        if (segments.isEmpty()) return null

        val host = segments[0].lowercase()
        val arg1 = segments.getOrNull(1)
        val arg2 = segments.getOrNull(2)
        val defaultEp = ClinicalSeedFactory.HEART_FAILURE_EPISODE_ID

        return when (host) {
            "episode" -> HdcRoute.PatientHome(episodeId = arg1 ?: defaultEp)
            "alert" -> HdcRoute.Alerts(
                episodeId = arg2 ?: defaultEp,
                highlightAlertId = arg1
            )
            "dose", "medication" -> HdcRoute.Medications(
                episodeId = arg2 ?: defaultEp,
                highlightMedicationId = arg1
            )
            "screen" -> when (arg1?.lowercase()) {
                "profile" -> HdcRoute.ProfileSelection
                "patient" -> HdcRoute.PatientHome(arg2 ?: defaultEp)
                "family" -> HdcRoute.FamilyHome(arg2 ?: defaultEp)
                "nurse" -> HdcRoute.NurseWorkstation
                "physician" -> HdcRoute.PhysicianWorkstation
                "careplan" -> HdcRoute.CarePlan(arg2 ?: defaultEp)
                "medications" -> HdcRoute.Medications(arg2 ?: defaultEp)
                "telemonitoring" -> HdcRoute.Telemonitoring(arg2 ?: defaultEp)
                "alerts" -> HdcRoute.Alerts(arg2 ?: defaultEp)
                "messaging" -> HdcRoute.Messaging(arg2 ?: defaultEp)
                "appointments" -> HdcRoute.Appointments(arg2 ?: defaultEp)
                "education" -> HdcRoute.Education(arg2 ?: defaultEp)
                "ehr" -> HdcRoute.EhrSync(arg2 ?: defaultEp)
                "demo" -> HdcRoute.DemoControls
                else -> null
            }
            else -> null
        }
    }
}
