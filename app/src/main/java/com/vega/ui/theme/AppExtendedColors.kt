package com.vega.ui.theme

import androidx.core.graphics.ColorUtils

data class AppExtendedColors(
    val statusSuccess: Int,
    val statusWarning: Int,
    val statusActive: Int,
    val statusSuccessContainer: Int,
    val statusWarningContainer: Int
)

val extendedColors = AppExtendedColors(
    statusSuccess          = Palette.Green500,
    statusWarning          = Palette.Amber400,
    statusActive           = Palette.Green500,
    statusSuccessContainer = ColorUtils.setAlphaComponent(Palette.Green500, (255 * 0.15f).toInt()),
    statusWarningContainer = ColorUtils.setAlphaComponent(Palette.Amber400, (255 * 0.15f).toInt())
)
