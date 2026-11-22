package com.hospitaldischargecompanion.feature.medications

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.WarningAmber
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
import com.hospitaldischargecompanion.core.medication.DoseActionType
import com.hospitaldischargecompanion.core.medication.MedicationChangeStatus
import com.hospitaldischargecompanion.core.medication.MedicationItem
import com.hospitaldischargecompanion.core.medication.RefillStatus
import com.hospitaldischargecompanion.core.ui.components.ErrorRetryCard
import com.hospitaldischargecompanion.core.ui.components.LoadingStateBox
import com.hospitaldischargecompanion.core.ui.components.SectionHeader
import com.hospitaldischargecompanion.core.ui.components.StatusPill
import com.hospitaldischargecompanion.core.ui.theme.HdcColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicationScreen(
    onBack: () -> Unit,
    viewModel: MedicationViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var skipTargetMed by remember { mutableStateOf<MedicationItem?>(null) }
    var skipReasonNote by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(text = stringResource(R.string.nav_medications), style = MaterialTheme.typography.titleLarge)
                        Text(
                            text = stringResource(
                                R.string.med_adherence_banner_format,
                                state.adherence.adherencePercent,
                                state.adherence.takenCount,
                                state.adherence.totalScheduledActive
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
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
        if (state.isLoading) {
            LoadingStateBox(modifier = Modifier.padding(innerPadding))
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            state.errorMessage?.let { error ->
                item { ErrorRetryCard(message = error, onRetry = { viewModel.dismissError() }) }
            }

            if (state.warnings.isNotEmpty()) {
                item {
                    SectionHeader(title = stringResource(R.string.med_warnings_header))
                }
                items(state.warnings, key = { it.id }) { warning ->
                    Card(colors = CardDefaults.cardColors(containerColor = HdcColors.StatusWarningContainer)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.WarningAmber,
                                contentDescription = null,
                                tint = HdcColors.StatusWarning
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "${warning.severityLabel}: ${warning.title}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = HdcColors.StatusWarning
                                )
                                Text(text = warning.detail, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }

            item {
                SectionHeader(title = stringResource(R.string.med_list_header))
                Text(
                    text = stringResource(
                        R.string.med_reconciliation_summary,
                        state.reconciliationCounts.newCount,
                        state.reconciliationCounts.changedCount,
                        state.reconciliationCounts.continuedCount,
                        state.reconciliationCounts.stoppedCount
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 6.dp)
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val filters = listOf(
                        null to R.string.filter_all,
                        MedicationChangeStatus.NEW to R.string.med_filter_new,
                        MedicationChangeStatus.CHANGED to R.string.med_filter_changed,
                        MedicationChangeStatus.CONTINUED to R.string.med_filter_continued,
                        MedicationChangeStatus.STOPPED to R.string.med_filter_stopped
                    )
                    items(filters) { (status, labelRes) ->
                        FilterChip(
                            selected = state.selectedFilter == status,
                            onClick = { viewModel.selectFilter(status) },
                            label = { Text(text = stringResource(labelRes)) }
                        )
                    }
                }
            }

            items(state.medications, key = { it.id }) { med ->
                MedicationCard(
                    med = med,
                    highlighted = med.id == state.highlightMedicationId,
                    onTake = { viewModel.recordDose(med, DoseActionType.TAKEN) },
                    onSkip = {
                        skipReasonNote = ""
                        skipTargetMed = med
                    },
                    onSnooze = { viewModel.recordDose(med, DoseActionType.SNOOZED, "Snoozed 30 minutes") },
                    onAttachPhoto = { viewModel.attachMockPillBox(med.id) },
                    onRequestRefill = { viewModel.requestRefill(med.id) }
                )
            }

            if (state.doseLogs.isNotEmpty()) {
                item {
                    SectionHeader(title = stringResource(R.string.med_history_header))
                }
                items(state.doseLogs, key = { it.id }) { log ->
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = "${log.action.name} · ${log.attributionLabel}",
                                style = MaterialTheme.typography.labelLarge
                            )
                            log.reasonOrNote?.let {
                                Text(text = it, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }

    skipTargetMed?.let { med ->
        val presets = listOf(
            stringResource(R.string.med_skip_reason_dizziness),
            stringResource(R.string.med_skip_reason_nausea),
            stringResource(R.string.med_skip_reason_supply)
        )
        AlertDialog(
            onDismissRequest = { skipTargetMed = null },
            title = { Text(text = stringResource(R.string.med_skip_dialog_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "${med.name} ${med.strengthAndForm}", style = MaterialTheme.typography.titleSmall)
                    presets.forEach { preset ->
                        FilterChip(
                            selected = skipReasonNote == preset,
                            onClick = { skipReasonNote = preset },
                            label = { Text(text = preset) }
                        )
                    }
                    OutlinedTextField(
                        value = skipReasonNote,
                        onValueChange = { skipReasonNote = it },
                        label = { Text(text = stringResource(R.string.med_skip_reason_custom_hint)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.recordDose(
                        med = med,
                        action = DoseActionType.SKIPPED,
                        reason = skipReasonNote.ifBlank { "Skipped by patient/caregiver" }
                    )
                    skipTargetMed = null
                }) {
                    Text(text = stringResource(R.string.action_skip))
                }
            },
            dismissButton = {
                TextButton(onClick = { skipTargetMed = null }) {
                    Text(text = stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

@Composable
private fun MedicationCard(
    med: MedicationItem,
    highlighted: Boolean,
    onTake: () -> Unit,
    onSkip: () -> Unit,
    onSnooze: () -> Unit,
    onAttachPhoto: () -> Unit,
    onRequestRefill: () -> Unit
) {
    val (badgeBg, badgeFg) = when (med.changeStatus) {
        MedicationChangeStatus.NEW -> HdcColors.StatusInfoContainer to HdcColors.StatusInfo
        MedicationChangeStatus.CHANGED -> HdcColors.StatusWarningContainer to HdcColors.StatusWarning
        MedicationChangeStatus.CONTINUED -> HdcColors.StatusSuccessContainer to HdcColors.StatusSuccess
        MedicationChangeStatus.STOPPED -> HdcColors.StatusCriticalContainer to HdcColors.StatusCritical
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = if (highlighted) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
    ) {
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "${med.name} ${med.strengthAndForm}", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = med.scheduleLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                StatusPill(label = med.changeStatus.name, containerColor = badgeBg, contentColor = badgeFg)
            }

            Text(text = med.instructions, style = MaterialTheme.typography.bodyMedium)
            med.changeNote?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            med.foodInteraction?.let {
                Text(
                    text = stringResource(R.string.med_food_format, it),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            med.allergenNote?.let {
                Text(
                    text = stringResource(R.string.med_allergen_format, it),
                    style = MaterialTheme.typography.bodySmall,
                    color = HdcColors.StatusUrgent
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.med_supply_format, med.daysSupplyRemaining),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (med.refillStatus != RefillStatus.IN_STOCK) {
                    TextButton(onClick = onRequestRefill) {
                        Text(
                            text = if (med.refillStatus == RefillStatus.OUT_OF_STOCK) {
                                stringResource(R.string.med_out_of_stock_action)
                            } else {
                                stringResource(R.string.med_refill_action)
                            }
                        )
                    }
                }
            }

            med.lastActionAttribution?.let { attr ->
                Text(
                    text = stringResource(R.string.med_last_action_format, med.todayDoseStatus.name, attr),
                    style = MaterialTheme.typography.labelSmall,
                    color = HdcColors.StatusSuccess
                )
            }

            if (med.hasPillBoxAttachment) {
                Text(
                    text = med.pillBoxAttachmentLabel ?: stringResource(R.string.med_pillbox_attached),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            if (med.changeStatus != MedicationChangeStatus.STOPPED) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(onClick = onTake) {
                        Text(text = stringResource(R.string.action_take))
                    }
                    OutlinedButton(onClick = onSnooze) {
                        Text(text = stringResource(R.string.action_snooze))
                    }
                    OutlinedButton(onClick = onSkip) {
                        Text(text = stringResource(R.string.action_skip))
                    }
                    IconButton(onClick = onAttachPhoto) {
                        Icon(
                            imageVector = Icons.Outlined.PhotoCamera,
                            contentDescription = stringResource(R.string.med_attach_pillbox)
                        )
                    }
                }
            }
        }
    }
}
