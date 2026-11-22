package com.hospitaldischargecompanion.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object HdcColors {
    val ClinicalTeal = Color(0xFF0E6B6B)
    val ClinicalTealContainer = Color(0xFFCFF2F0)
    val DeepClinicalNavy = Color(0xFF1F4E78)
    val DeepNavyContainer = Color(0xFFD6E6F7)
    val AccentCoral = Color(0xFFC25E3E)

    val LightBackground = Color(0xFFF5F9F9)
    val LightSurface = Color(0xFFFFFFFF)
    val LightSurfaceVariant = Color(0xFFE7EFEF)

    val DarkBackground = Color(0xFF0E1516)
    val DarkSurface = Color(0xFF162022)
    val DarkSurfaceVariant = Color(0xFF213033)

    val StatusSuccess = Color(0xFF1D7A4C)
    val StatusSuccessContainer = Color(0xFFD8F3E4)
    val StatusWarning = Color(0xFFB26A00)
    val StatusWarningContainer = Color(0xFFFFEFC7)
    val StatusUrgent = Color(0xFFD14B1F)
    val StatusUrgentContainer = Color(0xFFFFE0D6)
    val StatusCritical = Color(0xFFB3261E)
    val StatusCriticalContainer = Color(0xFFF9DEDC)
    val StatusInfo = Color(0xFF1A659E)
    val StatusInfoContainer = Color(0xFFD7ECFA)
}

private val HdcLightColorScheme = lightColorScheme(
    primary = HdcColors.ClinicalTeal,
    onPrimary = Color.White,
    primaryContainer = HdcColors.ClinicalTealContainer,
    onPrimaryContainer = Color(0xFF002020),
    secondary = HdcColors.DeepClinicalNavy,
    onSecondary = Color.White,
    secondaryContainer = HdcColors.DeepNavyContainer,
    onSecondaryContainer = Color(0xFF0B2540),
    tertiary = HdcColors.AccentCoral,
    onTertiary = Color.White,
    background = HdcColors.LightBackground,
    onBackground = Color(0xFF141D1E),
    surface = HdcColors.LightSurface,
    onSurface = Color(0xFF141D1E),
    surfaceVariant = HdcColors.LightSurfaceVariant,
    onSurfaceVariant = Color(0xFF425456),
    outline = Color(0xFF718587),
    error = HdcColors.StatusCritical,
    onError = Color.White,
    errorContainer = HdcColors.StatusCriticalContainer,
    onErrorContainer = Color(0xFF410E0B)
)

private val HdcDarkColorScheme = darkColorScheme(
    primary = Color(0xFF5BD3D1),
    onPrimary = Color(0xFF003737),
    primaryContainer = Color(0xFF005050),
    onPrimaryContainer = HdcColors.ClinicalTealContainer,
    secondary = Color(0xFFA2C9F0),
    onSecondary = Color(0xFF0C3152),
    secondaryContainer = Color(0xFF23476A),
    onSecondaryContainer = HdcColors.DeepNavyContainer,
    tertiary = Color(0xFFFFB59D),
    onTertiary = Color(0xFF5B1E0B),
    background = HdcColors.DarkBackground,
    onBackground = Color(0xFFE2ECEC),
    surface = HdcColors.DarkSurface,
    onSurface = Color(0xFFE2ECEC),
    surfaceVariant = HdcColors.DarkSurfaceVariant,
    onSurfaceVariant = Color(0xFFB5C7C9),
    outline = Color(0xFF839698),
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = HdcColors.StatusCriticalContainer
)

private val HdcTypography = Typography(
    headlineMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 28.sp
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp
    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp
    )
)

@Composable
fun HdcTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) HdcDarkColorScheme else HdcLightColorScheme,
        typography = HdcTypography,
        content = content
    )
}
