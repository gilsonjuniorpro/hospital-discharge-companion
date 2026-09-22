package com.hospitaldischargecompanion.feature.appointments

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.hospitaldischargecompanion.core.appointments.AppointmentItem
import com.hospitaldischargecompanion.core.appointments.AppointmentRepository
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

data class AppointmentsUiState(
    val episode: DischargeEpisode? = null,
    val activeRole: UserRole = UserRole.PATIENT,
    val appointments: List<AppointmentItem> = emptyList(),
    val feedbackBanner: String? = null,
    val errorMessage: String? = null,
    val isLoading: Boolean = true
)

@HiltViewModel
class AppointmentsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    sessionRepository: DemoSessionRepository,
    episodeRepository: DischargeEpisodeRepository,
    private val appointmentRepository: AppointmentRepository
) : ViewModel() {

    private val episodeId: String = savedStateHandle.get<String>("episodeId")
        ?: runCatching { savedStateHandle.toRoute<HdcRoute.Appointments>().episodeId }.getOrDefault("ep-hf-001")
    private val _feedback = MutableStateFlow<Pair<String?, String?>>(null to null)

    val uiState: StateFlow<AppointmentsUiState> = combine(
        episodeRepository.observeEpisode(episodeId),
        sessionRepository.sessionState,
        appointmentRepository.observeAppointments(episodeId),
        _feedback
    ) { episode, session, appointments, (banner, error) ->
        AppointmentsUiState(
            episode = episode,
            activeRole = session.activeRole ?: UserRole.PATIENT,
            appointments = appointments,
            feedbackBanner = banner,
            errorMessage = error,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AppointmentsUiState()
    )

    fun markAttended(appointmentId: String) {
        viewModelScope.launch {
            when (val res = appointmentRepository.markAttended(appointmentId)) {
                is DomainResult.Success -> _feedback.value = "Marked attended; post-visit task added to care plan." to null
                is DomainResult.Failure -> _feedback.value = null to res.error.detail
            }
        }
    }

    fun markMissed(appointmentId: String) {
        val actor = uiState.value.episode?.patientName ?: "Patient"
        viewModelScope.launch {
            when (val res = appointmentRepository.markMissed(appointmentId, actor)) {
                is DomainResult.Success -> _feedback.value = "Marked missed; nurse triage alert created." to null
                is DomainResult.Failure -> _feedback.value = null to res.error.detail
            }
        }
    }

    fun requestReschedule(appointmentId: String, preferredSlot: String) {
        viewModelScope.launch {
            when (val res = appointmentRepository.requestReschedule(appointmentId, preferredSlot)) {
                is DomainResult.Success -> _feedback.value = "Reschedule request sent for nurse approval." to null
                is DomainResult.Failure -> _feedback.value = null to res.error.detail
            }
        }
    }

    fun approveReschedule(appointmentId: String) {
        viewModelScope.launch {
            when (val res = appointmentRepository.approveReschedule(appointmentId, uiState.value.activeRole)) {
                is DomainResult.Success -> _feedback.value = "Reschedule approved and updated." to null
                is DomainResult.Failure -> _feedback.value = null to res.error.detail
            }
        }
    }

    fun dismissError() {
        _feedback.value = _feedback.value.first to null
    }
}
