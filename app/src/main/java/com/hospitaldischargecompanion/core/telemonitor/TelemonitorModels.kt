package com.hospitaldischargecompanion.core.telemonitor

data class VitalBaseline(
    val episodeId: String,
    val dryWeightKg: Double,
    val targetSystolicMaxMmHg: Int,
    val targetSystolicMinMmHg: Int,
    val targetSpo2MinPercent: Int,
    val targetTempMaxC: Double
)

data class VitalCheckIn(
    val id: String,
    val episodeId: String,
    val dayNumber: Int,
    val timestampIso: String,
    val systolicMmHg: Int,
    val diastolicMmHg: Int,
    val heartRateBpm: Int,
    val spo2Percent: Int,
    val weightKg: Double,
    val temperatureC: Double,
    val painScore: Int,
    val dyspneaSeverity: Int,
    val swellingSeverity: Int,
    val freeTextNotes: String,
    val recordedByLabel: String,
    val idempotencyKey: String
) {
    val isBloodPressureAbnormal: Boolean
        get() = systolicMmHg >= 160 || systolicMmHg <= 90 || diastolicMmHg >= 100

    val isHypoxic: Boolean
        get() = spo2Percent < 92

    val isFebrile: Boolean
        get() = temperatureC >= 38.0
}

data class NewCheckInRequest(
    val episodeId: String,
    val systolicMmHg: Int,
    val diastolicMmHg: Int,
    val heartRateBpm: Int,
    val spo2Percent: Int,
    val weightKg: Double,
    val temperatureC: Double,
    val painScore: Int,
    val dyspneaSeverity: Int,
    val swellingSeverity: Int,
    val freeTextNotes: String,
    val recordedByLabel: String,
    val idempotencyKey: String
)

enum class WearableConnectionStatus {
    DISCONNECTED,
    PAIRED_MOCK_DEMO
}

data class WearableDeviceSummary(
    val id: String,
    val name: String,
    val modality: String,
    val status: WearableConnectionStatus,
    val lastReadingLabel: String
)
