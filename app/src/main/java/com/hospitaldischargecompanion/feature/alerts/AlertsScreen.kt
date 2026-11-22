package com.hospitaldischargecompanion.feature.alerts

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import com.hospitaldischargecompanion.core.alerts.AlertSeverity
import com.hospitaldischargecompanion.core.alerts.ClinicalAlert
import com.hospitaldischargecompanion.core.ui.components.EmptyStateCard
import com.hospitaldischargecompanion.core.ui.components.ErrorRetryCard
import com.hospitaldischargecompanion.core.ui.components.LoadingStateBox
import com.hospitaldischargecompanion.core.ui.components.StatusPill
import com.hospitaldischargecompanion.core.ui.theme.HdcColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertsScreen(
    onBack: () -> Unit,
    viewModel: AlertsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.nav_alerts), style = MaterialTheme.typography.titleLarge) },
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

            if (state.alerts.isEmpty()) {
                item {
                    EmptyStateCard(
                        title = stringResource(R.string.alerts_empty_title),
                        subtitle = stringResource(R.string.alerts_empty_subtitle),
                        icon = Icons.Outlined.CheckCircle
                    )
                }
            }

            items(state.alerts, key = { it.id }) { alert ->
                AlertDetailCard(
                    alert = alert,
                    highlighted = alert.id == state.highlightAlertId,
                    isClinician = state.activeRole.isClinician,
                    onAcknowledge = { viewModel.acknowledge(alert.id) },
                    onSnooze = { viewModel.snooze(alert.id) },
                    onEscalate = { viewModel.escalate(alert.id) },
                    onResolve = { viewModel.resolve(alert.id) }
                )
            }
        }
    }
}

@Composable
private fun AlertDetailCard(
    alert: ClinicalAlert,
    highlighted: Boolean,
    isClinician: Boolean,
    onAcknowledge: () -> Unit,
    onSnooze: () -> Unit,
    onEscalate: () -> Unit,
    onResolve: () -> Unit
) {
    val (sevBg, sevFg) = when (alert.severity) {
        AlertSeverity.CRITICAL -> HdcColors.StatusCriticalContainer to HdcColors.StatusCritical
        AlertSeverity.URGENT -> HdcColors.StatusUrgentContainer to HdcColors.StatusUrgent
        AlertSeverity.WARNING -> HdcColors.StatusWarningContainer to HdcColors.StatusWarning
        AlertSeverity.INFO -> HdcColors.StatusInfoContainer to HdcColors.StatusInfo
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
                StatusPill(label = alert.severity.name, containerColor = sevBg, contentColor = sevFg)
                StatusPill(
                    label = "${alert.status.name} · ${alert.escalationStage.name.replace('_', ' ')}",
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(text = alert.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(text = alert.summary, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = stringResource(R.string.alert_guidance_format, alert.patientGuidance),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = stringResource(R.string.alert_audit_trail_title),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(top = 4.dp)
            )
            alert.auditTrail.forEach { audit ->
                Text(
                    text = "• ${audit.actorName} (${audit.actorRole.name}): ${audit.actionSummary}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (alert.isActive) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isClinician) {
                        Button(onClick = onAcknowledge) {
                            Text(text = stringResource(R.string.action_acknowledge))
                        }
                        OutlinedButton(onClick = onSnooze) {
                            Text(text = stringResource(R.string.action_snooze))
                        }
                    }
                    OutlinedButton(onClick = onEscalate) {
                        Text(text = stringResource(R.string.action_escalate))
                    }
                    if (isClinician) {
                        OutlinedButton(onClick = onResolve) {
                            Text(text = stringResource(R.string.action_resolve))
                        }
                    }
                }
            }
        }
    }
}
