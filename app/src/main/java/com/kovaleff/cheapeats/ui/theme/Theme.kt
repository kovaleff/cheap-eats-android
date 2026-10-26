package com.kovaleff.cheapeats.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

// Приложение всегда в тёмной фирменной теме веб-версии «Карта нищеброда»
private val CheapEatsColorScheme = darkColorScheme(
    primary = AccentLemon,
    onPrimary = DarkInk,
    secondary = AccentLime,
    onSecondary = DarkInk,
    tertiary = BrandGreen,
    background = BgDark,
    onBackground = TextLight,
    surface = BgDark,
    onSurface = TextLight,
    surfaceVariant = GreyPanelSoft,
    onSurfaceVariant = TextLight,
    surfaceContainer = GreyPanelSoft,
    surfaceContainerHigh = GreyPanel,
    outline = TextMuted,
    error = BrandRed,
)

@Composable
fun CheapEatsTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = CheapEatsColorScheme,
        typography = Typography,
        content = content
    )
}
