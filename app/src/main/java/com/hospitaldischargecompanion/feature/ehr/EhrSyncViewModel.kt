package com.hospitaldischargecompanion.feature.ehr

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.hospitaldischargecompanion.core.demo.DemoNetworkConfig
import com.hospitaldischargecompanion.core.demo.DemoNetworkSimulator
import com.hospitaldischargecompanion.core.demo.DomainResult
import com.hospitaldischargecompanion.core.demo.SimulatedFailureMode
import com.hospitaldischargecompanion.core.discharge.DischargeEpisode
import com.hospitaldischargecompanion.core.discharge.DischargeEpisodeRepository
import com.hospitaldischargecompanion.core.ehr.ConflictResolutionChoice
import com.hospitaldischargecompanion.core.ehr.EhrConflictRecord
import com.hospitaldischargecompanion.core.ehr.EhrSyncLogEntry
import com.hospitaldischargecompanion.core.ehr.EhrSyncRepository
import com.hospitaldischargecompanion.core.ehr.FhirBundleSerializer
import com.hospitaldischargecompanion.core.medication.MedicationRepository
import com.hospitaldischargecompanion.core.navigation.HdcRoute
import com.hospitaldischargecompanion.core.telemonitor.TelemonitoringRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EhrSyncUiState(
    val episode: DischargeEpisode? = null,
    val networkConfig: DemoNetworkConfig = DemoNetworkConfig(),
    val logs: List<EhrSyncLogEntry> = emptyList(),
    val openConflicts: List<EhrConflictRecord> = emptyList(),
    val fhirBundlePreviewJson: String? = null,
    val isSyncing: Boolean = false,
    val feedbackBanner: String? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class EhrSyncViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    episodeRepository: DischargeEpisodeRepository,
    private val ehrSyncRepository: EhrSyncRepository,
    private val medicationRepository: MedicationRepository,
    private val telemonitoringRepository: TelemonitoringRepository,
    private val networkSimulator: DemoNetworkSimulator
) : ViewModel() {

    private val episodeId: String = savedStateHandle.get<String>("episodeId")
        ?: runCatching { savedStateHandle.toRoute<HdcRoute.EhrSync>().episodeId }.getOrDefault("ep-hf-001")
    private val _busy = MutableStateFlow(false)
    private val _feedback = MutableStateFlow<Pair<String?, String?>>(null to null)
    private val _bundlePreview = MutableStateFlow<String?>(null)

    val uiState: StateFlow<EhrSyncUiState> = combine(
        episodeRepository.observeEpisode(episodeId),
        networkSimulator.config,
        ehrSyncRepository.observeLogs(episodeId),
        ehrSyncRepository.observeOpenConflicts(episodeId),
        combine(_busy, _feedback, _bundlePreview) { b, f, p -> Triple(b, f, p) }
    ) { episode, config, logs, conflicts, (busy, feedback, bundlePreview) ->
        EhrSyncUiState(
            episode = episode,
            networkConfig = config,
            logs = logs,
            openConflicts = conflicts,
            fhirBundlePreviewJson = bundlePreview,
            isSyncing = busy,
            feedbackBanner = feedback.first,
            errorMessage = feedback.second
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = EhrSyncUiState()
    )

    fun inspectFhirBundle() {
        val ep = uiState.value.episode ?: return
        viewModelScope.launch {
            val meds = medicationRepository.observeMedications(episodeId).first()
            val latest = telemonitoringRepository.observeCheckIns(episodeId).first().lastOrNull()
            _bundlePreview.value = FhirBundleSerializer.toPrettyJson(ep, meds, latest)
        }
    }

    fun dismissFhirBundle() {
        _bundlePreview.value = null
    }

    fun setFailureMode(mode: SimulatedFailureMode) {
        networkSimulator.setFailureMode(mode)
    }

    fun pullDischargeSummary() {
        runSync { ehrSyncRepository.pullDischargeSummary(episodeId) }
    }

    fun pushObservations() {
        runSync { ehrSyncRepository.pushObservationsAndAdherence(episodeId) }
    }

    fun resolveConflict(conflictId: String, choice: ConflictResolutionChoice) {
        viewModelScope.launch {
            when (val res = ehrSyncRepository.resolveConflict(conflictId, choice)) {
                is DomainResult.Success -> _feedback.value = "Conflict reconciled (${choice.name}) and synced." to null
                is DomainResult.Failure -> _feedback.value = null to res.error.detail
            }
        }
    }

    private fun runSync(action: suspend () -> DomainResult<EhrSyncLogEntry>) {
        viewModelScope.launch {
            _busy.value = true
            when (val res = action()) {
                is DomainResult.Success -> _feedback.value = res.data.detail to null
                is DomainResult.Failure -> _feedback.value = null to res.error.detail
            }
            _busy.value = false
        }
    }

    fun dismissError() {
        _feedback.value = _feedback.value.first to null
    }
}
