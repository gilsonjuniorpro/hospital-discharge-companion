package com.hospitaldischargecompanion.feature.alerts

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.hospitaldischargecompanion.core.alerts.AlertRepository
import com.hospitaldischargecompanion.core.alerts.ClinicalAlert
import com.hospitaldischargecompanion.core.demo.DemoSessionRepository
import com.hospitaldischargecompanion.core.demo.DomainResult
import com.hospitaldischargecompanion.core.demo.UserRole
import com.hospitaldischargecompanion.core.discharge.DischargeEpisode
import com.hospitaldischargecompanion.core.discharge.DischargeEpisodeRepository
import com.hospitaldischargecompanion.core.navigation.HdcRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AlertsUiState(
    val episode: DischargeEpisode? = null,
    val activeRole: UserRole = UserRole.PATIENT,
    val highlightAlertId: String? = null,
    val alerts: List<ClinicalAlert> = emptyList(),
    val errorMessage: String? = null,
    val isLoading: Boolean = true
)

@HiltViewModel
class AlertsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    sessionRepository: DemoSessionRepository,
    episodeRepository: DischargeEpisodeRepository,
    private val alertRepository: AlertRepository
) : ViewModel() {

    private val route = savedStateHandle.toRoute<HdcRoute.Alerts>()
    private val _error = MutableStateFlow<String?>(null)

    val uiState: StateFlow<AlertsUiState> = combine(
        episodeRepository.observeEpisode(route.episodeId),
        sessionRepository.sessionState,
        alertRepository.observeEpisodeAlerts(route.episodeId),
        _error
    ) { episode, session, alerts, error ->
        AlertsUiState(
            episode = episode,
            activeRole = session.activeRole ?: UserRole.PATIENT,
            highlightAlertId = route.highlightAlertId,
            alerts = alerts,
            errorMessage = error,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AlertsUiState()
    )

    private fun actorName(): String {
        val state = uiState.value
        return when (state.activeRole) {
            UserRole.PATIENT -> state.episode?.patientName ?: "Patient"
            UserRole.FAMILY -> "Maria Silva (Family Caregiver)"
            UserRole.NURSE -> state.episode?.primaryNurse ?: "Primary Nurse"
            UserRole.PHYSICIAN -> state.episode?.attendingPhysician ?: "Attending Physician"
        }
    }

    fun acknowledge(alertId: String) = perform {
        alertRepository.acknowledge(alertId, actorName(), uiState.value.activeRole)
    }

    fun snooze(alertId: String) = perform {
        alertRepository.snooze(alertId, actorName(), uiState.value.activeRole, "AWAITING_RECHECK")
    }

    fun escalate(alertId: String) = perform {
        alertRepository.escalate(alertId, actorName(), uiState.value.activeRole, "CARE_TEAM_ESCALATION")
    }

    fun resolve(alertId: String) = perform {
        alertRepository.resolve(alertId, actorName(), uiState.value.activeRole, "Reviewed and addressed in demo workflow")
    }

    private fun perform(block: suspend () -> DomainResult<ClinicalAlert>) {
        viewModelScope.launch {
            when (val res = block()) {
                is DomainResult.Success -> _error.value = null
                is DomainResult.Failure -> _error.value = res.error.detail
            }
        }
    }

    fun dismissError() {
        _error.value = null
    }
}
