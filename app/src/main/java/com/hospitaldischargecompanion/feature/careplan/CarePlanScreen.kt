package com.hospitaldischargecompanion.feature.careplan

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
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.History
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
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hospitaldischargecompanion.R
import com.hospitaldischargecompanion.core.discharge.CarePlanTask
import com.hospitaldischargecompanion.core.discharge.CarePlanTaskStatus
import com.hospitaldischargecompanion.core.ui.components.EmptyStateCard
import com.hospitaldischargecompanion.core.ui.components.ErrorRetryCard
import com.hospitaldischargecompanion.core.ui.components.LoadingStateBox
import com.hospitaldischargecompanion.core.ui.components.RedFlagEducationCard
import com.hospitaldischargecompanion.core.ui.components.StatusPill
import com.hospitaldischargecompanion.core.ui.theme.HdcColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CarePlanScreen(
    onBack: () -> Unit,
    viewModel: CarePlanViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var skipTargetTask by remember { mutableStateOf<CarePlanTask?>(null) }
    var skipReason by remember { mutableStateOf("") }
    var showRevisions by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(text = stringResource(R.string.nav_care_plan), style = MaterialTheme.typography.titleLarge)
                        state.episode?.let {
                            Text(
                                text = stringResource(R.string.care_plan_revision_badge, it.carePlanRevision),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showRevisions = true }) {
                        Icon(
                            imageVector = Icons.Outlined.History,
                            contentDescription = stringResource(R.string.care_plan_history_title)
                        )
                    }
                    IconButton(onClick = { viewModel.exportSummary() }) {
                        Icon(
                            imageVector = Icons.Outlined.Description,
                            contentDescription = stringResource(R.string.care_plan_export_title)
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

        val grouped = state.tasks.groupBy { it.dayNumber }.toSortedMap()

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            state.errorMessage?.let { error ->
                item {
                    ErrorRetryCard(message = error, onRetry = { viewModel.dismissError() })
                }
            }

            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(
                                R.string.care_plan_progress_format,
                                state.completedCount,
                                state.totalCount,
                                state.completionPercent
                            ),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        LinearProgressIndicator(
                            progress = { (state.completionPercent / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val chips = listOf(
                                null to R.string.filter_all,
                                CarePlanTaskStatus.DUE to R.string.filter_due,
                                CarePlanTaskStatus.OVERDUE to R.string.filter_overdue,
                                CarePlanTaskStatus.COMPLETED to R.string.filter_completed,
                                CarePlanTaskStatus.SKIPPED to R.string.filter_skipped
                            )
                            items(chips) { (status, res) ->
                                FilterChip(
                                    selected = state.selectedStatusFilter == status,
                                    onClick = { viewModel.selectStatusFilter(status) },
                                    label = { Text(text = stringResource(res)) }
                                )
                            }
                        }
                    }
                }
            }

            if (grouped.isEmpty()) {
                item {
                    EmptyStateCard(
                        title = stringResource(R.string.care_plan_empty_title),
                        subtitle = stringResource(R.string.care_plan_empty_subtitle)
                    )
                }
            }

            grouped.forEach { (day, dayTasks) ->
                item {
                    Text(
                        text = stringResource(R.string.care_plan_day_header, day),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                items(dayTasks, key = { it.id }) { task ->
                    CarePlanTaskCard(
                        task = task,
                        canReopen = state.activeRole.canReopenTasks,
                        onComplete = { viewModel.completeTask(task.id) },
                        onSkipClick = {
                            skipReason = ""
                            skipTargetTask = task
                        },
                        onReopen = { viewModel.reopenTask(task.id) }
                    )
                }
            }

            state.episode?.let { ep ->
                item {
                    RedFlagEducationCard(redFlags = ep.redFlags)
                }
            }
        }
    }

    skipTargetTask?.let { target ->
        AlertDialog(
            onDismissRequest = { skipTargetTask = null },
            title = { Text(text = stringResource(R.string.care_plan_skip_dialog_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = target.title, style = MaterialTheme.typography.bodyMedium)
                    OutlinedTextField(
                        value = skipReason,
                        onValueChange = { skipReason = it },
                        label = { Text(text = stringResource(R.string.care_plan_skip_reason_hint)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.skipTask(target.id, skipReason)
                    skipTargetTask = null
                }) {
                    Text(text = stringResource(R.string.action_skip))
                }
            },
            dismissButton = {
                TextButton(onClick = { skipTargetTask = null }) {
                    Text(text = stringResource(R.string.action_cancel))
                }
            }
        )
    }

    if (showRevisions) {
        AlertDialog(
            onDismissRequest = { showRevisions = false },
            title = { Text(text = stringResource(R.string.care_plan_history_title)) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    state.revisions.forEach { rev ->
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "v${rev.revisionNumber} · ${rev.authorName}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(text = rev.changeSummary, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showRevisions = false }) {
                    Text(text = stringResource(R.string.action_close))
                }
            }
        )
    }

    state.exportedSummary?.let { summary ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissExport() },
            title = { Text(text = stringResource(R.string.care_plan_export_title)) },
            text = {
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.verticalScroll(rememberScrollState())
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissExport() }) {
                    Text(text = stringResource(R.string.action_close))
                }
            }
        )
    }
}

@Composable
private fun CarePlanTaskCard(
    task: CarePlanTask,
    canReopen: Boolean,
    onComplete: () -> Unit,
    onSkipClick: () -> Unit,
    onReopen: () -> Unit
) {
    val (bg, fg) = when (task.status) {
        CarePlanTaskStatus.COMPLETED -> HdcColors.StatusSuccessContainer to HdcColors.StatusSuccess
        CarePlanTaskStatus.OVERDUE, CarePlanTaskStatus.ESCALATED -> HdcColors.StatusCriticalContainer to HdcColors.StatusCritical
        CarePlanTaskStatus.SKIPPED -> HdcColors.StatusWarningContainer to HdcColors.StatusWarning
        CarePlanTaskStatus.DUE -> HdcColors.ClinicalTealContainer to HdcColors.ClinicalTeal
        CarePlanTaskStatus.UPCOMING -> HdcColors.StatusInfoContainer to HdcColors.StatusInfo
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
                Text(
                    text = "${task.type.name.replace('_', ' ')} · ${task.scheduledTimeLabel}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                StatusPill(label = task.status.name, containerColor = bg, contentColor = fg)
            }
            Text(text = task.title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = task.instructions,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            task.completedByLabel?.let { actor ->
                Text(
                    text = stringResource(R.string.logged_by_format, actor),
                    style = MaterialTheme.typography.labelSmall,
                    color = HdcColors.StatusSuccess
                )
            }
            task.skipReason?.let { reason ->
                Text(
                    text = stringResource(R.string.care_plan_skip_reason_format, reason),
                    style = MaterialTheme.typography.labelSmall,
                    color = HdcColors.StatusWarning
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (task.status != CarePlanTaskStatus.COMPLETED) {
                    Button(onClick = onComplete) {
                        Text(text = stringResource(R.string.action_complete))
                    }
                    OutlinedButton(onClick = onSkipClick) {
                        Text(text = stringResource(R.string.action_skip))
                    }
                }
                if (canReopen && (task.status == CarePlanTaskStatus.COMPLETED || task.status == CarePlanTaskStatus.SKIPPED)) {
                    OutlinedButton(onClick = onReopen) {
                        Text(text = stringResource(R.string.action_reopen))
                    }
                }
            }
        }
    }
}
