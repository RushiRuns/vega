package com.vega.ui.views

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.vega.R
import com.vega.data.database.TaskPriority
import com.vega.data.database.TaskState
import com.vega.ui.theme.ThemeManager

class StatusPillView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val iconView = ImageView(context)
    private val textView = TextView(context)

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        
        val padHorizontal = dpToPx(8f).toInt()
        val padVertical = dpToPx(4f).toInt()
        setPadding(padHorizontal, padVertical, padHorizontal, padVertical)
        
        val minHeight = resources.getDimensionPixelSize(R.dimen.status_pill_height)
        minimumHeight = minHeight

        val iconSize = resources.getDimensionPixelSize(R.dimen.icon_size_small)
        val iconParams = LayoutParams(iconSize, iconSize).apply {
            marginEnd = dpToPx(4f).toInt()
        }
        iconView.layoutParams = iconParams
        iconView.visibility = GONE
        addView(iconView)

        textView.layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
        textView.typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        textView.includeFontPadding = false
        addView(textView)

        updateBackground(Color.TRANSPARENT)
    }

    fun setPriority(priority: TaskPriority) {
        iconView.visibility = GONE
        when (priority) {
            TaskPriority.HIGH -> {
                textView.text = "High"
                textView.setTextColor(ThemeManager.accentRed(context))
                updateBackground(ContextCompat.getColor(context, R.color.vega_status_error_bg))
                visibility = VISIBLE
            }
            TaskPriority.MEDIUM -> {
                textView.text = "Medium"
                textView.setTextColor(ThemeManager.accentAmber(context))
                updateBackground(ContextCompat.getColor(context, R.color.vega_status_warning_bg))
                visibility = VISIBLE
            }
            TaskPriority.LOW -> {
                textView.text = "Low"
                textView.setTextColor(ThemeManager.accentBlue(context))
                updateBackground(ThemeManager.surfaceElevated(context))
                visibility = VISIBLE
            }
            TaskPriority.NONE -> {
                visibility = GONE
            }
        }
    }

    fun setState(state: TaskState) {
        iconView.visibility = VISIBLE
        when (state) {
            TaskState.TODAY -> {
                textView.text = "Today"
                val color = ThemeManager.accentAmber(context)
                textView.setTextColor(color)
                iconView.setImageResource(R.drawable.ic_nav_today)
                iconView.imageTintList = ColorStateList.valueOf(color)
                updateBackground(ContextCompat.getColor(context, R.color.vega_status_warning_bg))
                visibility = VISIBLE
            }
            TaskState.UPCOMING -> {
                textView.text = "Upcoming"
                val color = ThemeManager.accentViolet(context)
                textView.setTextColor(color)
                iconView.setImageResource(R.drawable.ic_nav_upcoming)
                iconView.imageTintList = ColorStateList.valueOf(color)
                updateBackground(ThemeManager.surfaceElevated(context))
                visibility = VISIBLE
            }
            TaskState.DONE -> {
                textView.text = "Done"
                val color = ThemeManager.accentGreen(context)
                textView.setTextColor(color)
                iconView.setImageResource(R.drawable.ic_check) // Or another check icon
                iconView.imageTintList = ColorStateList.valueOf(color)
                updateBackground(ContextCompat.getColor(context, R.color.vega_status_success_bg))
                visibility = VISIBLE
            }
            TaskState.INBOX -> {
                textView.text = "Inbox"
                val color = ThemeManager.textSecondary(context)
                textView.setTextColor(color)
                iconView.setImageResource(R.drawable.ic_nav_inbox)
                iconView.imageTintList = ColorStateList.valueOf(color)
                updateBackground(ContextCompat.getColor(context, R.color.vega_status_neutral_bg))
                visibility = VISIBLE
            }
        }
    }
    
    fun setCustomTag(name: String, colorHex: String) {
        iconView.visibility = GONE
        textView.text = name
        
        val tagColor = try {
            Color.parseColor(colorHex)
        } catch (e: Exception) {
            ThemeManager.accentBlue(context)
        }
        
        textView.setTextColor(ThemeManager.textPrimary(context))
        updateBackground(tagColor)
        visibility = VISIBLE
    }

    private fun updateBackground(bgColor: Int) {
        val bg = GradientDrawable()
        bg.setColor(bgColor)
        bg.cornerRadius = dpToPx(999f) // Pill shape
        background = bg
    }

    private fun dpToPx(dp: Float): Float {
        return dp * resources.displayMetrics.density
    }
}
