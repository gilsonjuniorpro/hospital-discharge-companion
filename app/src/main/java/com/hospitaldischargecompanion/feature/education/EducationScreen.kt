package com.hospitaldischargecompanion.feature.education

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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import com.hospitaldischargecompanion.core.education.ModuleProgress
import com.hospitaldischargecompanion.core.ui.components.LoadingStateBox
import com.hospitaldischargecompanion.core.ui.components.StatusPill
import com.hospitaldischargecompanion.core.ui.theme.HdcColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EducationScreen(
    onBack: () -> Unit,
    viewModel: EducationViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.nav_education), style = MaterialTheme.typography.titleLarge) },
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
            item {
                FilterChip(
                    selected = state.showClinicalDetail,
                    onClick = { viewModel.toggleClinicalDetail() },
                    label = { Text(text = stringResource(R.string.education_clinical_detail_toggle)) }
                )
            }

            state.quizFeedback?.let { feedback ->
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = HdcColors.StatusInfoContainer)) {
                        Text(
                            text = feedback,
                            style = MaterialTheme.typography.bodyMedium,
                            color = HdcColors.StatusInfo,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            }

            items(state.modules, key = { it.id }) { module ->
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
                            Text(
                                text = stringResource(R.string.education_read_time_format, module.estimatedMinutes),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            StatusPill(
                                label = module.progress.name.replace('_', ' '),
                                containerColor = if (module.progress == ModuleProgress.COMPLETED) {
                                    HdcColors.StatusSuccessContainer
                                } else {
                                    HdcColors.StatusInfoContainer
                                },
                                contentColor = if (module.progress == ModuleProgress.COMPLETED) {
                                    HdcColors.StatusSuccess
                                } else {
                                    HdcColors.StatusInfo
                                }
                            )
                        }
                        Text(text = module.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(text = module.plainLanguageBody, style = MaterialTheme.typography.bodyMedium)

                        if (state.showClinicalDetail) {
                            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = stringResource(R.string.education_clinician_detail_header),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(text = module.clinicalDetailBody, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }

                        module.teachBackQuiz?.let { quiz ->
                            Text(
                                text = stringResource(R.string.education_teachback_header),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(text = quiz.question, style = MaterialTheme.typography.bodyMedium)
                            quiz.options.forEachIndexed { idx, opt ->
                                OutlinedButton(
                                    onClick = { viewModel.answerQuiz(module.id, idx) },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(text = opt)
                                }
                            }
                        }

                        if (module.progress != ModuleProgress.COMPLETED) {
                            Button(onClick = { viewModel.markCompleted(module.id) }) {
                                Text(text = stringResource(R.string.action_mark_completed))
                            }
                        }
                    }
                }
            }
        }
    }
}
