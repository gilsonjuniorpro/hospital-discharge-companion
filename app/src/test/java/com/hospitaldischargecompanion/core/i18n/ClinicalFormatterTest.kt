package com.hospitaldischargecompanion.core.i18n

import com.google.common.truth.Truth.assertThat
import com.hospitaldischargecompanion.core.demo.AppLocale
import com.hospitaldischargecompanion.core.demo.TemperatureUnit
import org.junit.Test

class ClinicalFormatterTest {

    @Test
    fun convertsAndFormatsTemperaturesAndWeightsAcrossLocales() {
        assertThat(ClinicalFormatter.formatTemperature(37.0, TemperatureUnit.CELSIUS, AppLocale.EN)).isEqualTo("37.0 °C")
        assertThat(ClinicalFormatter.formatTemperature(37.0, TemperatureUnit.FAHRENHEIT, AppLocale.EN)).isEqualTo("98.6 °F")
        assertThat(ClinicalFormatter.formatTemperature(37.5, TemperatureUnit.CELSIUS, AppLocale.PT_BR)).contains("37,5")
        assertThat(ClinicalFormatter.formatWeightKg(76.5, AppLocale.FR)).contains("76,5")
        assertThat(ClinicalFormatter.formatWeightDeltaKg(2.3, AppLocale.EN)).isEqualTo("+2.3 kg")
        assertThat(ClinicalFormatter.formatWeightDeltaKg(-0.8, AppLocale.EN)).isEqualTo("-0.8 kg")
        assertThat(ClinicalFormatter.fahrenheitToCelsius(98.6)).isWithin(0.05).of(37.0)
    }

    @Test
    fun classifiesBloodPressureThresholdsAndFormatsDates() {
        assertThat(ClinicalFormatter.classifyBloodPressure(88, 56)).isEqualTo(BloodPressureCategory.HYPOTENSIVE)
        assertThat(ClinicalFormatter.classifyBloodPressure(122, 78)).isEqualTo(BloodPressureCategory.NORMAL)
        assertThat(ClinicalFormatter.classifyBloodPressure(144, 86)).isEqualTo(BloodPressureCategory.ELEVATED)
        assertThat(ClinicalFormatter.classifyBloodPressure(172, 104)).isEqualTo(BloodPressureCategory.HYPERTENSIVE_URGENT)
        assertThat(ClinicalFormatter.classifyBloodPressure(186, 122)).isEqualTo(BloodPressureCategory.HYPERTENSIVE_CRITICAL)

        assertThat(ClinicalFormatter.formatIsoDate("2026-06-12", AppLocale.EN)).isNotEmpty()
        assertThat(ClinicalFormatter.formatIsoInstantShort("2026-06-15T08:15:00Z", AppLocale.ES)).isNotEmpty()
    }
}
