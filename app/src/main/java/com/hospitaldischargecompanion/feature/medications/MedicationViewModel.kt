package com.hospitaldischargecompanion.feature.medications

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.hospitaldischargecompanion.core.alerts.EvaluateAlertRulesUseCase
import com.hospitaldischargecompanion.core.demo.DemoSessionRepository
import com.hospitaldischargecompanion.core.demo.DomainResult
import com.hospitaldischargecompanion.core.demo.IdGenerator
import com.hospitaldischargecompanion.core.demo.UserRole
import com.hospitaldischargecompanion.core.discharge.DischargeEpisode
import com.hospitaldischargecompanion.core.discharge.DischargeEpisodeRepository
import com.hospitaldischargecompanion.core.medication.AdherenceSummary
import com.hospitaldischargecompanion.core.medication.DoseActionType
import com.hospitaldischargecompanion.core.medication.DoseLogEntry
import com.hospitaldischargecompanion.core.medication.MedicationInteractionWarning
import com.hospitaldischargecompanion.core.medication.MedicationItem
import com.hospitaldischargecompanion.core.medication.MedicationRepository
import com.hospitaldischargecompanion.core.navigation.HdcRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MedicationUiState(
    val episode: DischargeEpisode? = null,
    val activeRole: UserRole = UserRole.PATIENT,
    val highlightMedicationId: String? = null,
    val medications: List<MedicationItem> = emptyList(),
    val warnings: List<MedicationInteractionWarning> = emptyList(),
    val adherence: AdherenceSummary = AdherenceSummary(0, 0, 0, 0, 100),
    val doseLogs: List<DoseLogEntry> = emptyList(),
    val errorMessage: String? = null,
    val isLoading: Boolean = true
)

@HiltViewModel
class MedicationViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    sessionRepository: DemoSessionRepository,
    episodeRepository: DischargeEpisodeRepository,
    private val medicationRepository: MedicationRepository,
    private val evaluateAlertRules: EvaluateAlertRulesUseCase,
    private val idGenerator: IdGenerator
) : ViewModel() {

    private val episodeId: String = savedStateHandle.get<String>("episodeId")
        ?: runCatching { savedStateHandle.toRoute<HdcRoute.Medications>().episodeId }.getOrDefault("ep-hf-001")
    private val highlightMedicationId: String? = savedStateHandle.get<String>("highlightMedicationId")
        ?: runCatching { savedStateHandle.toRoute<HdcRoute.Medications>().highlightMedicationId }.getOrNull()
    private val _errorMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<MedicationUiState> = combine(
        episodeRepository.observeEpisode(episodeId),
        sessionRepository.sessionState,
        medicationRepository.observeMedications(episodeId),
        medicationRepository.observeWarnings(episodeId),
        combine(
            medicationRepository.observeAdherence(episodeId),
            medicationRepository.observeDoseLogs(episodeId),
            _errorMessage
        ) { adh, logs, err -> Triple(adh, logs, err) }
    ) { episode, session, meds, warnings, (adherence, logs, error) ->
        MedicationUiState(
            episode = episode,
            activeRole = session.activeRole ?: UserRole.PATIENT,
            highlightMedicationId = highlightMedicationId,
            medications = meds,
            warnings = warnings,
            adherence = adherence,
            doseLogs = logs,
            errorMessage = error,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MedicationUiState()
    )

    fun recordDose(med: MedicationItem, action: DoseActionType, reason: String? = null) {
        val state = uiState.value
        val patientName = state.episode?.patientName ?: "Patient"
        val attribution = when (state.activeRole) {
            UserRole.FAMILY -> "Maria Silva (Caregiver) on behalf of $patientName"
            UserRole.NURSE -> "${state.episode?.primaryNurse ?: "Nurse"} (Verified)"
            UserRole.PHYSICIAN -> "${state.episode?.attendingPhysician ?: "Physician"} (Verified)"
            UserRole.PATIENT -> patientName
        }
        viewModelScope.launch {
            val result = medicationRepository.recordDose(
                medicationId = med.id,
                action = action,
                attributionLabel = attribution,
                reasonOrNote = reason,
                idempotencyKey = idGenerator.nextId("dose-key")
            )
            when (result) {
                is DomainResult.Success -> {
                    _errorMessage.value = null
                    if (action == DoseActionType.SKIPPED) {
                        evaluateAlertRules.evaluateMissedMedication(
                            episodeId = med.episodeId,
                            medicationName = "${med.name} ${med.strengthAndForm}",
                            attributionLabel = attribution,
                            reason = reason
                        )
                    }
                }
                is DomainResult.Failure -> _errorMessage.value = result.error.detail
            }
        }
    }

    fun attachMockPillBox(medicationId: String) {
        viewModelScope.launch {
            when (val res = medicationRepository.attachMockPillBoxPhoto(medicationId, "Local demo photo: weekly pill organizer verified")) {
                is DomainResult.Success -> _errorMessage.value = null
                is DomainResult.Failure -> _errorMessage.value = res.error.detail
            }
        }
    }

    fun requestRefill(medicationId: String) {
        viewModelScope.launch {
            when (val res = medicationRepository.requestRefill(medicationId)) {
                is DomainResult.Success -> _errorMessage.value = null
                is DomainResult.Failure -> _errorMessage.value = res.error.detail
            }
        }
    }

    fun dismissError() {
        _errorMessage.value = null
    }
}
