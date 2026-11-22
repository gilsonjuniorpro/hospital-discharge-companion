package com.hospitaldischargecompanion.feature.careplan

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.hospitaldischargecompanion.core.demo.DemoSessionRepository
import com.hospitaldischargecompanion.core.demo.DomainResult
import com.hospitaldischargecompanion.core.demo.UserRole
import com.hospitaldischargecompanion.core.discharge.CarePlanRepository
import com.hospitaldischargecompanion.core.discharge.CarePlanRevision
import com.hospitaldischargecompanion.core.discharge.CarePlanTask
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

data class CarePlanUiState(
    val episode: DischargeEpisode? = null,
    val activeRole: UserRole = UserRole.PATIENT,
    val tasks: List<CarePlanTask> = emptyList(),
    val revisions: List<CarePlanRevision> = emptyList(),
    val exportedSummary: String? = null,
    val errorMessage: String? = null,
    val isLoading: Boolean = true
)

@HiltViewModel
class CarePlanViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    sessionRepository: DemoSessionRepository,
    episodeRepository: DischargeEpisodeRepository,
    private val carePlanRepository: CarePlanRepository
) : ViewModel() {

    private val episodeId: String = savedStateHandle.get<String>("episodeId")
        ?: runCatching { savedStateHandle.toRoute<HdcRoute.CarePlan>().episodeId }.getOrDefault("ep-hf-001")
    private val _exportedSummary = MutableStateFlow<String?>(null)
    private val _errorMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<CarePlanUiState> = combine(
        episodeRepository.observeEpisode(episodeId),
        sessionRepository.sessionState,
        carePlanRepository.observeTasks(episodeId),
        carePlanRepository.observeRevisions(episodeId),
        combine(_exportedSummary, _errorMessage) { exp, err -> exp to err }
    ) { episode, session, tasks, revisions, (exported, error) ->
        CarePlanUiState(
            episode = episode,
            activeRole = session.activeRole ?: UserRole.PATIENT,
            tasks = tasks,
            revisions = revisions,
            exportedSummary = exported,
            errorMessage = error,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CarePlanUiState()
    )

    fun completeTask(taskId: String) {
        val state = uiState.value
        val actorLabel = when (state.activeRole) {
            UserRole.FAMILY -> "Maria Silva (Caregiver) on behalf of ${state.episode?.patientName ?: "Patient"}"
            UserRole.NURSE -> state.episode?.primaryNurse ?: "Primary Nurse"
            UserRole.PHYSICIAN -> state.episode?.attendingPhysician ?: "Attending Physician"
            UserRole.PATIENT -> state.episode?.patientName ?: "Patient"
        }
        viewModelScope.launch {
            when (val result = carePlanRepository.completeTask(taskId, actorLabel)) {
                is DomainResult.Success -> _errorMessage.value = null
                is DomainResult.Failure -> _errorMessage.value = result.error.detail
            }
        }
    }

    fun skipTask(taskId: String, reason: String) {
        val state = uiState.value
        val actorLabel = if (state.activeRole == UserRole.FAMILY) {
            "Maria Silva (Caregiver)"
        } else {
            state.episode?.patientName ?: "Patient"
        }
        viewModelScope.launch {
            when (val result = carePlanRepository.skipTask(taskId, reason, actorLabel)) {
                is DomainResult.Success -> _errorMessage.value = null
                is DomainResult.Failure -> _errorMessage.value = result.error.detail
            }
        }
    }

    fun reopenTask(taskId: String) {
        viewModelScope.launch {
            when (val result = carePlanRepository.reopenTask(taskId, uiState.value.activeRole)) {
                is DomainResult.Success -> _errorMessage.value = null
                is DomainResult.Failure -> _errorMessage.value = result.error.detail
            }
        }
    }

    fun exportSummary() {
        val s = uiState.value
        val episode = s.episode ?: return
        _exportedSummary.value = carePlanRepository.buildExportSummary(episode, s.tasks, s.revisions)
    }

    fun dismissExport() {
        _exportedSummary.value = null
    }

    fun dismissError() {
        _errorMessage.value = null
    }
}
