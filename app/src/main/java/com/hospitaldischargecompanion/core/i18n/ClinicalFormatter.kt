package com.hospitaldischargecompanion.core.i18n

import com.hospitaldischargecompanion.core.demo.AppLocale
import com.hospitaldischargecompanion.core.demo.TemperatureUnit
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

enum class BloodPressureCategory {
    HYPOTENSIVE,
    NORMAL,
    ELEVATED,
    HYPERTENSIVE_URGENT,
    HYPERTENSIVE_CRITICAL
}

object ClinicalFormatter {

    fun toJavaLocale(appLocale: AppLocale): Locale = when (appLocale) {
        AppLocale.PT_BR -> Locale.forLanguageTag("pt-BR")
        AppLocale.ES -> Locale.forLanguageTag("es")
        AppLocale.FR -> Locale.forLanguageTag("fr")
        AppLocale.EN, AppLocale.SYSTEM -> Locale.US
    }

    fun formatTemperature(tempC: Double, unit: TemperatureUnit, locale: AppLocale = AppLocale.EN): String {
        val value = if (unit == TemperatureUnit.FAHRENHEIT) celsiusToFahrenheit(tempC) else tempC
        val suffix = if (unit == TemperatureUnit.FAHRENHEIT) "°F" else "°C"
        val nf = NumberFormat.getNumberInstance(toJavaLocale(locale)).apply {
            minimumFractionDigits = 1
            maximumFractionDigits = 1
        }
        return "${nf.format(value)} $suffix"
    }

    fun celsiusToFahrenheit(tempC: Double): Double = (tempC * 9.0 / 5.0) + 32.0

    fun fahrenheitToCelsius(tempF: Double): Double = (tempF - 32.0) * 5.0 / 9.0

    fun formatWeightKg(weightKg: Double, locale: AppLocale = AppLocale.EN): String {
        val nf = NumberFormat.getNumberInstance(toJavaLocale(locale)).apply {
            minimumFractionDigits = 1
            maximumFractionDigits = 1
        }
        return "${nf.format(weightKg)} kg"
    }

    fun formatWeightDeltaKg(deltaKg: Double, locale: AppLocale = AppLocale.EN): String {
        val sign = if (deltaKg >= 0) "+" else "-"
        val nf = NumberFormat.getNumberInstance(toJavaLocale(locale)).apply {
            minimumFractionDigits = 1
            maximumFractionDigits = 1
        }
        return "$sign${nf.format(kotlin.math.abs(deltaKg))} kg"
    }

    fun classifyBloodPressure(systolicMmHg: Int, diastolicMmHg: Int): BloodPressureCategory = when {
        systolicMmHg <= 90 || diastolicMmHg <= 55 -> BloodPressureCategory.HYPOTENSIVE
        systolicMmHg >= 180 || diastolicMmHg >= 120 -> BloodPressureCategory.HYPERTENSIVE_CRITICAL
        systolicMmHg >= 160 || diastolicMmHg >= 100 -> BloodPressureCategory.HYPERTENSIVE_URGENT
        systolicMmHg >= 135 || diastolicMmHg >= 85 -> BloodPressureCategory.ELEVATED
        else -> BloodPressureCategory.NORMAL
    }

    fun formatIsoDate(isoDate: String, locale: AppLocale = AppLocale.EN): String = runCatching {
        val parsed = LocalDate.parse(isoDate)
        parsed.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(toJavaLocale(locale)))
    }.getOrDefault(isoDate)

    fun formatIsoInstantShort(isoInstant: String, locale: AppLocale = AppLocale.EN): String = runCatching {
        val instant = Instant.parse(isoInstant)
        DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
            .withLocale(toJavaLocale(locale))
            .withZone(ZoneOffset.UTC)
            .format(instant)
    }.getOrDefault(isoInstant)
}
