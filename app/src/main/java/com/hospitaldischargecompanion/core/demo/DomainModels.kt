package com.hospitaldischargecompanion.core.demo

enum class UserRole {
    PATIENT,
    FAMILY,
    NURSE,
    PHYSICIAN;

    val isClinician: Boolean
        get() = this == NURSE || this == PHYSICIAN

    val canModifyCarePlan: Boolean
        get() = this == PHYSICIAN

    val canReopenTasks: Boolean
        get() = this == NURSE || this == PHYSICIAN

    val canWriteClinicalNotes: Boolean
        get() = this == NURSE || this == PHYSICIAN
}

enum class AppLocale(val languageTag: String) {
    SYSTEM(""),
    EN("en"),
    PT_BR("pt-BR"),
    ES("es"),
    FR("fr");

    companion object {
        fun fromTag(tag: String?): AppLocale =
            entries.firstOrNull { it.languageTag.equals(tag, ignoreCase = true) } ?: SYSTEM
    }
}

enum class TemperatureUnit {
    CELSIUS,
    FAHRENHEIT
}

enum class AppThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

enum class SimulatedFailureMode {
    NONE,
    TIMEOUT,
    NETWORK_UNAVAILABLE,
    EHR_UNAUTHORIZED_401,
    EHR_CONFLICT_409
}

data class DemoNetworkConfig(
    val latencyMs: Long = 150L,
    val failureMode: SimulatedFailureMode = SimulatedFailureMode.NONE
)

data class DemoSessionState(
    val activeRole: UserRole? = null,
    val selectedEpisodeId: String = "ep-hf-001",
    val locale: AppLocale = AppLocale.EN,
    val temperatureUnit: TemperatureUnit = TemperatureUnit.CELSIUS,
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val quietHoursEnabled: Boolean = false,
    val caregiverAlertsEnabled: Boolean = true
)
