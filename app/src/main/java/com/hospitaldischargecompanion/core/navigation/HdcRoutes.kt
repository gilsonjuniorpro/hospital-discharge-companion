package com.hospitaldischargecompanion.core.navigation

import kotlinx.serialization.Serializable

sealed interface HdcRoute {
    @Serializable
    data object ProfileSelection : HdcRoute

    @Serializable
    data class PatientHome(val episodeId: String? = null) : HdcRoute

    @Serializable
    data class FamilyHome(val episodeId: String? = null) : HdcRoute

    @Serializable
    data object NurseWorkstation : HdcRoute

    @Serializable
    data object PhysicianWorkstation : HdcRoute

    @Serializable
    data class CarePlan(val episodeId: String) : HdcRoute

    @Serializable
    data class Medications(val episodeId: String, val highlightMedicationId: String? = null) : HdcRoute

    @Serializable
    data class Telemonitoring(val episodeId: String) : HdcRoute

    @Serializable
    data class Alerts(val episodeId: String, val highlightAlertId: String? = null) : HdcRoute

    @Serializable
    data class Messaging(val episodeId: String) : HdcRoute

    @Serializable
    data class Appointments(val episodeId: String) : HdcRoute

    @Serializable
    data class Education(val episodeId: String) : HdcRoute

    @Serializable
    data class EhrSync(val episodeId: String) : HdcRoute

    @Serializable
    data object DemoControls : HdcRoute
}
