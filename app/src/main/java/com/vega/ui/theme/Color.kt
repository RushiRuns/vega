package com.vega.ui.theme

import android.graphics.Color

data class AppColorScheme(
    val background: Int,
    val surface: Int,
    val surfaceVariant: Int,
    val onBackground: Int,
    val onSurface: Int,
    val onSurfaceVariant: Int,
    val primary: Int,
    val onPrimary: Int,
    val error: Int,
    val outline: Int
)

val AppDarkColorScheme = AppColorScheme(
    background      = Palette.Grey950,
    surface         = Palette.Grey900,
    surfaceVariant  = Palette.Grey700,
    onBackground    = Palette.White,
    onSurface       = Palette.White,
    onSurfaceVariant = Palette.Grey500,
    primary         = Palette.Blue500,
    onPrimary       = Palette.White,
    error           = Palette.Red500,
    outline         = Color.parseColor("#3A3A3C")
)

val AppLightColorScheme = AppColorScheme(
    background      = Palette.OffWhite,
    surface         = Palette.White,
    surfaceVariant  = Color.parseColor("#ECECEE"),
    onBackground    = Color.parseColor("#1C1C1E"),
    onSurface       = Color.parseColor("#1C1C1E"),
    onSurfaceVariant = Palette.Grey500,
    primary         = Palette.Blue500,
    onPrimary       = Palette.White,
    error           = Palette.Red500,
    outline         = Color.parseColor("#E0E0E3")
)
