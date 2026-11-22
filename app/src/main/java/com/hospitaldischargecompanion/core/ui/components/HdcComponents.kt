package com.hospitaldischargecompanion.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.HealthAndSafety
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hospitaldischargecompanion.R
import com.hospitaldischargecompanion.core.discharge.EhrSyncStatus
import com.hospitaldischargecompanion.core.discharge.RiskTier
import com.hospitaldischargecompanion.core.ui.theme.HdcColors

@Composable
fun StatusPill(
    label: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    onClick: (() -> Unit)? = null
) {
    val clickableMod = if (onClick != null) {
        modifier.clip(RoundedCornerShape(50)).clickable(onClick = onClick)
    } else {
        modifier.clip(RoundedCornerShape(50))
    }
    Surface(
        color = containerColor,
        contentColor = contentColor,
        shape = RoundedCornerShape(50),
        modifier = clickableMod
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp)
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun RiskTierBadge(riskTier: RiskTier, modifier: Modifier = Modifier) {
    val (labelRes, bg, fg) = when (riskTier) {
        RiskTier.HIGH -> Triple(R.string.risk_high, HdcColors.StatusCriticalContainer, HdcColors.StatusCritical)
        RiskTier.MEDIUM -> Triple(R.string.risk_medium, HdcColors.StatusWarningContainer, HdcColors.StatusWarning)
        RiskTier.LOW -> Triple(R.string.risk_low, HdcColors.StatusSuccessContainer, HdcColors.StatusSuccess)
    }
    StatusPill(
        label = stringResource(labelRes),
        containerColor = bg,
        contentColor = fg,
        modifier = modifier
    )
}

@Composable
fun EhrSyncStatusChip(
    status: EhrSyncStatus,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val (labelRes, icon, bg, fg) = when (status) {
        EhrSyncStatus.SYNCED -> Quad(R.string.ehr_status_synced, Icons.Outlined.CloudDone, HdcColors.StatusSuccessContainer, HdcColors.StatusSuccess)
        EhrSyncStatus.PENDING -> Quad(R.string.ehr_status_pending, Icons.Outlined.CloudSync, HdcColors.StatusInfoContainer, HdcColors.StatusInfo)
        EhrSyncStatus.CONFLICT -> Quad(R.string.ehr_status_conflict, Icons.Outlined.WarningAmber, HdcColors.StatusUrgentContainer, HdcColors.StatusUrgent)
        EhrSyncStatus.FAILED -> Quad(R.string.ehr_status_failed, Icons.Outlined.CloudOff, HdcColors.StatusCriticalContainer, HdcColors.StatusCritical)
    }
    StatusPill(
        label = stringResource(labelRes),
        containerColor = bg,
        contentColor = fg,
        icon = icon,
        onClick = onClick,
        modifier = modifier
    )
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@Composable
fun PhiTeachingBadge(modifier: Modifier = Modifier) {
    val description = stringResource(R.string.phi_badge_a11y)
    StatusPill(
        label = stringResource(R.string.phi_badge_label),
        containerColor = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        icon = Icons.Outlined.Lock,
        modifier = modifier.semantics { contentDescription = description }
    )
}

@Composable
fun RedFlagEducationCard(
    redFlags: List<String>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = HdcColors.StatusCriticalContainer)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.WarningAmber,
                    contentDescription = null,
                    tint = HdcColors.StatusCritical
                )
                Text(
                    text = stringResource(R.string.red_flag_card_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = HdcColors.StatusCritical
                )
            }
            redFlags.forEach { flag ->
                Text(
                    text = "• $flag",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF410E0B)
                )
            }
            Text(
                text = stringResource(R.string.red_flag_copy_disclaimer),
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF5D1B17)
            )
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        if (actionLabel != null && onAction != null) {
            TextButton(onClick = onAction) {
                Text(text = actionLabel)
            }
        }
    }
}

@Composable
fun ErrorRetryCard(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.ErrorOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            Button(onClick = onRetry) {
                Text(text = stringResource(R.string.action_retry))
            }
        }
    }
}

@Composable
fun EmptyStateCard(
    title: String,
    subtitle: String,
    icon: ImageVector = Icons.Outlined.Info,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun LoadingStateBox(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
fun DisclaimerBanner(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Outlined.HealthAndSafety,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = stringResource(R.string.disclaimer_banner),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
