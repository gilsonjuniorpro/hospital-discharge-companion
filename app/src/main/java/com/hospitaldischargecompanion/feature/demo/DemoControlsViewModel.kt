package com.hospitaldischargecompanion.feature.demo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hospitaldischargecompanion.core.demo.AppLocale
import com.hospitaldischargecompanion.core.demo.AppThemeMode
import com.hospitaldischargecompanion.core.demo.DemoNetworkConfig
import com.hospitaldischargecompanion.core.demo.DemoNetworkSimulator
import com.hospitaldischargecompanion.core.demo.DemoScenarioPreset
import com.hospitaldischargecompanion.core.demo.DemoScenarioRunner
import com.hospitaldischargecompanion.core.demo.DemoSessionRepository
import com.hospitaldischargecompanion.core.demo.DemoSessionState
import com.hospitaldischargecompanion.core.demo.MockClinicalStore
import com.hospitaldischargecompanion.core.demo.SimulatedFailureMode
import com.hospitaldischargecompanion.core.demo.TemperatureUnit
import com.hospitaldischargecompanion.core.notifications.DemoNotificationItem
import com.hospitaldischargecompanion.core.notifications.NotificationCategory
import com.hospitaldischargecompanion.core.notifications.NotificationCenterRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DemoControlsUiState(
    val session: DemoSessionState = DemoSessionState(),
    val networkConfig: DemoNetworkConfig = DemoNetworkConfig(),
    val notifications: List<DemoNotificationItem> = emptyList(),
    val statusBanner: String? = null
)

@HiltViewModel
class DemoControlsViewModel @Inject constructor(
    private val sessionRepository: DemoSessionRepository,
    private val store: MockClinicalStore,
    private val networkSimulator: DemoNetworkSimulator,
    private val notificationRepository: NotificationCenterRepository,
    private val scenarioRunner: DemoScenarioRunner
) : ViewModel() {

    private val _banner = MutableStateFlow<String?>(null)

    val uiState: StateFlow<DemoControlsUiState> = combine(
        sessionRepository.sessionState,
        networkSimulator.config,
        notificationRepository.observeNotifications(),
        _banner
    ) { session, net, notifs, banner ->
        DemoControlsUiState(
            session = session,
            networkConfig = net,
            notifications = notifs,
            statusBanner = banner
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DemoControlsUiState()
    )

    fun setLocale(locale: AppLocale) {
        viewModelScope.launch { sessionRepository.setLocale(locale) }
    }

    fun setTemperatureUnit(unit: TemperatureUnit) {
        viewModelScope.launch { sessionRepository.setTemperatureUnit(unit) }
    }

    fun setThemeMode(mode: AppThemeMode) {
        viewModelScope.launch { sessionRepository.setThemeMode(mode) }
    }

    fun setQuietHours(enabled: Boolean) {
        viewModelScope.launch { sessionRepository.setQuietHoursEnabled(enabled) }
    }

    fun setCaregiverAlerts(enabled: Boolean) {
        viewModelScope.launch { sessionRepository.setCaregiverAlertsEnabled(enabled) }
    }

    fun setLatencyMs(ms: Long) {
        networkSimulator.setLatencyMs(ms)
    }

    fun setFailureMode(mode: SimulatedFailureMode) {
        networkSimulator.setFailureMode(mode)
    }

    fun triggerSampleNotification() {
        val epId = uiState.value.session.selectedEpisodeId
        viewModelScope.launch {
            notificationRepository.pushLocalReminder(
                episodeId = epId,
                category = NotificationCategory.MEDICATION_DUE,
                title = "Demo Reminder: Evening Furosemide & Dry Weight",
                message = "Tap to jump directly into Medication Management.",
                deepLinkUri = "hdc://dose/med-hf-furosemide"
            )
            _banner.value = "Queued local in-app demo reminder."
        }
    }

    fun markNotificationRead(id: String) {
        viewModelScope.launch { notificationRepository.markRead(id) }
    }

    fun applyScenario(preset: DemoScenarioPreset) {
        viewModelScope.launch {
            _banner.value = scenarioRunner.apply(preset)
        }
    }

    fun resetDemonstration() {
        viewModelScope.launch {
            store.resetAll()
            networkSimulator.reset()
            sessionRepository.resetSession()
            _banner.value = "Demonstration reset to initial seeded clinical episodes."
        }
    }
}
