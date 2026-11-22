package com.hospitaldischargecompanion.core.navigation

import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Test

class HdcRoutesTest {
    private val json = Json

    @Test
    fun patientHomeRouteRoundTripsWithOptionalEpisodeId() {
        val route = HdcRoute.PatientHome(episodeId = "ep-hf-001")
        val encoded = json.encodeToString(route)
        val decoded = json.decodeFromString<HdcRoute.PatientHome>(encoded)
        assertThat(decoded).isEqualTo(route)
        assertThat(HdcRoute.PatientHome().episodeId).isNull()
    }

    @Test
    fun medicationsAndAlertsRoutesPreserveHighlightIdentifiers() {
        val medRoute = HdcRoute.Medications(episodeId = "ep-hf-001", highlightMedicationId = "med-furosemide")
        val alertRoute = HdcRoute.Alerts(episodeId = "ep-hf-001", highlightAlertId = "alert-bp-01")

        assertThat(json.decodeFromString<HdcRoute.Medications>(json.encodeToString(medRoute)))
            .isEqualTo(medRoute)
        assertThat(json.decodeFromString<HdcRoute.Alerts>(json.encodeToString(alertRoute)))
            .isEqualTo(alertRoute)
    }
}
