package com.hospitaldischargecompanion.feature.patient

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hospitaldischargecompanion.core.alerts.AlertRepository
import com.hospitaldischargecompanion.core.alerts.ClinicalAlert
import com.hospitaldischargecompanion.core.demo.DemoSessionRepository
import com.hospitaldischargecompanion.core.discharge.CarePlanRepository
import com.hospitaldischargecompanion.core.discharge.CarePlanTask
import com.hospitaldischargecompanion.core.discharge.CarePlanTaskStatus
import com.hospitaldischargecompanion.core.discharge.DischargeEpisode
import com.hospitaldischargecompanion.core.discharge.DischargeEpisodeRepository
import com.hospitaldischargecompanion.core.i18n.LocalizedClinicalPack
import com.hospitaldischargecompanion.core.medication.AdherenceSummary
import com.hospitaldischargecompanion.core.medication.MedicationRepository
import com.hospitaldischargecompanion.core.telemonitor.TelemonitoringRepository
import com.hospitaldischargecompanion.core.telemonitor.VitalCheckIn
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PatientHomeUiState(
    val episode: DischargeEpisode? = null,
    val allEpisodes: List<DischargeEpisode> = emptyList(),
    val dueTasksCount: Int = 0,
    val completedTasksCount: Int = 0,
    val totalTasksCount: Int = 0,
    val nextTask: CarePlanTask? = null,
    val adherence: AdherenceSummary = AdherenceSummary(0, 0, 0, 0, 100),
    val latestCheckIn: VitalCheckIn? = null,
    val openAlerts: List<ClinicalAlert> = emptyList(),
    val isLoading: Boolean = true
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PatientHomeViewModel @Inject constructor(
    private val sessionRepository: DemoSessionRepository,
    private val episodeRepository: DischargeEpisodeRepository,
    private val carePlanRepository: CarePlanRepository,
    private val medicationRepository: MedicationRepository,
    private val telemonitoringRepository: TelemonitoringRepository,
    private val alertRepository: AlertRepository
) : ViewModel() {

    val uiState: StateFlow<PatientHomeUiState> = sessionRepository.sessionState
        .flatMapLatest { session ->
            val episodeId = session.selectedEpisodeId
            combine(
                episodeRepository.observeAllEpisodes(),
                carePlanRepository.observeTasks(episodeId),
                medicationRepository.observeAdherence(episodeId),
                telemonitoringRepository.observeCheckIns(episodeId),
                alertRepository.observeEpisodeAlerts(episodeId)
            ) { allEpisodes, tasks, adherence, checkIns, alerts ->
                val localizedEpisodes = allEpisodes.map { LocalizedClinicalPack.applyTo(it, session.locale) }
                val activeEpisode = localizedEpisodes.firstOrNull { it.id == episodeId } ?: localizedEpisodes.firstOrNull()
                val dueOrOverdue = tasks.filter {
                    it.status == CarePlanTaskStatus.DUE || it.status == CarePlanTaskStatus.OVERDUE
                }
                PatientHomeUiState(
                    episode = activeEpisode,
                    allEpisodes = localizedEpisodes,
                    dueTasksCount = dueOrOverdue.size,
                    completedTasksCount = tasks.count { it.status == CarePlanTaskStatus.COMPLETED },
                    totalTasksCount = tasks.size,
                    nextTask = dueOrOverdue.firstOrNull() ?: tasks.firstOrNull(),
                    adherence = adherence,
                    latestCheckIn = checkIns.lastOrNull(),
                    openAlerts = alerts.filter { it.isActive },
                    isLoading = false
                )
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = PatientHomeUiState()
        )

    fun selectEpisode(episodeId: String) {
        viewModelScope.launch {
            sessionRepository.setSelectedEpisode(episodeId)
        }
    }
}
