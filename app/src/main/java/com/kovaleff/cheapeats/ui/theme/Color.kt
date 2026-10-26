package com.kovaleff.cheapeats.ui.theme

import androidx.compose.ui.graphics.Color

// Брендовые цвета веб-версии (public/css/site.css)
val BgDark = Color(0xFF252627)        // --bg
val DarkInk = Color(0xFF0D0E10)       // --dark
val GreyPanel = Color(0xFF434349)     // --grey-bg (без альфы)
val GreyPanelSoft = Color(0xFF35363B)
val AccentLemon = Color(0xFFFCC202)   // --accent-lemon
val AccentLime = Color(0xFFDDF00D)    // --accent-lime
val BrandGreen = Color(0xFF22C55E)    // cheap
val BrandAmber = Color(0xFFF59E0B)    // moderate
val BrandRed = Color(0xFFEF4444)      // expensive
val BrandBrownLight = Color(0xFFB86412)
val TextLight = Color(0xFFF5F5F4)
val TextMuted = Color(0xFFA8A8A3)

// Цена уровня (как getPriceLevelColor в вебе)
fun priceLevelColor(level: String?): Color = when (level) {
    "cheap" -> BrandGreen
    "moderate" -> BrandAmber
    "expensive" -> BrandRed
    else -> TextMuted
}
