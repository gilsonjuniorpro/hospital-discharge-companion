package com.hospitaldischargecompanion.feature.patient

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hospitaldischargecompanion.R
import com.hospitaldischargecompanion.core.ui.components.EhrSyncStatusChip
import com.hospitaldischargecompanion.core.ui.components.EmptyStateCard
import com.hospitaldischargecompanion.core.ui.components.LoadingStateBox
import com.hospitaldischargecompanion.core.ui.components.PhiTeachingBadge
import com.hospitaldischargecompanion.core.ui.components.RedFlagEducationCard
import com.hospitaldischargecompanion.core.ui.components.RiskTierBadge
import com.hospitaldischargecompanion.core.ui.components.SectionHeader
import com.hospitaldischargecompanion.core.ui.theme.HdcColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientHomeScreen(
    onSwitchRole: () -> Unit,
    onOpenDemoControls: () -> Unit,
    onOpenCarePlan: (String) -> Unit,
    onOpenMedications: (String) -> Unit,
    onOpenTelemonitoring: (String) -> Unit,
    onOpenAlerts: (String) -> Unit,
    onOpenMessaging: (String) -> Unit,
    onOpenAppointments: (String) -> Unit,
    onOpenEducation: (String) -> Unit,
    onOpenEhrSync: (String) -> Unit,
    viewModel: PatientHomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(text = stringResource(R.string.patient_home_title), style = MaterialTheme.typography.titleLarge)
                        state.episode?.let {
                            Text(
                                text = "${it.patientName} · ${it.mrnMasked}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onSwitchRole) {
                        Icon(
                            imageVector = Icons.Outlined.SwapHoriz,
                            contentDescription = stringResource(R.string.action_switch_role)
                        )
                    }
                    IconButton(onClick = onOpenDemoControls) {
                        Icon(
                            imageVector = Icons.Outlined.Tune,
                            contentDescription = stringResource(R.string.action_demo_controls)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        if (state.isLoading) {
            LoadingStateBox(modifier = Modifier.padding(innerPadding))
            return@Scaffold
        }
        val episode = state.episode
        if (episode == null) {
            EmptyStateCard(
                title = stringResource(R.string.empty_episode_title),
                subtitle = stringResource(R.string.empty_episode_subtitle),
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(16.dp)
            )
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.allEpisodes) { item ->
                        FilterChip(
                            selected = item.id == episode.id,
                            onClick = { viewModel.selectEpisode(item.id) },
                            label = { Text(text = item.patientName) }
                        )
                    }
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RiskTierBadge(riskTier = episode.riskTier)
                            EhrSyncStatusChip(
                                status = episode.ehrSyncStatus,
                                onClick = { onOpenEhrSync(episode.id) }
                            )
                        }
                        Text(
                            text = episode.diagnosisTitle,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = episode.diagnosisSummary,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = stringResource(
                                R.string.discharge_snapshot_meta,
                                episode.dischargeDateIso,
                                episode.hospitalUnit,
                                episode.carePlanRevision
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = stringResource(
                                R.string.discharge_care_team_format,
                                episode.attendingPhysician,
                                episode.primaryNurse
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            PhiTeachingBadge()
                            Text(
                                text = stringResource(R.string.care_circle_code_format, episode.careCircleInviteCode),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }

            if (state.openAlerts.isNotEmpty()) {
                item {
                    val topAlert = state.openAlerts.first()
                    Card(
                        onClick = { onOpenAlerts(episode.id) },
                        colors = CardDefaults.cardColors(containerColor = HdcColors.StatusUrgentContainer)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.patient_active_alerts_banner, state.openAlerts.size),
                                style = MaterialTheme.typography.titleMedium,
                                color = HdcColors.StatusUrgent
                            )
                            Text(
                                text = topAlert.title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = topAlert.patientGuidance,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SummaryMetricCard(
                        title = stringResource(R.string.metric_med_adherence),
                        value = "${state.adherence.adherencePercent}%",
                        subtitle = stringResource(
                            R.string.metric_med_count_format,
                            state.adherence.takenCount,
                            state.adherence.totalScheduledActive
                        ),
                        onClick = { onOpenMedications(episode.id) },
                        modifier = Modifier.weight(1f)
                    )
                    SummaryMetricCard(
                        title = stringResource(R.string.metric_care_tasks),
                        value = "${state.completedTasksCount}/${state.totalTasksCount}",
                        subtitle = stringResource(R.string.metric_due_tasks_format, state.dueTasksCount),
                        onClick = { onOpenCarePlan(episode.id) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            state.latestCheckIn?.let { latest ->
                item {
                    Card(
                        onClick = { onOpenTelemonitoring(episode.id) },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.latest_vitals_title),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = stringResource(
                                    R.string.latest_vitals_line,
                                    latest.systolicMmHg,
                                    latest.diastolicMmHg,
                                    latest.heartRateBpm,
                                    latest.spo2Percent,
                                    latest.weightKg
                                ),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = stringResource(R.string.logged_by_format, latest.recordedByLabel),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item {
                SectionHeader(title = stringResource(R.string.section_care_modules))
                Spacer(modifier = Modifier.height(8.dp))
                val actions = listOf(
                    ModuleTile(R.string.nav_care_plan, Icons.Outlined.Checklist) { onOpenCarePlan(episode.id) },
                    ModuleTile(R.string.nav_medications, Icons.Outlined.Medication) { onOpenMedications(episode.id) },
                    ModuleTile(R.string.nav_telemonitoring, Icons.Outlined.MonitorHeart) { onOpenTelemonitoring(episode.id) },
                    ModuleTile(R.string.nav_alerts, Icons.Outlined.NotificationsActive) { onOpenAlerts(episode.id) },
                    ModuleTile(R.string.nav_messaging, Icons.Outlined.Forum) { onOpenMessaging(episode.id) },
                    ModuleTile(R.string.nav_appointments, Icons.Outlined.CalendarMonth) { onOpenAppointments(episode.id) },
                    ModuleTile(R.string.nav_education, Icons.Outlined.School) { onOpenEducation(episode.id) },
                    ModuleTile(R.string.nav_ehr_sync, Icons.Outlined.CloudSync) { onOpenEhrSync(episode.id) }
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    actions.chunked(2).forEach { pair ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            pair.forEach { tile ->
                                Card(
                                    onClick = tile.onClick,
                                    modifier = Modifier.weight(1f),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = tile.icon,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Text(
                                            text = stringResource(tile.labelRes),
                                            style = MaterialTheme.typography.labelLarge
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                RedFlagEducationCard(redFlags = episode.redFlags)
            }
        }
    }
}

private data class ModuleTile(
    val labelRes: Int,
    val icon: ImageVector,
    val onClick: () -> Unit
)

@Composable
private fun SummaryMetricCard(
    title: String,
    value: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
