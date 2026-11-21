package com.hospitaldischargecompanion.feature.nurse

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hospitaldischargecompanion.R
import com.hospitaldischargecompanion.core.alerts.AlertSeverity
import com.hospitaldischargecompanion.core.ui.HdcTestTags
import com.hospitaldischargecompanion.core.ui.components.EhrSyncStatusChip
import com.hospitaldischargecompanion.core.ui.components.LoadingStateBox
import com.hospitaldischargecompanion.core.ui.components.RiskTierBadge
import com.hospitaldischargecompanion.core.ui.components.SectionHeader
import com.hospitaldischargecompanion.core.ui.components.StatusPill
import com.hospitaldischargecompanion.core.ui.theme.HdcColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NurseWorkstationScreen(
    onSwitchRole: () -> Unit,
    onOpenDemoControls: () -> Unit,
    onOpenCarePlan: (String) -> Unit,
    onOpenMessaging: (String) -> Unit,
    onOpenTelemonitoring: (String) -> Unit,
    onOpenEhrSync: (String) -> Unit,
    viewModel: NurseWorkstationViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var noteTargetEpisode by remember { mutableStateOf<Pair<String, String>?>(null) }
    var handoffReport by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(text = stringResource(R.string.nurse_workstation_title), style = MaterialTheme.typography.titleLarge)
                        Text(
                            text = stringResource(R.string.nurse_workstation_subtitle),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
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

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag(HdcTestTags.SCREEN_NURSE),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            state.feedbackBanner?.let { banner ->
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = HdcColors.StatusSuccessContainer)) {
                        Text(
                            text = banner,
                            style = MaterialTheme.typography.bodyMedium,
                            color = HdcColors.StatusSuccess,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            }

            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.nurse_handoff_title),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(text = state.shiftHandoffSummary, style = MaterialTheme.typography.bodyMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { viewModel.bulkSendReminders() }) {
                                Text(text = stringResource(R.string.nurse_bulk_remind))
                            }
                            TextButton(onClick = { handoffReport = viewModel.buildShiftHandoffReport() }) {
                                Text(text = stringResource(R.string.nurse_export_handoff))
                            }
                        }
                    }
                }
            }

            item {
                SectionHeader(title = stringResource(R.string.nurse_triage_queue_title, state.triageQueue.size))
            }

            items(state.triageQueue, key = { it.id }) { alert ->
                val (bg, fg) = when (alert.severity) {
                    AlertSeverity.CRITICAL -> HdcColors.StatusCriticalContainer to HdcColors.StatusCritical
                    AlertSeverity.URGENT -> HdcColors.StatusUrgentContainer to HdcColors.StatusUrgent
                    AlertSeverity.WARNING -> HdcColors.StatusWarningContainer to HdcColors.StatusWarning
                    AlertSeverity.INFO -> HdcColors.StatusInfoContainer to HdcColors.StatusInfo
                }
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            StatusPill(label = alert.severity.name, containerColor = bg, contentColor = fg)
                            Text(
                                text = alert.escalationStage.name.replace('_', ' '),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(text = alert.title, style = MaterialTheme.typography.titleMedium)
                        Text(text = alert.summary, style = MaterialTheme.typography.bodyMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { viewModel.acknowledgeAlert(alert.id) }) {
                                Text(text = stringResource(R.string.action_acknowledge))
                            }
                            OutlinedButton(onClick = { viewModel.escalateToPhysician(alert.id) }) {
                                Text(text = stringResource(R.string.nurse_escalate_md))
                            }
                            TextButton(onClick = { viewModel.resolveAlert(alert.id) }) {
                                Text(text = stringResource(R.string.action_resolve))
                            }
                        }
                    }
                }
            }

            item {
                SectionHeader(title = stringResource(R.string.nurse_census_title))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val chips = listOf(
                        NurseCensusFilter.ALL to R.string.filter_all,
                        NurseCensusFilter.HIGH_RISK to R.string.filter_high_risk,
                        NurseCensusFilter.OVERDUE_TASKS to R.string.filter_overdue,
                        NurseCensusFilter.OPEN_ALERTS to R.string.filter_open_alerts
                    )
                    items(chips) { (filter, labelRes) ->
                        FilterChip(
                            selected = state.filter == filter,
                            onClick = { viewModel.setFilter(filter) },
                            label = { Text(text = stringResource(labelRes)) }
                        )
                    }
                }
            }

            items(state.census, key = { it.episode.id }) { item ->
                val ep = item.episode
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "${ep.patientName} (${ep.patientAge})", style = MaterialTheme.typography.titleMedium)
                            RiskTierBadge(riskTier = ep.riskTier)
                        }
                        Text(text = ep.diagnosisTitle, style = MaterialTheme.typography.bodyMedium)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(
                                    R.string.nurse_census_metrics,
                                    item.adherencePercent,
                                    item.openAlertsCount,
                                    item.overdueTasksCount
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            EhrSyncStatusChip(status = ep.ehrSyncStatus, onClick = { onOpenEhrSync(ep.id) })
                        }
                        item.latestCheckIn?.let { v ->
                            Text(
                                text = stringResource(
                                    R.string.latest_vitals_line,
                                    v.systolicMmHg,
                                    v.diastolicMmHg,
                                    v.heartRateBpm,
                                    v.spo2Percent,
                                    v.weightKg
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { viewModel.requestRecheckAndDocumentNote(ep.id, ep.patientName) }) {
                                Text(text = stringResource(R.string.nurse_request_recheck_note))
                            }
                            OutlinedButton(onClick = { onOpenCarePlan(ep.id) }) {
                                Text(text = stringResource(R.string.nav_care_plan))
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { onOpenTelemonitoring(ep.id) }) {
                                Text(text = stringResource(R.string.nav_telemonitoring))
                            }
                            TextButton(onClick = { onOpenMessaging(ep.id) }) {
                                Text(text = stringResource(R.string.nav_messaging))
                            }
                            TextButton(onClick = { noteTargetEpisode = ep.id to ep.patientName }) {
                                Text(text = stringResource(R.string.nurse_templates_action))
                            }
                        }
                    }
                }
            }
        }
    }

    handoffReport?.let { report ->
        AlertDialog(
            onDismissRequest = { handoffReport = null },
            title = { Text(text = stringResource(R.string.nurse_handoff_dialog_title)) },
            text = { Text(text = report, style = MaterialTheme.typography.bodySmall) },
            confirmButton = {
                TextButton(onClick = { handoffReport = null }) {
                    Text(text = stringResource(R.string.action_close))
                }
            }
        )
    }

    noteTargetEpisode?.let { (epId, patientName) ->
        AlertDialog(
            onDismissRequest = { noteTargetEpisode = null },
            title = { Text(text = stringResource(R.string.nurse_templates_dialog_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = patientName, style = MaterialTheme.typography.titleSmall)
                    state.noteTemplates.forEach { tpl ->
                        OutlinedButton(
                            onClick = {
                                viewModel.postNursingTemplateNote(epId, patientName, tpl)
                                noteTargetEpisode = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = tpl.title)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { noteTargetEpisode = null }) {
                    Text(text = stringResource(R.string.action_cancel))
                }
            }
        )
    }
}
