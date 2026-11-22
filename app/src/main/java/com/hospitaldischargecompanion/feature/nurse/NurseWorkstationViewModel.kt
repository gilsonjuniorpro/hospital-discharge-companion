package com.hospitaldischargecompanion.feature.nurse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hospitaldischargecompanion.core.alerts.AlertRepository
import com.hospitaldischargecompanion.core.alerts.ClinicalAlert
import com.hospitaldischargecompanion.core.demo.UserRole
import com.hospitaldischargecompanion.core.discharge.CarePlanRepository
import com.hospitaldischargecompanion.core.discharge.CarePlanTaskStatus
import com.hospitaldischargecompanion.core.discharge.DischargeEpisode
import com.hospitaldischargecompanion.core.discharge.DischargeEpisodeRepository
import com.hospitaldischargecompanion.core.discharge.RiskTier
import com.hospitaldischargecompanion.core.medication.DoseActionType
import com.hospitaldischargecompanion.core.medication.MedicationChangeStatus
import com.hospitaldischargecompanion.core.medication.MedicationRepository
import com.hospitaldischargecompanion.core.messaging.MessagingRepository
import com.hospitaldischargecompanion.core.notifications.NotificationCategory
import com.hospitaldischargecompanion.core.notifications.NotificationCenterRepository
import com.hospitaldischargecompanion.core.telemonitor.TelemonitoringRepository
import com.hospitaldischargecompanion.core.telemonitor.VitalCheckIn
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class NurseCensusFilter {
    ALL,
    HIGH_RISK,
    OVERDUE_TASKS,
    OPEN_ALERTS
}

data class NurseEpisodeSummary(
    val episode: DischargeEpisode,
    val openAlertsCount: Int,
    val overdueTasksCount: Int,
    val adherencePercent: Int,
    val latestCheckIn: VitalCheckIn?
)

data class NurseWorkstationUiState(
    val filter: NurseCensusFilter = NurseCensusFilter.ALL,
    val census: List<NurseEpisodeSummary> = emptyList(),
    val triageQueue: List<ClinicalAlert> = emptyList(),
    val shiftHandoffSummary: String = "",
    val feedbackBanner: String? = null,
    val isLoading: Boolean = true
)

@HiltViewModel
class NurseWorkstationViewModel @Inject constructor(
    episodeRepository: DischargeEpisodeRepository,
    carePlanRepository: CarePlanRepository,
    medicationRepository: MedicationRepository,
    telemonitoringRepository: TelemonitoringRepository,
    private val alertRepository: AlertRepository,
    private val messagingRepository: MessagingRepository,
    private val notificationRepository: NotificationCenterRepository
) : ViewModel() {

    private val _filter = MutableStateFlow(NurseCensusFilter.ALL)
    private val _banner = MutableStateFlow<String?>(null)

    val uiState: StateFlow<NurseWorkstationUiState> = combine(
        episodeRepository.observeAllEpisodes(),
        carePlanRepository.observeAllTasks(),
        medicationRepository.observeAllMedications(),
        telemonitoringRepository.observeAllCheckIns(),
        combine(alertRepository.observeNurseTriageQueue(), _filter, _banner) { q, f, b -> Triple(q, f, b) }
    ) { episodes, tasks, meds, checkIns, (queue, filter, banner) ->
        val summaries = episodes.map { ep ->
            val epTasks = tasks.filter { it.episodeId == ep.id }
            val epMeds = meds.filter { it.episodeId == ep.id && it.changeStatus != MedicationChangeStatus.STOPPED }
            val taken = epMeds.count { it.todayDoseStatus == DoseActionType.TAKEN }
            val adh = if (epMeds.isEmpty()) 100 else (taken * 100) / epMeds.size
            NurseEpisodeSummary(
                episode = ep,
                openAlertsCount = queue.count { it.episodeId == ep.id },
                overdueTasksCount = epTasks.count { it.status == CarePlanTaskStatus.OVERDUE },
                adherencePercent = adh,
                latestCheckIn = checkIns.filter { it.episodeId == ep.id }.maxByOrNull { it.dayNumber }
            )
        }
        val filtered = when (filter) {
            NurseCensusFilter.ALL -> summaries
            NurseCensusFilter.HIGH_RISK -> summaries.filter { it.episode.riskTier == RiskTier.HIGH }
            NurseCensusFilter.OVERDUE_TASKS -> summaries.filter { it.overdueTasksCount > 0 }
            NurseCensusFilter.OPEN_ALERTS -> summaries.filter { it.openAlertsCount > 0 }
        }
        val handoff = "Active census: ${episodes.size} episodes · Open alerts: ${queue.size} · High-risk watch: ${
            episodes.filter { it.riskTier == RiskTier.HIGH }.joinToString { it.patientName }
        }"
        NurseWorkstationUiState(
            filter = filter,
            census = filtered,
            triageQueue = queue,
            shiftHandoffSummary = handoff,
            feedbackBanner = banner,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = NurseWorkstationUiState()
    )

    fun setFilter(filter: NurseCensusFilter) {
        _filter.value = filter
    }

    fun acknowledgeAlert(alertId: String) {
        viewModelScope.launch {
            alertRepository.acknowledge(alertId, "RN Clara Mendes", UserRole.NURSE)
            _banner.value = "Alert acknowledged in nurse triage queue."
        }
    }

    fun escalateToPhysician(alertId: String) {
        viewModelScope.launch {
            alertRepository.escalate(alertId, "RN Clara Mendes", UserRole.NURSE, "NURSE_ESCALATION_TO_MD")
            _banner.value = "Escalated alert to Physician review queue."
        }
    }

    fun resolveAlert(alertId: String) {
        viewModelScope.launch {
            alertRepository.resolve(alertId, "RN Clara Mendes", UserRole.NURSE, "Triaged and resolved with patient callback")
            _banner.value = "Alert resolved and recorded in audit trail."
        }
    }

    fun requestRecheckAndDocumentNote(episodeId: String, patientName: String) {
        viewModelScope.launch {
            messagingRepository.sendMessage(
                episodeId = episodeId,
                senderName = "RN Clara Mendes",
                senderRole = UserRole.NURSE,
                body = "Clinical Note: Requested repeat seated BP & weight check for $patientName; reinforced low-sodium and red-flag instructions.",
                isClinicalNote = true
            )
            notificationRepository.pushLocalReminder(
                episodeId = episodeId,
                category = NotificationCategory.CHECKIN_DUE,
                title = "Nurse requested vitals recheck",
                message = "Please repeat BP and dry weight for $patientName.",
                deepLinkUri = "hdc://episode/$episodeId"
            )
            _banner.value = "Sent recheck reminder and saved nursing clinical note for $patientName."
        }
    }

    fun bulkSendReminders() {
        val visible = uiState.value.census
        viewModelScope.launch {
            visible.forEach { summary ->
                notificationRepository.pushLocalReminder(
                    episodeId = summary.episode.id,
                    category = NotificationCategory.MEDICATION_DUE,
                    title = "Care team check-in reminder",
                    message = "Please confirm today's medications and vitals for ${summary.episode.patientName}.",
                    deepLinkUri = "hdc://episode/${summary.episode.id}"
                )
            }
            _banner.value = "Bulk check-in reminders sent to ${visible.size} active discharge episode(s)."
        }
    }
}
