package com.hospitaldischargecompanion.feature.messaging

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
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.hospitaldischargecompanion.core.ui.components.ErrorRetryCard
import com.hospitaldischargecompanion.core.ui.components.LoadingStateBox
import com.hospitaldischargecompanion.core.ui.components.StatusPill
import com.hospitaldischargecompanion.core.ui.theme.HdcColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagingScreen(
    onBack: () -> Unit,
    viewModel: MessagingViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var messageInput by remember { mutableStateOf("") }
    var clinicalNoteChecked by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(text = stringResource(R.string.nav_messaging), style = MaterialTheme.typography.titleLarge)
                        state.episode?.let {
                            Text(
                                text = "${it.patientName} · ${state.activeRole.name}",
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
                }
            )
        }
    ) { innerPadding ->
        if (state.isLoading) {
            LoadingStateBox(modifier = Modifier.padding(innerPadding))
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (state.hasCriticalAlert) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    colors = CardDefaults.cardColors(containerColor = HdcColors.StatusCriticalContainer)
                ) {
                    Text(
                        text = stringResource(R.string.messaging_urgent_alert_banner),
                        style = MaterialTheme.typography.labelLarge,
                        color = HdcColors.StatusCritical,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            state.errorMessage?.let { err ->
                ErrorRetryCard(
                    message = err,
                    onRetry = { viewModel.dismissError() },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    label = { Text(text = stringResource(R.string.messaging_search_hint)) },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = state.clinicalNotesOnly,
                    onClick = { viewModel.toggleClinicalNotesOnly() },
                    label = { Text(text = stringResource(R.string.messaging_filter_clinical_only)) }
                )
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(state.messages, key = { it.id }) { msg ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (msg.isClinicalNote) {
                                MaterialTheme.colorScheme.secondaryContainer
                            } else {
                                MaterialTheme.colorScheme.surface
                            }
                        )
                    ) {
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
                                Text(
                                    text = "${msg.senderName} (${msg.senderRole.name})",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                                if (msg.isClinicalNote) {
                                    StatusPill(
                                        label = stringResource(R.string.messaging_clinical_note_badge),
                                        containerColor = HdcColors.DeepClinicalNavy,
                                        contentColor = MaterialTheme.colorScheme.onSecondary
                                    )
                                }
                            }
                            Text(text = msg.body, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = if (msg.readByCareTeam) {
                                    stringResource(R.string.messaging_read_receipt_seen)
                                } else {
                                    stringResource(R.string.messaging_read_receipt_delivered)
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.templates, key = { it.id }) { tpl ->
                        AssistChip(
                            onClick = { messageInput = tpl.body },
                            label = { Text(text = tpl.label) }
                        )
                    }
                }
                if (state.activeRole.canWriteClinicalNotes) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = clinicalNoteChecked,
                            onCheckedChange = { clinicalNoteChecked = it }
                        )
                        Text(
                            text = stringResource(R.string.messaging_mark_clinical_note),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = messageInput,
                        onValueChange = { messageInput = it },
                        label = { Text(text = stringResource(R.string.messaging_input_hint)) },
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = {
                            if (messageInput.isNotBlank()) {
                                viewModel.sendMessage(messageInput, clinicalNoteChecked)
                                messageInput = ""
                                clinicalNoteChecked = false
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.Send,
                            contentDescription = stringResource(R.string.action_send)
                        )
                    }
                }
            }
        }
    }
}
