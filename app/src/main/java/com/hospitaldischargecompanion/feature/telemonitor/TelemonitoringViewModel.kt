package com.hospitaldischargecompanion.feature.telemonitor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.hospitaldischargecompanion.core.alerts.ClinicalAlert
import com.hospitaldischargecompanion.core.alerts.EvaluateAlertRulesUseCase
import com.hospitaldischargecompanion.core.demo.DemoSessionRepository
import com.hospitaldischargecompanion.core.demo.DomainResult
import com.hospitaldischargecompanion.core.demo.IdGenerator
import com.hospitaldischargecompanion.core.demo.TemperatureUnit
import com.hospitaldischargecompanion.core.demo.UserRole
import com.hospitaldischargecompanion.core.discharge.DischargeEpisode
import com.hospitaldischargecompanion.core.discharge.DischargeEpisodeRepository
import com.hospitaldischargecompanion.core.navigation.HdcRoute
import com.hospitaldischargecompanion.core.telemonitor.NewCheckInRequest
import com.hospitaldischargecompanion.core.telemonitor.TelemonitoringRepository
import com.hospitaldischargecompanion.core.telemonitor.VitalBaseline
import com.hospitaldischargecompanion.core.telemonitor.VitalCheckIn
import com.hospitaldischargecompanion.core.telemonitor.WearableDeviceSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CheckInFormDraft(
    val systolicInput: String = "134",
    val diastolicInput: String = "82",
    val heartRateInput: String = "78",
    val spo2Input: String = "96",
    val weightInput: String = "77.4",
    val temperatureCInput: String = "36.7",
    val painScore: Int = 1,
    val dyspneaSeverity: Int = 1,
    val swellingSeverity: Int = 1,
    val notes: String = ""
)

data class TelemonitoringUiState(
    val episode: DischargeEpisode? = null,
    val activeRole: UserRole = UserRole.PATIENT,
    val temperatureUnit: TemperatureUnit = TemperatureUnit.CELSIUS,
    val baseline: VitalBaseline? = null,
    val checkIns: List<VitalCheckIn> = emptyList(),
    val wearables: List<WearableDeviceSummary> = emptyList(),
    val draft: CheckInFormDraft = CheckInFormDraft(),
    val triggeredAlerts: List<ClinicalAlert> = emptyList(),
    val lastSuccessNotice: Boolean = false,
    val errorMessage: String? = null,
    val isLoading: Boolean = true
)

@HiltViewModel
class TelemonitoringViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    sessionRepository: DemoSessionRepository,
    episodeRepository: DischargeEpisodeRepository,
    private val telemonitoringRepository: TelemonitoringRepository,
    private val evaluateAlertRules: EvaluateAlertRulesUseCase,
    private val idGenerator: IdGenerator
) : ViewModel() {

    private val route = savedStateHandle.toRoute<HdcRoute.Telemonitoring>()
    private val _draft = MutableStateFlow(CheckInFormDraft())
    private val _feedback = MutableStateFlow(Triple<List<ClinicalAlert>, Boolean, String?>(emptyList(), false, null))

    val uiState: StateFlow<TelemonitoringUiState> = combine(
        episodeRepository.observeEpisode(route.episodeId),
        sessionRepository.sessionState,
        telemonitoringRepository.observeBaseline(route.episodeId),
        telemonitoringRepository.observeCheckIns(route.episodeId),
        combine(telemonitoringRepository.observeWearables(), _draft, _feedback) { w, d, f -> Triple(w, d, f) }
    ) { episode, session, baseline, checkIns, (wearables, draft, feedback) ->
        TelemonitoringUiState(
            episode = episode,
            activeRole = session.activeRole ?: UserRole.PATIENT,
            temperatureUnit = session.temperatureUnit,
            baseline = baseline,
            checkIns = checkIns,
            wearables = wearables,
            draft = draft,
            triggeredAlerts = feedback.first,
            lastSuccessNotice = feedback.second,
            errorMessage = feedback.third,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TelemonitoringUiState()
    )

    fun updateDraft(transform: (CheckInFormDraft) -> CheckInFormDraft) {
        _draft.update(transform)
    }

    fun fillOutOfRangeBpPreset() {
        _draft.value = CheckInFormDraft(
            systolicInput = "172",
            diastolicInput = "104",
            heartRateInput = "94",
            spo2Input = "93",
            weightInput = "78.8",
            temperatureCInput = "36.8",
            painScore = 2,
            dyspneaSeverity = 3,
            swellingSeverity = 3,
            notes = "Elevated morning BP and +2.3 kg above dry weight with mild shortness of breath."
        )
    }

    fun submitCheckIn() {
        val state = uiState.value
        val d = state.draft
        val sys = d.systolicInput.toIntOrNull()
        val dia = d.diastolicInput.toIntOrNull()
        val hr = d.heartRateInput.toIntOrNull()
        val spo2 = d.spo2Input.toIntOrNull()
        val wt = d.weightInput.toDoubleOrNull()
        val temp = d.temperatureCInput.toDoubleOrNull()

        if (sys == null || dia == null || hr == null || spo2 == null || wt == null || temp == null) {
            _feedback.value = Triple(emptyList(), false, "Enter valid numeric values for all vital signs.")
            return
        }

        val patientName = state.episode?.patientName ?: "Patient"
        val recorder = when (state.activeRole) {
            UserRole.FAMILY -> "Maria Silva (Caregiver) on behalf of $patientName"
            UserRole.NURSE -> state.episode?.primaryNurse ?: "Nurse"
            UserRole.PHYSICIAN -> state.episode?.attendingPhysician ?: "Physician"
            UserRole.PATIENT -> patientName
        }

        viewModelScope.launch {
            val result = telemonitoringRepository.submitCheckIn(
                NewCheckInRequest(
                    episodeId = route.episodeId,
                    systolicMmHg = sys,
                    diastolicMmHg = dia,
                    heartRateBpm = hr,
                    spo2Percent = spo2,
                    weightKg = wt,
                    temperatureC = temp,
                    painScore = d.painScore,
                    dyspneaSeverity = d.dyspneaSeverity,
                    swellingSeverity = d.swellingSeverity,
                    freeTextNotes = d.notes,
                    recordedByLabel = recorder,
                    idempotencyKey = idGenerator.nextId("chk-key")
                )
            )
            when (result) {
                is DomainResult.Success -> {
                    val alerts = evaluateAlertRules.evaluateCheckIn(result.data, state.baseline)
                    _feedback.value = Triple(alerts, true, null)
                }
                is DomainResult.Failure -> {
                    _feedback.value = Triple(emptyList(), false, result.error.detail)
                }
            }
        }
    }

    fun syncWearables() {
        viewModelScope.launch {
            telemonitoringRepository.syncWearableStub()
        }
    }

    fun dismissError() {
        _feedback.update { it.copy(third = null) }
    }
}
