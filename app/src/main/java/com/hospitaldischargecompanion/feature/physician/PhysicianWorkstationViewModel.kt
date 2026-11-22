package com.hospitaldischargecompanion.feature.physician

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hospitaldischargecompanion.core.alerts.AlertRepository
import com.hospitaldischargecompanion.core.alerts.ClinicalAlert
import com.hospitaldischargecompanion.core.demo.DemoSessionRepository
import com.hospitaldischargecompanion.core.demo.DomainResult
import com.hospitaldischargecompanion.core.demo.UserRole
import com.hospitaldischargecompanion.core.discharge.CarePlanRepository
import com.hospitaldischargecompanion.core.discharge.CarePlanRevision
import com.hospitaldischargecompanion.core.discharge.CarePlanTaskType
import com.hospitaldischargecompanion.core.discharge.DischargeEpisode
import com.hospitaldischargecompanion.core.discharge.DischargeEpisodeRepository
import com.hospitaldischargecompanion.core.medication.MedicationItem
import com.hospitaldischargecompanion.core.medication.MedicationRepository
import com.hospitaldischargecompanion.core.telemonitor.TelemonitoringRepository
import com.hospitaldischargecompanion.core.telemonitor.VitalCheckIn
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ClinicalDisposition(val summaryText: String) {
    CONTINUE_HOME("Continue home telemonitoring with updated evening weight check."),
    EARLIER_FOLLOW_UP("Schedule earlier cardiology/clinic follow-up within 48 hours."),
    ADVISE_ER_COPY_ONLY("Advise ER evaluation / call local emergency services (educational demo copy only).")
}

data class PhysicianWorkstationUiState(
    val escalatedQueue: List<ClinicalAlert> = emptyList(),
    val episodes: List<DischargeEpisode> = emptyList(),
    val selectedEpisode: DischargeEpisode? = null,
    val recentVitals: List<VitalCheckIn> = emptyList(),
    val medications: List<MedicationItem> = emptyList(),
    val revisions: List<CarePlanRevision> = emptyList(),
    val selectedDisposition: ClinicalDisposition = ClinicalDisposition.CONTINUE_HOME,
    val feedbackBanner: String? = null,
    val errorMessage: String? = null,
    val isLoading: Boolean = true
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PhysicianWorkstationViewModel @Inject constructor(
    private val sessionRepository: DemoSessionRepository,
    episodeRepository: DischargeEpisodeRepository,
    private val carePlanRepository: CarePlanRepository,
    medicationRepository: MedicationRepository,
    telemonitoringRepository: TelemonitoringRepository,
    private val alertRepository: AlertRepository
) : ViewModel() {

    private val _disposition = MutableStateFlow(ClinicalDisposition.CONTINUE_HOME)
    private val _feedback = MutableStateFlow<Pair<String?, String?>>(null to null)

    val uiState: StateFlow<PhysicianWorkstationUiState> = sessionRepository.sessionState
        .flatMapLatest { session ->
            val episodeId = session.selectedEpisodeId
            combine(
                alertRepository.observePhysicianEscalatedQueue(),
                episodeRepository.observeAllEpisodes(),
                telemonitoringRepository.observeCheckIns(episodeId),
                medicationRepository.observeMedications(episodeId),
                combine(
                    carePlanRepository.observeRevisions(episodeId),
                    _disposition,
                    _feedback
                ) { rev, disp, fb -> Triple(rev, disp, fb) }
            ) { queue, episodes, vitals, meds, (revisions, disposition, feedback) ->
                val activeEp = episodes.firstOrNull { it.id == episodeId } ?: episodes.firstOrNull()
                PhysicianWorkstationUiState(
                    escalatedQueue = queue,
                    episodes = episodes,
                    selectedEpisode = activeEp,
                    recentVitals = vitals,
                    medications = meds,
                    revisions = revisions,
                    selectedDisposition = disposition,
                    feedbackBanner = feedback.first,
                    errorMessage = feedback.second,
                    isLoading = false
                )
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = PhysicianWorkstationUiState()
        )

    fun selectEpisode(episodeId: String) {
        viewModelScope.launch {
            sessionRepository.setSelectedEpisode(episodeId)
        }
    }

    fun selectDisposition(disposition: ClinicalDisposition) {
        _disposition.value = disposition
    }

    fun amendAndSignOffCarePlan(
        changeSummary: String,
        newTaskTitle: String,
        newRedFlag: String
    ) {
        val state = uiState.value
        val ep = state.selectedEpisode ?: return
        viewModelScope.launch {
            val res = carePlanRepository.amendCarePlan(
                episodeId = ep.id,
                actorRole = UserRole.PHYSICIAN,
                authorName = ep.attendingPhysician,
                changeSummary = changeSummary.ifBlank { state.selectedDisposition.summaryText },
                newTaskTitle = newTaskTitle.ifBlank { "Evening dry-weight & seated BP recheck" },
                newTaskInstructions = "Physician amendment (${state.selectedDisposition.name}): confirm evening reading.",
                newTaskType = CarePlanTaskType.VITAL_CHECK,
                newRedFlag = newRedFlag.takeIf { it.isNotBlank() },
                dispositionNote = state.selectedDisposition.summaryText
            )
            when (res) {
                is DomainResult.Success -> {
                    _feedback.value = "Signed off Care Plan v${res.data.revisionNumber} for ${ep.patientName}." to null
                }
                is DomainResult.Failure -> {
                    _feedback.value = null to res.error.detail
                }
            }
        }
    }

    fun resolveEscalatedAlert(alertId: String) {
        val doctor = uiState.value.selectedEpisode?.attendingPhysician ?: "Dr. Elena Vance, MD"
        viewModelScope.launch {
            val res = alertRepository.resolve(
                alertId = alertId,
                actorName = doctor,
                actorRole = UserRole.PHYSICIAN,
                resolutionNote = uiState.value.selectedDisposition.summaryText
            )
            when (res) {
                is DomainResult.Success -> _feedback.value = "Alert resolved with physician disposition." to null
                is DomainResult.Failure -> _feedback.value = null to res.error.detail
            }
        }
    }

    fun dismissError() {
        _feedback.value = _feedback.value.first to null
    }
}
