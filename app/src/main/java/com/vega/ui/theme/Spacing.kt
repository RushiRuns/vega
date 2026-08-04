package com.vega.ui.theme

import android.content.res.Resources
import android.util.TypedValue

object Spacing {
    val displayMetrics = Resources.getSystem().displayMetrics

    val xxs = dpToPx(4f)
    val xs = dpToPx(8f)
    val sm = dpToPx(12f)
    val md = dpToPx(16f)
    val lg = dpToPx(20f)
    val xl = dpToPx(24f)
    val xxl = dpToPx(32f)
    val xxxl = dpToPx(48f)

    val screenHorizontal = lg
    val cardPadding = md
    val sectionGap = xl
    val itemGap = sm

    private fun dpToPx(dp: Float): Int {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, displayMetrics).toInt()
    }
}
