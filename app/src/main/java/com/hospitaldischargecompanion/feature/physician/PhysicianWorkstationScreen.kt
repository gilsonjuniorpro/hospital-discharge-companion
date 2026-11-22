package com.hospitaldischargecompanion.feature.physician

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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hospitaldischargecompanion.R
import com.hospitaldischargecompanion.core.ui.components.EhrSyncStatusChip
import com.hospitaldischargecompanion.core.ui.components.EmptyStateCard
import com.hospitaldischargecompanion.core.ui.components.ErrorRetryCard
import com.hospitaldischargecompanion.core.ui.components.LoadingStateBox
import com.hospitaldischargecompanion.core.ui.components.RiskTierBadge
import com.hospitaldischargecompanion.core.ui.components.SectionHeader
import com.hospitaldischargecompanion.core.ui.components.StatusPill
import com.hospitaldischargecompanion.core.ui.theme.HdcColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhysicianWorkstationScreen(
    onSwitchRole: () -> Unit,
    onOpenDemoControls: () -> Unit,
    onOpenCarePlan: (String) -> Unit,
    onOpenEhrSync: (String) -> Unit,
    viewModel: PhysicianWorkstationViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var amendmentSummary by remember { mutableStateOf("Add evening weight & BP recheck due to fluid trend") }
    var newTaskTitle by remember { mutableStateOf("Evening dry-weight & seated BP recheck") }
    var newRedFlag by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(text = stringResource(R.string.physician_workstation_title), style = MaterialTheme.typography.titleLarge)
                        Text(
                            text = stringResource(R.string.physician_workstation_subtitle),
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
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            state.errorMessage?.let { err ->
                item { ErrorRetryCard(message = err, onRetry = { viewModel.dismissError() }) }
            }
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
                SectionHeader(title = stringResource(R.string.physician_escalated_queue_title, state.escalatedQueue.size))
            }

            if (state.escalatedQueue.isEmpty()) {
                item {
                    EmptyStateCard(
                        title = stringResource(R.string.physician_queue_empty_title),
                        subtitle = stringResource(R.string.physician_queue_empty_subtitle)
                    )
                }
            } else {
                items(state.escalatedQueue, key = { it.id }) { alert ->
                    Card(colors = CardDefaults.cardColors(containerColor = HdcColors.StatusCriticalContainer)) {
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
                                StatusPill(
                                    label = alert.severity.name,
                                    containerColor = HdcColors.StatusCritical,
                                    contentColor = MaterialTheme.colorScheme.onError
                                )
                                Text(
                                    text = alert.escalationStage.name.replace('_', ' '),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                            Text(text = alert.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text(text = alert.summary, style = MaterialTheme.typography.bodyMedium)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { viewModel.selectEpisode(alert.episodeId) }) {
                                    Text(text = stringResource(R.string.physician_open_sheet))
                                }
                                OutlinedButton(onClick = { viewModel.resolveEscalatedAlert(alert.id) }) {
                                    Text(text = stringResource(R.string.action_resolve))
                                }
                            }
                        }
                    }
                }
            }

            item {
                SectionHeader(title = stringResource(R.string.physician_clinical_sheet_title))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.episodes) { ep ->
                        FilterChip(
                            selected = ep.id == state.selectedEpisode?.id,
                            onClick = { viewModel.selectEpisode(ep.id) },
                            label = { Text(text = ep.patientName) }
                        )
                    }
                }
            }

            state.selectedEpisode?.let { ep ->
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "${ep.patientName} · ${ep.mrnMasked}", style = MaterialTheme.typography.titleMedium)
                                RiskTierBadge(riskTier = ep.riskTier)
                            }
                            Text(text = ep.diagnosisTitle, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                            Text(text = ep.riskRationale, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.care_plan_revision_badge, ep.carePlanRevision),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                EhrSyncStatusChip(status = ep.ehrSyncStatus, onClick = { onOpenEhrSync(ep.id) })
                            }
                            state.recentVitals.lastOrNull()?.let { v ->
                                Text(
                                    text = stringResource(
                                        R.string.latest_vitals_line,
                                        v.systolicMmHg,
                                        v.diastolicMmHg,
                                        v.heartRateBpm,
                                        v.spo2Percent,
                                        v.weightKg
                                    ),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            Text(
                                text = stringResource(
                                    R.string.physician_med_count_format,
                                    state.medications.size,
                                    ep.dispositionNote
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = stringResource(R.string.physician_active_meds_header),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            state.medications.forEach { med ->
                                Text(
                                    text = "• [${med.changeStatus.name}] ${med.name} ${med.strengthAndForm} (${med.todayDoseStatus.name})",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            if (state.revisions.isNotEmpty()) {
                                Text(
                                    text = stringResource(R.string.physician_signed_revisions_header),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                                state.revisions.take(3).forEach { rev ->
                                    Text(
                                        text = "• v${rev.revisionNumber} (${rev.authorName}): ${rev.changeSummary}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            OutlinedButton(onClick = { onOpenCarePlan(ep.id) }) {
                                Text(text = stringResource(R.string.physician_inspect_care_plan))
                            }
                        }
                    }
                }

                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.physician_amend_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                val options = listOf(
                                    ClinicalDisposition.CONTINUE_HOME to R.string.disposition_continue_home,
                                    ClinicalDisposition.EARLIER_FOLLOW_UP to R.string.disposition_earlier_followup,
                                    ClinicalDisposition.ADVISE_ER_COPY_ONLY to R.string.disposition_advise_er
                                )
                                items(options) { (disp, labelRes) ->
                                    FilterChip(
                                        selected = state.selectedDisposition == disp,
                                        onClick = { viewModel.selectDisposition(disp) },
                                        label = { Text(text = stringResource(labelRes)) }
                                    )
                                }
                            }
                            OutlinedTextField(
                                value = amendmentSummary,
                                onValueChange = { amendmentSummary = it },
                                label = { Text(text = stringResource(R.string.physician_amend_summary_hint)) },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = newTaskTitle,
                                onValueChange = { newTaskTitle = it },
                                label = { Text(text = stringResource(R.string.physician_amend_task_hint)) },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = newRedFlag,
                                onValueChange = { newRedFlag = it },
                                label = { Text(text = stringResource(R.string.physician_amend_redflag_hint)) },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Button(
                                onClick = {
                                    viewModel.amendAndSignOffCarePlan(amendmentSummary, newTaskTitle, newRedFlag)
                                    newRedFlag = ""
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(text = stringResource(R.string.physician_sign_off_action))
                            }
                        }
                    }
                }
            }
        }
    }
}
