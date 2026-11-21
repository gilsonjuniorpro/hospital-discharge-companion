package com.hospitaldischargecompanion.feature.demo

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
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hospitaldischargecompanion.R
import com.hospitaldischargecompanion.core.demo.AppLocale
import com.hospitaldischargecompanion.core.demo.AppThemeMode
import com.hospitaldischargecompanion.core.demo.DemoScenarioPreset
import com.hospitaldischargecompanion.core.demo.SimulatedFailureMode
import com.hospitaldischargecompanion.core.demo.TemperatureUnit
import com.hospitaldischargecompanion.core.ui.HdcTestTags
import com.hospitaldischargecompanion.core.ui.components.DisclaimerBanner
import com.hospitaldischargecompanion.core.ui.components.SectionHeader
import com.hospitaldischargecompanion.core.ui.theme.HdcColors
import kotlin.math.roundToLong

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DemoControlsScreen(
    onBack: () -> Unit,
    onOpenDeepLink: (String) -> Unit,
    viewModel: DemoControlsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.action_demo_controls), style = MaterialTheme.typography.titleLarge) },
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
                .padding(innerPadding)
                .testTag(HdcTestTags.SCREEN_DEMO_CONTROLS),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            state.statusBanner?.let { banner ->
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
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(text = stringResource(R.string.demo_reset_title), style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = stringResource(R.string.demo_reset_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = { viewModel.resetDemonstration() },
                            colors = ButtonDefaults.buttonColors(containerColor = HdcColors.StatusCritical)
                        ) {
                            Icon(imageVector = Icons.Outlined.RestartAlt, contentDescription = null)
                            Text(text = stringResource(R.string.action_reset_demo), modifier = Modifier.padding(start = 6.dp))
                        }
                    }
                }
            }

            item {
                SectionHeader(title = stringResource(R.string.demo_scenarios_title))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val scenarios = listOf(
                        DemoScenarioPreset.FULL_ADHERENCE to R.string.scenario_full_adherence,
                        DemoScenarioPreset.MISSED_DOSE to R.string.scenario_missed_dose,
                        DemoScenarioPreset.CRITICAL_BP to R.string.scenario_critical_bp,
                        DemoScenarioPreset.EHR_CONFLICT_409 to R.string.scenario_ehr_conflict,
                        DemoScenarioPreset.CAREGIVER_CONFIRMED_MEDS to R.string.scenario_caregiver_meds,
                        DemoScenarioPreset.PHYSICIAN_AMENDED_PLAN to R.string.scenario_physician_amend
                    )
                    items(scenarios) { (preset, labelRes) ->
                        OutlinedButton(onClick = { viewModel.applyScenario(preset) }) {
                            Text(text = stringResource(labelRes))
                        }
                    }
                }
            }

            item {
                SectionHeader(title = stringResource(R.string.section_language))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val locales = listOf(
                        AppLocale.EN to R.string.locale_en,
                        AppLocale.PT_BR to R.string.locale_pt_br,
                        AppLocale.ES to R.string.locale_es,
                        AppLocale.FR to R.string.locale_fr
                    )
                    items(locales) { (loc, res) ->
                        FilterChip(
                            selected = state.session.locale == loc,
                            onClick = { viewModel.setLocale(loc) },
                            label = { Text(text = stringResource(res)) }
                        )
                    }
                }
                Row(
                    modifier = Modifier.padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = state.session.temperatureUnit == TemperatureUnit.CELSIUS,
                        onClick = { viewModel.setTemperatureUnit(TemperatureUnit.CELSIUS) },
                        label = { Text(text = stringResource(R.string.unit_celsius)) }
                    )
                    FilterChip(
                        selected = state.session.temperatureUnit == TemperatureUnit.FAHRENHEIT,
                        onClick = { viewModel.setTemperatureUnit(TemperatureUnit.FAHRENHEIT) },
                        label = { Text(text = stringResource(R.string.unit_fahrenheit)) }
                    )
                }
                Text(
                    text = stringResource(R.string.demo_theme_title),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(top = 10.dp)
                )
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val modes = listOf(
                        AppThemeMode.SYSTEM to R.string.theme_system,
                        AppThemeMode.LIGHT to R.string.theme_light,
                        AppThemeMode.DARK to R.string.theme_dark
                    )
                    modes.forEach { (mode, res) ->
                        FilterChip(
                            selected = state.session.themeMode == mode,
                            onClick = { viewModel.setThemeMode(mode) },
                            label = { Text(text = stringResource(res)) }
                        )
                    }
                }
            }

            item {
                SectionHeader(title = stringResource(R.string.ehr_simulator_title))
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.demo_latency_format, state.networkConfig.latencyMs),
                            style = MaterialTheme.typography.labelLarge
                        )
                        Slider(
                            value = state.networkConfig.latencyMs.toFloat(),
                            onValueChange = { viewModel.setLatencyMs(it.roundToLong()) },
                            valueRange = 0f..2000f
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val modes = listOf(
                                SimulatedFailureMode.NONE to R.string.sim_mode_ok,
                                SimulatedFailureMode.EHR_CONFLICT_409 to R.string.sim_mode_409,
                                SimulatedFailureMode.EHR_UNAUTHORIZED_401 to R.string.sim_mode_401,
                                SimulatedFailureMode.TIMEOUT to R.string.sim_mode_timeout,
                                SimulatedFailureMode.NETWORK_UNAVAILABLE to R.string.sim_mode_offline
                            )
                            items(modes) { (mode, res) ->
                                FilterChip(
                                    selected = state.networkConfig.failureMode == mode,
                                    onClick = { viewModel.setFailureMode(mode) },
                                    label = { Text(text = stringResource(res)) }
                                )
                            }
                        }
                    }
                }
            }

            item {
                SectionHeader(
                    title = stringResource(R.string.demo_notifications_title),
                    actionLabel = stringResource(R.string.demo_push_reminder),
                    onAction = { viewModel.triggerSampleNotification() }
                )
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
                            Text(text = stringResource(R.string.pref_quiet_hours), style = MaterialTheme.typography.bodyMedium)
                            Switch(
                                checked = state.session.quietHoursEnabled,
                                onCheckedChange = { viewModel.setQuietHours(it) }
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = stringResource(R.string.pref_caregiver_alerts), style = MaterialTheme.typography.bodyMedium)
                            Switch(
                                checked = state.session.caregiverAlertsEnabled,
                                onCheckedChange = { viewModel.setCaregiverAlerts(it) }
                            )
                        }
                    }
                }
            }

            items(state.notifications, key = { it.id }) { notif ->
                Card(
                    onClick = {
                        viewModel.markNotificationRead(notif.id)
                        onOpenDeepLink(notif.deepLinkUri)
                    },
                    colors = CardDefaults.cardColors(
                        containerColor = if (notif.isRead) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Notifications,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = notif.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text(text = notif.message, style = MaterialTheme.typography.bodySmall)
                            Text(
                                text = notif.deepLinkUri,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            item {
                SectionHeader(title = stringResource(R.string.about_section_title))
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.about_presentation_script_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = stringResource(R.string.about_presentation_script_body),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        OutlinedButton(onClick = { onOpenDeepLink("hdc://episode/ep-hf-001") }) {
                            Text(text = stringResource(R.string.about_jump_hf_case))
                        }
                    }
                }
            }

            item {
                DisclaimerBanner()
            }
        }
    }
}
