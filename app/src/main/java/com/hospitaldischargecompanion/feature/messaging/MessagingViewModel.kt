package com.hospitaldischargecompanion.feature.messaging

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.hospitaldischargecompanion.core.alerts.AlertRepository
import com.hospitaldischargecompanion.core.alerts.AlertSeverity
import com.hospitaldischargecompanion.core.demo.DemoSessionRepository
import com.hospitaldischargecompanion.core.demo.DomainResult
import com.hospitaldischargecompanion.core.demo.UserRole
import com.hospitaldischargecompanion.core.discharge.DischargeEpisode
import com.hospitaldischargecompanion.core.discharge.DischargeEpisodeRepository
import com.hospitaldischargecompanion.core.messaging.EpisodeMessage
import com.hospitaldischargecompanion.core.messaging.MessagingRepository
import com.hospitaldischargecompanion.core.messaging.QuickReplyTemplate
import com.hospitaldischargecompanion.core.navigation.HdcRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MessagingUiState(
    val episode: DischargeEpisode? = null,
    val activeRole: UserRole = UserRole.PATIENT,
    val clinicalNotesOnly: Boolean = false,
    val searchQuery: String = "",
    val messages: List<EpisodeMessage> = emptyList(),
    val templates: List<QuickReplyTemplate> = emptyList(),
    val hasCriticalAlert: Boolean = false,
    val errorMessage: String? = null,
    val isLoading: Boolean = true
)

@HiltViewModel
class MessagingViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    sessionRepository: DemoSessionRepository,
    episodeRepository: DischargeEpisodeRepository,
    alertRepository: AlertRepository,
    private val messagingRepository: MessagingRepository
) : ViewModel() {

    private val episodeId: String = savedStateHandle.get<String>("episodeId")
        ?: runCatching { savedStateHandle.toRoute<HdcRoute.Messaging>().episodeId }.getOrDefault("ep-hf-001")
    private val _clinicalNotesOnly = MutableStateFlow(false)
    private val _searchQuery = MutableStateFlow("")
    private val _error = MutableStateFlow<String?>(null)

    val uiState: StateFlow<MessagingUiState> = combine(
        episodeRepository.observeEpisode(episodeId),
        sessionRepository.sessionState,
        messagingRepository.observeThread(episodeId),
        alertRepository.observeEpisodeAlerts(episodeId),
        combine(_clinicalNotesOnly, _searchQuery, _error) { c, q, e -> Triple(c, q, e) }
    ) { episode, session, messages, alerts, (notesOnly, query, error) ->
        val role = session.activeRole ?: UserRole.PATIENT
        val filtered = messages.filter { msg ->
            (!notesOnly || msg.isClinicalNote) &&
                (query.isBlank() || msg.body.contains(query, ignoreCase = true) || msg.senderName.contains(query, ignoreCase = true))
        }
        MessagingUiState(
            episode = episode,
            activeRole = role,
            clinicalNotesOnly = notesOnly,
            searchQuery = query,
            messages = filtered,
            templates = messagingRepository.getQuickReplyTemplates(role),
            hasCriticalAlert = alerts.any { it.isActive && (it.severity == AlertSeverity.CRITICAL || it.severity == AlertSeverity.URGENT) },
            errorMessage = error,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MessagingUiState()
    )

    fun toggleClinicalNotesOnly() {
        _clinicalNotesOnly.value = !_clinicalNotesOnly.value
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun sendMessage(body: String, asClinicalNote: Boolean) {
        val state = uiState.value
        val sender = when (state.activeRole) {
            UserRole.PATIENT -> state.episode?.patientName ?: "Patient"
            UserRole.FAMILY -> "Maria Silva (Family)"
            UserRole.NURSE -> state.episode?.primaryNurse ?: "RN Clara Mendes"
            UserRole.PHYSICIAN -> state.episode?.attendingPhysician ?: "Dr. Elena Vance, MD"
        }
        viewModelScope.launch {
            when (
                val res = messagingRepository.sendMessage(
                    episodeId = episodeId,
                    senderName = sender,
                    senderRole = state.activeRole,
                    body = body,
                    isClinicalNote = asClinicalNote && state.activeRole.canWriteClinicalNotes
                )
            ) {
                is DomainResult.Success -> _error.value = null
                is DomainResult.Failure -> _error.value = res.error.detail
            }
        }
    }

    fun dismissError() {
        _error.value = null
    }
}
