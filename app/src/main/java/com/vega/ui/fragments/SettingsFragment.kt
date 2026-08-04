package com.vega.ui.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.PopupWindow
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.vega.R
import com.vega.alarms.TaskAlarmScheduler
import com.vega.databinding.FragmentSettingsBinding
import com.vega.databinding.PopupAlarmOffsetBinding
import com.vega.databinding.PopupNotificationModeBinding
import com.vega.databinding.PopupThemeBinding
import com.vega.ui.theme.ThemeManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SettingsFragment : Fragment() {

    @Inject
    lateinit var scheduler: TaskAlarmScheduler

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private var notifModePopup: PopupWindow? = null
    private var alarmOffsetPopup: PopupWindow? = null
    private var themePopup: PopupWindow? = null

    private val prefs by lazy {
        requireContext().getSharedPreferences("vega_prefs", Context.MODE_PRIVATE)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupNotificationModeSection()
        setupAlarmOffsetSection()
        setupThemeSection()
    }

    private fun setupNotificationModeSection() {
        val currentMode = prefs.getString("pref_notification_mode", "task_specific") ?: "task_specific"
        updateNotificationModeText(currentMode)

        binding.cardNotificationMode.setOnClickListener {
            if (notifModePopup?.isShowing == true) {
                dismissAllPopups()
            } else {
                dismissAllPopups()
                showNotificationModePopup()
            }
        }
    }

    private fun showNotificationModePopup() {
        val anchor = binding.cardNotificationMode
        val popupBinding = PopupNotificationModeBinding.inflate(LayoutInflater.from(requireContext()))
        val currentMode = prefs.getString("pref_notification_mode", "task_specific") ?: "task_specific"
        val isTaskSpecific = currentMode == "task_specific"

        popupBinding.optionNotifTaskSpecific.setBackgroundResource(
            if (isTaskSpecific) R.drawable.bg_setting_option_selected else R.drawable.bg_setting_card
        )
        popupBinding.ivCheckTaskSpecific.visibility = if (isTaskSpecific) View.VISIBLE else View.GONE

        popupBinding.optionNotifRitual.setBackgroundResource(
            if (!isTaskSpecific) R.drawable.bg_setting_option_selected else R.drawable.bg_setting_card
        )
        popupBinding.ivCheckRitual.visibility = if (!isTaskSpecific) View.VISIBLE else View.GONE

        val popup = buildPopup(popupBinding.root, anchor.width)
        notifModePopup = popup

        popupBinding.optionNotifTaskSpecific.setOnClickListener {
            selectNotificationMode("task_specific")
            dismissAllPopups()
        }
        popupBinding.optionNotifRitual.setOnClickListener {
            selectNotificationMode("fixed_ritual")
            dismissAllPopups()
        }

        showPopupBelow(popup, anchor)
        binding.ivChevronNotificationMode.setImageResource(R.drawable.ic_chevron_up)
    }

    private fun selectNotificationMode(mode: String) {
        val prevMode = prefs.getString("pref_notification_mode", "task_specific")
        if (prevMode != mode) {
            prefs.edit().putString("pref_notification_mode", mode).apply()
            scheduler.rescheduleAllAlarms()
        }
        updateNotificationModeText(mode)
    }

    private fun updateNotificationModeText(mode: String) {
        val isTaskSpecific = mode == "task_specific"
        binding.tvNotificationModeValue.text = if (isTaskSpecific) "Task-Specific Alarms" else "Fixed Daily Rituals"
    }

    private fun setupAlarmOffsetSection() {
        val currentOffset = prefs.getInt("pref_alarm_offset_minutes", 0)
        updateAlarmOffsetText(currentOffset)

        binding.cardAlarmOffset.setOnClickListener {
            if (alarmOffsetPopup?.isShowing == true) {
                dismissAllPopups()
            } else {
                dismissAllPopups()
                showAlarmOffsetPopup()
            }
        }
    }

    private fun showAlarmOffsetPopup() {
        val anchor = binding.cardAlarmOffset
        val popupBinding = PopupAlarmOffsetBinding.inflate(LayoutInflater.from(requireContext()))
        val selectedOffset = prefs.getInt("pref_alarm_offset_minutes", 0)

        val orangeColor = ContextCompat.getColor(requireContext(), R.color.vega_primary)
        val mutedColor = ContextCompat.getColor(requireContext(), R.color.vega_on_surface_muted)

        val options = listOf(
            Triple(0, popupBinding.optionOffset0, Pair(popupBinding.ivIconOffset0, popupBinding.ivCheckOffset0)),
            Triple(5, popupBinding.optionOffset5, Pair(popupBinding.ivIconOffset5, popupBinding.ivCheckOffset5)),
            Triple(15, popupBinding.optionOffset15, Pair(popupBinding.ivIconOffset15, popupBinding.ivCheckOffset15)),
            Triple(60, popupBinding.optionOffset60, Pair(popupBinding.ivIconOffset60, popupBinding.ivCheckOffset60)),
            Triple(1440, popupBinding.optionOffset1440, Pair(popupBinding.ivIconOffset1440, popupBinding.ivCheckOffset1440))
        )

        for ((offset, layout, icons) in options) {
            val isSelected = offset == selectedOffset
            layout.setBackgroundResource(
                if (isSelected) R.drawable.bg_setting_option_selected else R.drawable.bg_setting_card
            )
            icons.first.setColorFilter(if (isSelected) orangeColor else mutedColor)
            icons.second.visibility = if (isSelected) View.VISIBLE else View.GONE

            layout.setOnClickListener {
                selectAlarmOffset(offset)
                dismissAllPopups()
            }
        }

        val popup = buildPopup(popupBinding.root, anchor.width)
        alarmOffsetPopup = popup

        showPopupBelow(popup, anchor)
        binding.ivChevronAlarmOffset.setImageResource(R.drawable.ic_chevron_up)
    }

    private fun selectAlarmOffset(offsetMinutes: Int) {
        val prevOffset = prefs.getInt("pref_alarm_offset_minutes", 0)
        if (prevOffset != offsetMinutes) {
            prefs.edit().putInt("pref_alarm_offset_minutes", offsetMinutes).apply()
            scheduler.rescheduleAllAlarms()
        }
        updateAlarmOffsetText(offsetMinutes)
    }

    private fun updateAlarmOffsetText(selectedOffset: Int) {
        val labelMap = mapOf(
            0 to "At due time",
            5 to "5 minutes before",
            15 to "15 minutes before",
            60 to "1 hour before",
            1440 to "1 day before"
        )
        binding.tvAlarmOffsetValue.text = labelMap[selectedOffset] ?: "At due time"
    }

    private fun setupThemeSection() {
        updateThemeText(ThemeManager.getTheme(requireContext()))
        binding.cardTheme.setOnClickListener {
            if (themePopup?.isShowing == true) {
                dismissAllPopups()
            } else {
                dismissAllPopups()
                showThemePopup()
            }
        }
    }

    private fun showThemePopup() {
        val anchor = binding.cardTheme
        val popupBinding = PopupThemeBinding.inflate(LayoutInflater.from(requireContext()))
        
        val currentTheme = ThemeManager.getTheme(requireContext())
        
        val orangeColor = ContextCompat.getColor(requireContext(), R.color.vega_primary)
        val mutedColor = ContextCompat.getColor(requireContext(), R.color.vega_on_surface_muted)

        val options = listOf(
            Triple("system", popupBinding.optionThemeSystem, popupBinding.ivCheckSystem),
            Triple("dark", popupBinding.optionThemeDark, popupBinding.ivCheckDark),
            Triple("light", popupBinding.optionThemeLight, popupBinding.ivCheckLight)
        )

        for ((themeVal, layout, checkIcon) in options) {
            val isSelected = themeVal == currentTheme
            layout.setBackgroundResource(
                if (isSelected) R.drawable.bg_setting_option_selected else R.drawable.bg_setting_card
            )
            checkIcon.visibility = if (isSelected) View.VISIBLE else View.GONE
            
            layout.setOnClickListener {
                selectTheme(themeVal)
                dismissAllPopups()
            }
        }

        val popup = buildPopup(popupBinding.root, anchor.width)
        themePopup = popup

        showPopupBelow(popup, anchor)
        binding.ivChevronTheme.setImageResource(R.drawable.ic_chevron_up)
    }

    private fun selectTheme(theme: String) {
        ThemeManager.setTheme(requireContext(), theme)
        updateThemeText(theme)
    }

    private fun updateThemeText(theme: String) {
        val labelMap = mapOf(
            "system" to "System Default",
            "dark" to "Dark Mode",
            "light" to "Light Mode"
        )
        binding.tvThemeValue.text = labelMap[theme] ?: "System Default"
    }

    private fun buildPopup(contentView: View, widthPx: Int): PopupWindow {
        contentView.measure(
            View.MeasureSpec.makeMeasureSpec(widthPx, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        return PopupWindow(
            contentView,
            widthPx,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            true
        ).apply {
            isOutsideTouchable = true
            elevation = 24f
            setOnDismissListener { resetChevrons() }
        }
    }

    private fun showPopupBelow(popup: PopupWindow, anchor: View) {
        anchor.post {
            popup.showAsDropDown(anchor, 0, 0)
        }
    }

    private fun dismissAllPopups() {
        notifModePopup?.dismiss()
        alarmOffsetPopup?.dismiss()
        themePopup?.dismiss()
        notifModePopup = null
        alarmOffsetPopup = null
        themePopup = null
        resetChevrons()
    }

    private fun resetChevrons() {
        if (notifModePopup?.isShowing != true)
            binding.ivChevronNotificationMode.setImageResource(R.drawable.ic_chevron_down)
        if (alarmOffsetPopup?.isShowing != true)
            binding.ivChevronAlarmOffset.setImageResource(R.drawable.ic_chevron_down)
        if (themePopup?.isShowing != true)
            binding.ivChevronTheme.setImageResource(R.drawable.ic_chevron_down)
    }

    override fun onDestroyView() {
        dismissAllPopups()
        super.onDestroyView()
        _binding = null
    }
}
