package com.hospitaldischargecompanion.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hospitaldischargecompanion.core.demo.AppLocale
import com.hospitaldischargecompanion.core.demo.DemoSessionRepository
import com.hospitaldischargecompanion.core.demo.UserRole
import com.hospitaldischargecompanion.core.discharge.DischargeEpisode
import com.hospitaldischargecompanion.core.discharge.DischargeEpisodeRepository
import com.hospitaldischargecompanion.core.i18n.LocalizedClinicalPack
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileSelectionUiState(
    val lastRole: UserRole? = null,
    val selectedEpisodeId: String = "ep-hf-001",
    val selectedLocale: AppLocale = AppLocale.EN,
    val episodes: List<DischargeEpisode> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class ProfileSelectionViewModel @Inject constructor(
    private val sessionRepository: DemoSessionRepository,
    episodeRepository: DischargeEpisodeRepository
) : ViewModel() {

    val uiState: StateFlow<ProfileSelectionUiState> = combine(
        sessionRepository.sessionState,
        episodeRepository.observeAllEpisodes()
    ) { session, episodes ->
        ProfileSelectionUiState(
            lastRole = session.activeRole,
            selectedEpisodeId = session.selectedEpisodeId,
            selectedLocale = session.locale,
            episodes = episodes.map { LocalizedClinicalPack.applyTo(it, session.locale) },
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ProfileSelectionUiState()
    )

    fun selectRole(role: UserRole, onNavigated: (UserRole, String) -> Unit) {
        val episodeId = uiState.value.selectedEpisodeId
        viewModelScope.launch {
            sessionRepository.setActiveRole(role)
            onNavigated(role, episodeId)
        }
    }

    fun selectEpisode(episodeId: String) {
        viewModelScope.launch {
            sessionRepository.setSelectedEpisode(episodeId)
        }
    }

    fun selectLocale(locale: AppLocale) {
        viewModelScope.launch {
            sessionRepository.setLocale(locale)
        }
    }
}
