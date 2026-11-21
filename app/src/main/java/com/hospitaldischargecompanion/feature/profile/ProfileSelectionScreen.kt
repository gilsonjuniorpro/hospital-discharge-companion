package com.hospitaldischargecompanion.feature.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.FamilyRestroom
import androidx.compose.material.icons.outlined.LocalHospital
import androidx.compose.material.icons.outlined.MedicalServices
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hospitaldischargecompanion.R
import com.hospitaldischargecompanion.core.demo.AppLocale
import com.hospitaldischargecompanion.core.demo.UserRole
import com.hospitaldischargecompanion.core.ui.HdcTestTags
import com.hospitaldischargecompanion.core.ui.components.DisclaimerBanner
import com.hospitaldischargecompanion.core.ui.components.EhrSyncStatusChip
import com.hospitaldischargecompanion.core.ui.components.LoadingStateBox
import com.hospitaldischargecompanion.core.ui.components.PhiTeachingBadge
import com.hospitaldischargecompanion.core.ui.components.RiskTierBadge
import com.hospitaldischargecompanion.core.ui.components.SectionHeader
import com.hospitaldischargecompanion.core.ui.components.StatusPill
import com.hospitaldischargecompanion.core.ui.theme.HdcColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSelectionScreen(
    onRoleSelected: (UserRole, String) -> Unit,
    onOpenDemoControls: () -> Unit,
    viewModel: ProfileSelectionViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = stringResource(R.string.profile_subtitle),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
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

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag(HdcTestTags.SCREEN_PROFILE),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PhiTeachingBadge()
                    Text(
                        text = stringResource(R.string.offline_mode_badge),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            item {
                SectionHeader(title = stringResource(R.string.section_language))
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val locales = listOf(
                        AppLocale.EN to R.string.locale_en,
                        AppLocale.PT_BR to R.string.locale_pt_br,
                        AppLocale.ES to R.string.locale_es,
                        AppLocale.FR to R.string.locale_fr
                    )
                    items(locales) { (locale, labelRes) ->
                        FilterChip(
                            selected = state.selectedLocale == locale,
                            onClick = { viewModel.selectLocale(locale) },
                            label = { Text(text = stringResource(labelRes)) }
                        )
                    }
                }
            }

            item {
                SectionHeader(title = stringResource(R.string.section_seeded_episode))
                Spacer(modifier = Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.episodes.forEach { episode ->
                        val selected = episode.id == state.selectedEpisodeId
                        Card(
                            onClick = { viewModel.selectEpisode(episode.id) },
                            colors = CardDefaults.cardColors(
                                containerColor = if (selected) {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                                } else {
                                    MaterialTheme.colorScheme.surface
                                }
                            ),
                            border = BorderStroke(
                                width = if (selected) 2.dp else 1.dp,
                                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                            )
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
                                    Text(
                                        text = "${episode.patientName} (${episode.patientAge})",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    RiskTierBadge(riskTier = episode.riskTier)
                                }
                                Text(
                                    text = episode.diagnosisTitle,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = stringResource(
                                            R.string.episode_day_unit_format,
                                            episode.dischargeDayNumber,
                                            episode.hospitalUnit
                                        ),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    EhrSyncStatusChip(status = episode.ehrSyncStatus)
                                }
                            }
                        }
                    }
                }
            }

            item {
                SectionHeader(title = stringResource(R.string.section_choose_role))
            }

            items(roleCards()) { item ->
                val isLast = state.lastRole == item.role
                Card(
                    onClick = { viewModel.selectRole(item.role, onRoleSelected) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(
                        width = if (isLast) 2.dp else 1.dp,
                        color = if (isLast) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(30.dp)
                        )
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = stringResource(item.titleRes),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                if (isLast) {
                                    StatusPill(
                                        label = stringResource(R.string.role_last_used),
                                        containerColor = HdcColors.ClinicalTealContainer,
                                        contentColor = HdcColors.ClinicalTeal
                                    )
                                }
                            }
                            Text(
                                text = stringResource(item.descriptionRes),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                DisclaimerBanner()
            }
        }
    }
}

private data class RoleCardModel(
    val role: UserRole,
    val titleRes: Int,
    val descriptionRes: Int,
    val icon: ImageVector
)

private fun roleCards(): List<RoleCardModel> = listOf(
    RoleCardModel(
        role = UserRole.PATIENT,
        titleRes = R.string.role_patient_title,
        descriptionRes = R.string.role_patient_desc,
        icon = Icons.Outlined.Person
    ),
    RoleCardModel(
        role = UserRole.FAMILY,
        titleRes = R.string.role_family_title,
        descriptionRes = R.string.role_family_desc,
        icon = Icons.Outlined.FamilyRestroom
    ),
    RoleCardModel(
        role = UserRole.NURSE,
        titleRes = R.string.role_nurse_title,
        descriptionRes = R.string.role_nurse_desc,
        icon = Icons.Outlined.MedicalServices
    ),
    RoleCardModel(
        role = UserRole.PHYSICIAN,
        titleRes = R.string.role_physician_title,
        descriptionRes = R.string.role_physician_desc,
        icon = Icons.Outlined.LocalHospital
    )
)
