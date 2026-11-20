package com.hospitaldischargecompanion.feature.appointments

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.hospitaldischargecompanion.core.appointments.AppointmentItem
import com.hospitaldischargecompanion.core.appointments.AppointmentStatus
import com.hospitaldischargecompanion.core.ui.components.ErrorRetryCard
import com.hospitaldischargecompanion.core.ui.components.LoadingStateBox
import com.hospitaldischargecompanion.core.ui.components.StatusPill
import com.hospitaldischargecompanion.core.ui.theme.HdcColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppointmentsScreen(
    onBack: () -> Unit,
    viewModel: AppointmentsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var rescheduleTarget by remember { mutableStateOf<AppointmentItem?>(null) }
    var preferredSlotInput by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.nav_appointments), style = MaterialTheme.typography.titleLarge) },
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

            items(state.appointments, key = { it.id }) { appt ->
                val (bg, fg) = when (appt.status) {
                    AppointmentStatus.ATTENDED -> HdcColors.StatusSuccessContainer to HdcColors.StatusSuccess
                    AppointmentStatus.MISSED -> HdcColors.StatusCriticalContainer to HdcColors.StatusCritical
                    AppointmentStatus.RESCHEDULE_REQUESTED -> HdcColors.StatusWarningContainer to HdcColors.StatusWarning
                    AppointmentStatus.SCHEDULED -> HdcColors.StatusInfoContainer to HdcColors.StatusInfo
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
                                text = appt.category.name.replace('_', ' '),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            StatusPill(label = appt.status.name.replace('_', ' '), containerColor = bg, contentColor = fg)
                        }
                        Text(text = appt.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(text = "${appt.scheduledDateTimeLabel} · ${appt.providerName}", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = "${appt.locationDisplay} — ${appt.directionsNote}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        appt.requestedNewSlot?.let { slot ->
                            Text(
                                text = stringResource(R.string.appointment_requested_slot_format, slot),
                                style = MaterialTheme.typography.labelSmall,
                                color = HdcColors.StatusWarning
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (appt.status == AppointmentStatus.SCHEDULED) {
                                Button(onClick = { viewModel.markAttended(appt.id) }) {
                                    Text(text = stringResource(R.string.action_mark_attended))
                                }
                                OutlinedButton(onClick = { viewModel.markMissed(appt.id) }) {
                                    Text(text = stringResource(R.string.action_mark_missed))
                                }
                                TextButton(onClick = {
                                    preferredSlotInput = ""
                                    rescheduleTarget = appt
                                }) {
                                    Text(text = stringResource(R.string.action_reschedule))
                                }
                            } else if (appt.status == AppointmentStatus.RESCHEDULE_REQUESTED && state.activeRole.isClinician) {
                                Button(onClick = { viewModel.approveReschedule(appt.id) }) {
                                    Text(text = stringResource(R.string.action_approve_reschedule))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    rescheduleTarget?.let { target ->
        val morning = stringResource(R.string.appointment_slot_morning)
        val afternoon = stringResource(R.string.appointment_slot_afternoon)
        AlertDialog(
            onDismissRequest = { rescheduleTarget = null },
            title = { Text(text = stringResource(R.string.appointment_reschedule_dialog_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = target.title, style = MaterialTheme.typography.titleSmall)
                    FilterChip(
                        selected = preferredSlotInput == morning,
                        onClick = { preferredSlotInput = morning },
                        label = { Text(text = morning) }
                    )
                    FilterChip(
                        selected = preferredSlotInput == afternoon,
                        onClick = { preferredSlotInput = afternoon },
                        label = { Text(text = afternoon) }
                    )
                    OutlinedTextField(
                        value = preferredSlotInput,
                        onValueChange = { preferredSlotInput = it },
                        label = { Text(text = stringResource(R.string.appointment_slot_hint)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.requestReschedule(target.id, preferredSlotInput.ifBlank { morning })
                    rescheduleTarget = null
                }) {
                    Text(text = stringResource(R.string.action_reschedule))
                }
            },
            dismissButton = {
                TextButton(onClick = { rescheduleTarget = null }) {
                    Text(text = stringResource(R.string.action_cancel))
                }
            }
        )
    }
}
