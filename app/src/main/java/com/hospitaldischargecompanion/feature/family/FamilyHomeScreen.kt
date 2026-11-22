package com.hospitaldischargecompanion.feature.family

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
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.hospitaldischargecompanion.core.carecircle.CareCircleMemberStatus
import com.hospitaldischargecompanion.core.ui.components.ErrorRetryCard
import com.hospitaldischargecompanion.core.ui.components.LoadingStateBox
import com.hospitaldischargecompanion.core.ui.components.RiskTierBadge
import com.hospitaldischargecompanion.core.ui.components.SectionHeader
import com.hospitaldischargecompanion.core.ui.components.StatusPill
import com.hospitaldischargecompanion.core.ui.theme.HdcColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamilyHomeScreen(
    onSwitchRole: () -> Unit,
    onOpenDemoControls: () -> Unit,
    onOpenMedications: (String) -> Unit,
    onOpenTelemonitoring: (String) -> Unit,
    onOpenAlerts: (String) -> Unit,
    onOpenMessaging: (String) -> Unit,
    viewModel: FamilyHomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var inviteCodeInput by remember { mutableStateOf("") }
    var inviteNameInput by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(text = stringResource(R.string.family_home_title), style = MaterialTheme.typography.titleLarge)
                        state.episode?.let {
                            Text(
                                text = stringResource(R.string.family_caring_for_format, it.patientName),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
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
        val episode = state.episode ?: return@Scaffold

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
                            Text(text = episode.patientName, style = MaterialTheme.typography.titleLarge)
                            RiskTierBadge(riskTier = episode.riskTier)
                        }
                        Text(text = episode.diagnosisTitle, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = stringResource(R.string.family_dual_attribution_note),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { onOpenMedications(episode.id) }) {
                                Text(text = stringResource(R.string.family_assist_meds))
                            }
                            OutlinedButton(onClick = { onOpenTelemonitoring(episode.id) }) {
                                Text(text = stringResource(R.string.family_log_vitals))
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { viewModel.escalateOnBehalfOfPatient("Family requested urgent nurse review") },
                                colors = ButtonDefaults.buttonColors(containerColor = HdcColors.StatusUrgent)
                            ) {
                                Text(text = stringResource(R.string.family_escalate_nurse))
                            }
                            OutlinedButton(onClick = { onOpenMessaging(episode.id) }) {
                                Text(text = stringResource(R.string.nav_messaging))
                            }
                        }
                    }
                }
            }

            if (state.openAlerts.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = stringResource(R.string.family_shared_alerts),
                        actionLabel = stringResource(R.string.action_view_all),
                        onAction = { onOpenAlerts(episode.id) }
                    )
                }
                items(state.openAlerts, key = { it.id }) { alert ->
                    Card(colors = CardDefaults.cardColors(containerColor = HdcColors.StatusUrgentContainer)) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(text = alert.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text(text = alert.patientGuidance, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            item {
                SectionHeader(title = stringResource(R.string.family_pending_tasks))
            }
            items(state.todayTasks.take(4), key = { it.id }) { task ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = task.title, style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = task.scheduledTimeLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Button(onClick = { viewModel.completeTaskOnBehalf(task.id) }) {
                            Text(text = stringResource(R.string.action_confirm))
                        }
                    }
                }
            }

            item {
                SectionHeader(title = stringResource(R.string.family_care_circle_title))
                Text(
                    text = stringResource(R.string.care_circle_code_format, episode.careCircleInviteCode),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            items(state.careCircle, key = { it.id }) { member ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "${member.name} (${member.relationship})", style = MaterialTheme.typography.titleSmall)
                            StatusPill(
                                label = member.status.name,
                                containerColor = if (member.status == CareCircleMemberStatus.ACTIVE) {
                                    HdcColors.StatusSuccessContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                },
                                contentColor = if (member.status == CareCircleMemberStatus.ACTIVE) {
                                    HdcColors.StatusSuccess
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                        Text(
                            text = stringResource(R.string.family_permission_format, member.permission.name.replace('_', ' ')),
                            style = MaterialTheme.typography.bodySmall
                        )
                        if (member.status != CareCircleMemberStatus.REVOKED) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(onClick = { viewModel.cyclePermission(member) }) {
                                    Text(text = stringResource(R.string.family_change_permission))
                                }
                                TextButton(onClick = { viewModel.revokeMember(member.id) }) {
                                    Text(text = stringResource(R.string.action_revoke))
                                }
                            }
                        }
                    }
                }
            }

            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(text = stringResource(R.string.family_join_by_code_title), style = MaterialTheme.typography.titleSmall)
                        OutlinedTextField(
                            value = inviteCodeInput,
                            onValueChange = { inviteCodeInput = it },
                            label = { Text(text = stringResource(R.string.family_invite_code_hint)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = inviteNameInput,
                            onValueChange = { inviteNameInput = it },
                            label = { Text(text = stringResource(R.string.family_caregiver_name_hint)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(onClick = {
                            viewModel.acceptInviteCode(inviteCodeInput, inviteNameInput)
                            inviteCodeInput = ""
                            inviteNameInput = ""
                        }) {
                            Text(text = stringResource(R.string.action_join_care_circle))
                        }
                    }
                }
            }
        }
    }
}
