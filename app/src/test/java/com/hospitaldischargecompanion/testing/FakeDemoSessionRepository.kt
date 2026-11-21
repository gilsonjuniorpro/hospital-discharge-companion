package com.hospitaldischargecompanion.testing

import com.hospitaldischargecompanion.core.demo.AppLocale
import com.hospitaldischargecompanion.core.demo.AppThemeMode
import com.hospitaldischargecompanion.core.demo.DemoSessionRepository
import com.hospitaldischargecompanion.core.demo.DemoSessionState
import com.hospitaldischargecompanion.core.demo.TemperatureUnit
import com.hospitaldischargecompanion.core.demo.UserRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class FakeDemoSessionRepository(
    initial: DemoSessionState = DemoSessionState()
) : DemoSessionRepository {
    private val state = MutableStateFlow(initial)
    override val sessionState: Flow<DemoSessionState> = state.asStateFlow()

    val current: DemoSessionState
        get() = state.value

    override suspend fun setActiveRole(role: UserRole?) {
        state.update { it.copy(activeRole = role) }
    }

    override suspend fun setSelectedEpisode(episodeId: String) {
        state.update { it.copy(selectedEpisodeId = episodeId) }
    }

    override suspend fun setLocale(locale: AppLocale) {
        state.update { it.copy(locale = locale) }
    }

    override suspend fun setTemperatureUnit(unit: TemperatureUnit) {
        state.update { it.copy(temperatureUnit = unit) }
    }

    override suspend fun setThemeMode(mode: AppThemeMode) {
        state.update { it.copy(themeMode = mode) }
    }

    override suspend fun setQuietHoursEnabled(enabled: Boolean) {
        state.update { it.copy(quietHoursEnabled = enabled) }
    }

    override suspend fun setCaregiverAlertsEnabled(enabled: Boolean) {
        state.update { it.copy(caregiverAlertsEnabled = enabled) }
    }

    override suspend fun resetSession() {
        state.value = DemoSessionState(locale = state.value.locale)
    }
}
