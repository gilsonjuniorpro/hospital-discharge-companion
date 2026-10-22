package com.hospitaldischargecompanion.core.navigation

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DeepLinkParserTest {

    @Test
    fun parsesEpisodeAlertDoseAndDemoScreenDeepLinks() {
        assertThat(DeepLinkParser.parse("hdc://episode/ep-ps-002"))
            .isEqualTo(HdcRoute.PatientHome("ep-ps-002"))
        assertThat(DeepLinkParser.parse("hdc://alert/alert-hf-weight"))
            .isEqualTo(HdcRoute.Alerts("ep-hf-001", "alert-hf-weight"))
        assertThat(DeepLinkParser.parse("hdc://dose/med-hf-furosemide"))
            .isEqualTo(HdcRoute.Medications("ep-hf-001", "med-hf-furosemide"))
        assertThat(DeepLinkParser.parse("hdc://screen/nurse"))
            .isEqualTo(HdcRoute.NurseWorkstation)
        assertThat(DeepLinkParser.parse("hdc://screen/physician"))
            .isEqualTo(HdcRoute.PhysicianWorkstation)
        assertThat(DeepLinkParser.parse("https://example.com/ignored")).isNull()
    }
}
