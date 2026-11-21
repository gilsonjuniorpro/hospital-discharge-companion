package com.hospitaldischargecompanion.feature.ehr

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hospitaldischargecompanion.R
import com.hospitaldischargecompanion.core.demo.SimulatedFailureMode
import com.hospitaldischargecompanion.core.ehr.ConflictResolutionChoice
import com.hospitaldischargecompanion.core.ui.components.EhrSyncStatusChip
import com.hospitaldischargecompanion.core.ui.components.ErrorRetryCard
import com.hospitaldischargecompanion.core.ui.components.SectionHeader
import com.hospitaldischargecompanion.core.ui.theme.HdcColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EhrSyncScreen(
    onBack: () -> Unit,
    viewModel: EhrSyncViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.nav_ehr_sync), style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (state.isSyncing) {
                item { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }
            }

            state.errorMessage?.let { err ->
                item {
                    ErrorRetryCard(
                        message = err,
                        onRetry = {
                            viewModel.dismissError()
                            viewModel.setFailureMode(SimulatedFailureMode.NONE)
                            viewModel.pushObservations()
                        }
                    )
                }
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

            state.episode?.let { ep ->
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
                                Text(text = ep.patientName, style = MaterialTheme.typography.titleMedium)
                                EhrSyncStatusChip(status = ep.ehrSyncStatus)
                            }
                            Text(
                                text = stringResource(R.string.ehr_fhir_resources_note),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = stringResource(R.string.ehr_last_sync_format, ep.lastEhrSyncIso),
                                style = MaterialTheme.typography.labelSmall
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { viewModel.pushObservations() }) {
                                    Text(text = stringResource(R.string.ehr_push_outbound))
                                }
                                OutlinedButton(onClick = { viewModel.pullDischargeSummary() }) {
                                    Text(text = stringResource(R.string.ehr_pull_summary))
                                }
                            }
                            TextButton(onClick = { viewModel.inspectFhirBundle() }) {
                                Text(text = stringResource(R.string.ehr_inspect_fhir_bundle))
                            }
                        }
                    }
                }
            }

            item {
                SectionHeader(title = stringResource(R.string.ehr_simulator_title))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val modes = listOf(
                        SimulatedFailureMode.NONE to R.string.sim_mode_ok,
                        SimulatedFailureMode.EHR_CONFLICT_409 to R.string.sim_mode_409,
                        SimulatedFailureMode.EHR_UNAUTHORIZED_401 to R.string.sim_mode_401,
                        SimulatedFailureMode.TIMEOUT to R.string.sim_mode_timeout
                    )
                    items(modes) { (mode, labelRes) ->
                        FilterChip(
                            selected = state.networkConfig.failureMode == mode,
                            onClick = { viewModel.setFailureMode(mode) },
                            label = { Text(text = stringResource(labelRes)) }
                        )
                    }
                }
            }

            if (state.openConflicts.isNotEmpty()) {
                item {
                    SectionHeader(title = stringResource(R.string.ehr_conflicts_title))
                }
                items(state.openConflicts, key = { it.id }) { conflict ->
                    Card(colors = CardDefaults.cardColors(containerColor = HdcColors.StatusUrgentContainer)) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = conflict.resourceType,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = HdcColors.StatusUrgent
                            )
                            Text(text = conflict.localSummary, style = MaterialTheme.typography.bodySmall)
                            Text(text = conflict.ehrUpstreamSummary, style = MaterialTheme.typography.bodySmall)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { viewModel.resolveConflict(conflict.id, ConflictResolutionChoice.KEEP_LOCAL_HOME) }) {
                                    Text(text = stringResource(R.string.ehr_keep_local))
                                }
                                OutlinedButton(onClick = { viewModel.resolveConflict(conflict.id, ConflictResolutionChoice.ACCEPT_EHR_UPSTREAM) }) {
                                    Text(text = stringResource(R.string.ehr_accept_upstream))
                                }
                            }
                        }
                    }
                }
            }

            item {
                SectionHeader(title = stringResource(R.string.ehr_sync_log_title))
            }
            items(state.logs, key = { it.id }) { entry ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = entry.direction, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                            EhrSyncStatusChip(status = entry.outcomeStatus)
                        }
                        Text(text = entry.detail, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }

    state.fhirBundlePreviewJson?.let { json ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissFhirBundle() },
            title = { Text(text = stringResource(R.string.ehr_fhir_bundle_dialog_title)) },
            text = {
                Text(
                    text = json,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.verticalScroll(rememberScrollState())
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissFhirBundle() }) {
                    Text(text = stringResource(R.string.action_close))
                }
            }
        )
    }
}
