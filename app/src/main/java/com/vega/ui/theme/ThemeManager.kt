package com.vega.ui.theme

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import com.vega.R

object ThemeManager {
    private const val PREFS_NAME = "vega_prefs"
    private const val KEY_THEME = "pref_theme"

    fun applyTheme(context: Context) {
        val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val theme = sharedPrefs.getString(KEY_THEME, "system") ?: "system"
        val mode = when (theme) {
            "light" -> AppCompatDelegate.MODE_NIGHT_NO
            "dark" -> AppCompatDelegate.MODE_NIGHT_YES
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        AppCompatDelegate.setDefaultNightMode(mode)
    }

    fun setTheme(context: Context, theme: String) {
        val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sharedPrefs.edit().putString(KEY_THEME, theme).apply()
        applyTheme(context)
    }

    fun getTheme(context: Context): String {
        val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return sharedPrefs.getString(KEY_THEME, "system") ?: "system"
    }

    // --- Token Helpers ---
    fun accentBlue(context: Context) = ContextCompat.getColor(context, R.color.vega_accent_blue)
    fun accentGreen(context: Context) = ContextCompat.getColor(context, R.color.vega_accent_green)
    fun accentAmber(context: Context) = ContextCompat.getColor(context, R.color.vega_accent_amber)
    fun accentRed(context: Context) = ContextCompat.getColor(context, R.color.vega_accent_red)
    fun accentViolet(context: Context) = ContextCompat.getColor(context, R.color.vega_accent_violet)
    
    fun textPrimary(context: Context) = ContextCompat.getColor(context, R.color.vega_on_background)
    fun textSecondary(context: Context) = ContextCompat.getColor(context, R.color.vega_on_surface)
    fun textTertiary(context: Context) = ContextCompat.getColor(context, R.color.vega_on_surface_muted)
    
    fun surface(context: Context) = ContextCompat.getColor(context, R.color.vega_surface)
    fun surfaceElevated(context: Context) = ContextCompat.getColor(context, R.color.vega_surface_elevated)
    fun border(context: Context) = ContextCompat.getColor(context, R.color.vega_border)
    fun primary(context: Context) = ContextCompat.getColor(context, R.color.vega_primary)
}
