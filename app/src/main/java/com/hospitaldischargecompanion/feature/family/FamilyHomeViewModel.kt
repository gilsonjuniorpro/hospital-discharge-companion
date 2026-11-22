package com.hospitaldischargecompanion.feature.family

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hospitaldischargecompanion.core.alerts.AlertRepository
import com.hospitaldischargecompanion.core.alerts.AlertSeverity
import com.hospitaldischargecompanion.core.alerts.AlertType
import com.hospitaldischargecompanion.core.alerts.ClinicalAlert
import com.hospitaldischargecompanion.core.alerts.EscalationStage
import com.hospitaldischargecompanion.core.carecircle.CareCircleMember
import com.hospitaldischargecompanion.core.carecircle.CareCircleRepository
import com.hospitaldischargecompanion.core.carecircle.CaregiverPermission
import com.hospitaldischargecompanion.core.demo.DemoSessionRepository
import com.hospitaldischargecompanion.core.demo.DomainResult
import com.hospitaldischargecompanion.core.demo.UserRole
import com.hospitaldischargecompanion.core.discharge.CarePlanRepository
import com.hospitaldischargecompanion.core.discharge.CarePlanTask
import com.hospitaldischargecompanion.core.discharge.CarePlanTaskStatus
import com.hospitaldischargecompanion.core.discharge.DischargeEpisode
import com.hospitaldischargecompanion.core.discharge.DischargeEpisodeRepository
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

data class FamilyHomeUiState(
    val episode: DischargeEpisode? = null,
    val todayTasks: List<CarePlanTask> = emptyList(),
    val openAlerts: List<ClinicalAlert> = emptyList(),
    val careCircle: List<CareCircleMember> = emptyList(),
    val feedbackBanner: String? = null,
    val errorMessage: String? = null,
    val isLoading: Boolean = true
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class FamilyHomeViewModel @Inject constructor(
    sessionRepository: DemoSessionRepository,
    episodeRepository: DischargeEpisodeRepository,
    private val carePlanRepository: CarePlanRepository,
    private val alertRepository: AlertRepository,
    private val careCircleRepository: CareCircleRepository
) : ViewModel() {

    private val _feedback = MutableStateFlow<Pair<String?, String?>>(null to null)

    val uiState: StateFlow<FamilyHomeUiState> = sessionRepository.sessionState
        .flatMapLatest { session ->
            val episodeId = session.selectedEpisodeId
            combine(
                episodeRepository.observeEpisode(episodeId),
                carePlanRepository.observeTasks(episodeId),
                alertRepository.observeEpisodeAlerts(episodeId),
                careCircleRepository.observeCareCircle(episodeId),
                _feedback
            ) { episode, tasks, alerts, circle, (banner, error) ->
                FamilyHomeUiState(
                    episode = episode,
                    todayTasks = tasks.filter { it.status != CarePlanTaskStatus.COMPLETED },
                    openAlerts = alerts.filter { it.isActive },
                    careCircle = circle,
                    feedbackBanner = banner,
                    errorMessage = error,
                    isLoading = false
                )
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = FamilyHomeUiState()
        )

    fun completeTaskOnBehalf(taskId: String) {
        val patient = uiState.value.episode?.patientName ?: "Patient"
        viewModelScope.launch {
            when (val res = carePlanRepository.completeTask(taskId, "Maria Silva (Spouse) on behalf of $patient")) {
                is DomainResult.Success -> _feedback.value = "Task confirmed with dual attribution for $patient." to null
                is DomainResult.Failure -> _feedback.value = null to res.error.detail
            }
        }
    }

    fun escalateOnBehalfOfPatient(note: String) {
        val ep = uiState.value.episode ?: return
        viewModelScope.launch {
            val activeAlert = uiState.value.openAlerts.firstOrNull()
            val result = if (activeAlert != null) {
                alertRepository.escalate(
                    alertId = activeAlert.id,
                    actorName = "Maria Silva (Family Caregiver)",
                    actorRole = UserRole.FAMILY,
                    reasonCode = "FAMILY_ESCALATION"
                )
            } else {
                alertRepository.raiseAlert(
                    episodeId = ep.id,
                    type = AlertType.RED_FLAG_SYMPTOM,
                    severity = AlertSeverity.URGENT,
                    escalationStage = EscalationStage.NURSE_TRIAGE,
                    title = "Family caregiver escalated concern for ${ep.patientName}",
                    summary = note.ifBlank { "Family caregiver requested urgent nurse callback." },
                    patientGuidance = "Nurse triage has received the family escalation and will respond shortly.",
                    actorName = "Maria Silva (Family Caregiver)",
                    actorRole = UserRole.FAMILY,
                    reasonCode = "FAMILY_MANUAL_ESCALATION"
                )
            }
            when (result) {
                is DomainResult.Success -> _feedback.value = "Escalated to Nurse Triage queue on behalf of ${ep.patientName}." to null
                is DomainResult.Failure -> _feedback.value = null to result.error.detail
            }
        }
    }

    fun acceptInviteCode(code: String, name: String) {
        viewModelScope.launch {
            when (val res = careCircleRepository.acceptByCode(code, name, "Family")) {
                is DomainResult.Success -> _feedback.value = "${res.data.name} joined the Care Circle." to null
                is DomainResult.Failure -> _feedback.value = null to res.error.detail
            }
        }
    }

    fun cyclePermission(member: CareCircleMember) {
        val next = when (member.permission) {
            CaregiverPermission.VIEW_ONLY -> CaregiverPermission.ASSIST_MEDS_AND_CHECKINS
            CaregiverPermission.ASSIST_MEDS_AND_CHECKINS -> CaregiverPermission.ESCALATE
            CaregiverPermission.ESCALATE -> CaregiverPermission.VIEW_ONLY
        }
        viewModelScope.launch {
            careCircleRepository.updatePermission(member.id, next)
        }
    }

    fun revokeMember(memberId: String) {
        viewModelScope.launch {
            careCircleRepository.revoke(memberId)
        }
    }

    fun dismissError() {
        _feedback.value = _feedback.value.first to null
    }
}
