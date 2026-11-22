package com.hospitaldischargecompanion.feature.telemonitor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Bluetooth
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hospitaldischargecompanion.R
import com.hospitaldischargecompanion.core.demo.TemperatureUnit
import com.hospitaldischargecompanion.core.telemonitor.VitalCheckIn
import com.hospitaldischargecompanion.core.ui.components.ErrorRetryCard
import com.hospitaldischargecompanion.core.ui.components.LoadingStateBox
import com.hospitaldischargecompanion.core.ui.components.SectionHeader
import com.hospitaldischargecompanion.core.ui.components.StatusPill
import com.hospitaldischargecompanion.core.ui.theme.HdcColors
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelemonitoringScreen(
    onBack: () -> Unit,
    onOpenAlerts: (String) -> Unit,
    viewModel: TelemonitoringViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.nav_telemonitoring), style = MaterialTheme.typography.titleLarge) },
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            state.errorMessage?.let { error ->
                item { ErrorRetryCard(message = error, onRetry = { viewModel.dismissError() }) }
            }

            if (state.triggeredAlerts.isNotEmpty()) {
                item {
                    Card(
                        onClick = { state.episode?.let { onOpenAlerts(it.id) } },
                        colors = CardDefaults.cardColors(containerColor = HdcColors.StatusCriticalContainer)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.vitals_alert_fired_title, state.triggeredAlerts.size),
                                style = MaterialTheme.typography.titleMedium,
                                color = HdcColors.StatusCritical
                            )
                            Text(
                                text = state.triggeredAlerts.first().patientGuidance,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            } else if (state.lastSuccessNotice) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = HdcColors.StatusSuccessContainer)) {
                        Text(
                            text = stringResource(R.string.vitals_saved_banner),
                            style = MaterialTheme.typography.bodyMedium,
                            color = HdcColors.StatusSuccess,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            }

            state.baseline?.let { base ->
                val latest = state.checkIns.lastOrNull()
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.vitals_baseline_title),
                                style = MaterialTheme.typography.titleMedium
                            )
                            val deltaWeight = latest?.let { it.weightKg - base.dryWeightKg } ?: 0.0
                            Text(
                                text = stringResource(
                                    R.string.vitals_baseline_summary,
                                    base.dryWeightKg,
                                    latest?.weightKg ?: base.dryWeightKg,
                                    deltaWeight
                                ),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            if (state.checkIns.size >= 2) {
                                VitalsTrendChart(
                                    checkIns = state.checkIns,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(120.dp)
                                        .padding(top = 8.dp)
                                )
                                Text(
                                    text = stringResource(R.string.vitals_chart_legend),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            item {
                SectionHeader(
                    title = stringResource(R.string.vitals_checkin_form_title),
                    actionLabel = stringResource(R.string.vitals_demo_abnormal_preset),
                    onAction = { viewModel.fillOutOfRangeBpPreset() }
                )
            }

            item {
                val d = state.draft
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            VitalTextField(
                                value = d.systolicInput,
                                label = stringResource(R.string.vital_systolic_label),
                                onValueChange = { v -> viewModel.updateDraft { it.copy(systolicInput = v) } },
                                modifier = Modifier.weight(1f)
                            )
                            VitalTextField(
                                value = d.diastolicInput,
                                label = stringResource(R.string.vital_diastolic_label),
                                onValueChange = { v -> viewModel.updateDraft { it.copy(diastolicInput = v) } },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            VitalTextField(
                                value = d.heartRateInput,
                                label = stringResource(R.string.vital_hr_label),
                                onValueChange = { v -> viewModel.updateDraft { it.copy(heartRateInput = v) } },
                                modifier = Modifier.weight(1f)
                            )
                            VitalTextField(
                                value = d.spo2Input,
                                label = stringResource(R.string.vital_spo2_label),
                                onValueChange = { v -> viewModel.updateDraft { it.copy(spo2Input = v) } },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            VitalTextField(
                                value = d.weightInput,
                                label = stringResource(R.string.vital_weight_label),
                                onValueChange = { v -> viewModel.updateDraft { it.copy(weightInput = v) } },
                                modifier = Modifier.weight(1f)
                            )
                            VitalTextField(
                                value = d.temperatureCInput,
                                label = stringResource(R.string.vital_temp_c_label),
                                onValueChange = { v -> viewModel.updateDraft { it.copy(temperatureCInput = v) } },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Text(
                            text = stringResource(R.string.vital_dyspnea_slider, d.dyspneaSeverity),
                            style = MaterialTheme.typography.labelLarge
                        )
                        Slider(
                            value = d.dyspneaSeverity.toFloat(),
                            onValueChange = { v -> viewModel.updateDraft { it.copy(dyspneaSeverity = v.roundToInt()) } },
                            valueRange = 0f..5f,
                            steps = 4
                        )

                        Text(
                            text = stringResource(R.string.vital_pain_slider, d.painScore),
                            style = MaterialTheme.typography.labelLarge
                        )
                        Slider(
                            value = d.painScore.toFloat(),
                            onValueChange = { v -> viewModel.updateDraft { it.copy(painScore = v.roundToInt()) } },
                            valueRange = 0f..10f,
                            steps = 9
                        )

                        OutlinedTextField(
                            value = d.notes,
                            onValueChange = { v -> viewModel.updateDraft { it.copy(notes = v) } },
                            label = { Text(text = stringResource(R.string.vital_notes_hint)) },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2
                        )

                        Button(
                            onClick = { viewModel.submitCheckIn() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = stringResource(R.string.action_submit_checkin))
                        }
                    }
                }
            }

            item {
                SectionHeader(title = stringResource(R.string.wearables_section_title))
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        state.wearables.forEach { device ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = device.name, style = MaterialTheme.typography.labelLarge)
                                    Text(
                                        text = "${device.modality} · ${device.lastReadingLabel}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Outlined.Bluetooth,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        OutlinedButton(onClick = { viewModel.syncWearables() }) {
                            Text(text = stringResource(R.string.action_sync_wearable_stub))
                        }
                    }
                }
            }

            item {
                SectionHeader(title = stringResource(R.string.vitals_history_title))
            }

            items(state.checkIns.reversed(), key = { it.id }) { reading ->
                val tempDisplay = if (state.temperatureUnit == TemperatureUnit.FAHRENHEIT) {
                    "${String.format("%.1f", reading.temperatureC * 9.0 / 5.0 + 32.0)} °F"
                } else {
                    "${String.format("%.1f", reading.temperatureC)} °C"
                }
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
                            Text(
                                text = "Day ${reading.dayNumber} · BP ${reading.systolicMmHg}/${reading.diastolicMmHg} mmHg",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (reading.isBloodPressureAbnormal || reading.isHypoxic || reading.isFebrile) {
                                StatusPill(
                                    label = stringResource(R.string.vital_flag_abnormal),
                                    containerColor = HdcColors.StatusCriticalContainer,
                                    contentColor = HdcColors.StatusCritical
                                )
                            }
                        }
                        Text(
                            text = "HR ${reading.heartRateBpm} bpm · SpO2 ${reading.spo2Percent}% · ${reading.weightKg} kg · $tempDisplay",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (reading.freeTextNotes.isNotBlank()) {
                            Text(
                                text = reading.freeTextNotes,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = stringResource(R.string.logged_by_format, reading.recordedByLabel),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VitalTextField(
    value: String,
    label: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(text = label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        modifier = modifier
    )
}

@Composable
private fun VitalsTrendChart(
    checkIns: List<VitalCheckIn>,
    modifier: Modifier = Modifier
) {
    val bpColor = HdcColors.ClinicalTeal
    val weightColor = HdcColors.AccentCoral
    Canvas(modifier = modifier) {
        if (checkIns.size < 2) return@Canvas
        val stepX = size.width / (checkIns.size - 1).coerceAtLeast(1)
        val bpMin = 90f
        val bpMax = 190f
        val wtMin = (checkIns.minOf { it.weightKg } - 1.5).toFloat()
        val wtMax = (checkIns.maxOf { it.weightKg } + 1.5).toFloat().coerceAtLeast(wtMin + 2f)

        val bpPath = Path()
        val wtPath = Path()

        checkIns.forEachIndexed { idx, item ->
            val x = idx * stepX
            val bpNorm = ((item.systolicMmHg - bpMin) / (bpMax - bpMin)).coerceIn(0f, 1f)
            val bpY = size.height - (bpNorm * size.height * 0.85f) - size.height * 0.08f
            if (idx == 0) bpPath.moveTo(x, bpY) else bpPath.lineTo(x, bpY)

            val wtNorm = ((item.weightKg.toFloat() - wtMin) / (wtMax - wtMin)).coerceIn(0f, 1f)
            val wtY = size.height - (wtNorm * size.height * 0.85f) - size.height * 0.08f
            if (idx == 0) wtPath.moveTo(x, wtY) else wtPath.lineTo(x, wtY)

            drawCircle(color = bpColor, radius = 6f, center = Offset(x, bpY))
            drawCircle(color = weightColor, radius = 6f, center = Offset(x, wtY))
        }

        drawPath(path = bpPath, color = bpColor, style = Stroke(width = 4f, cap = StrokeCap.Round))
        drawPath(path = wtPath, color = weightColor, style = Stroke(width = 4f, cap = StrokeCap.Round))
    }
}
