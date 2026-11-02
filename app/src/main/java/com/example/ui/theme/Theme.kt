package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val IPhoneDarkColorScheme = darkColorScheme(
    primary = IPhoneYellow,
    onPrimary = IPhoneBlack,
    primaryContainer = IPhoneControlFill,
    onPrimaryContainer = IPhoneYellow,
    secondary = IPhoneCyan,
    onSecondary = IPhoneBlack,
    secondaryContainer = IPhoneCardElevated,
    onSecondaryContainer = IPhoneCyan,
    tertiary = IPhoneBlue,
    onTertiary = IPhoneTextPrimary,
    background = IPhoneBlack,
    onBackground = IPhoneTextPrimary,
    surface = IPhoneDarkSurface,
    onSurface = IPhoneTextPrimary,
    surfaceVariant = IPhoneCardDark,
    onSurfaceVariant = IPhoneTextSecondary,
    outline = IPhoneBorder,
    error = IPhoneRed,
    onError = IPhoneBlack
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = IPhoneDarkColorScheme,
        typography = Typography,
        content = content
    )
}
