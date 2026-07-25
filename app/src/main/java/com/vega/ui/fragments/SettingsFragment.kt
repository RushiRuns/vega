package com.vega.ui.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.vega.R
import com.vega.alarms.TaskAlarmScheduler
import com.vega.databinding.FragmentSettingsBinding
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SettingsFragment : Fragment() {

    @Inject
    lateinit var scheduler: TaskAlarmScheduler

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

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
        updateNotificationModeUI(currentMode)

        binding.cardNotificationMode.setOnClickListener {
            val isExpanded = binding.layoutOptionsNotificationMode.visibility == View.VISIBLE
            binding.layoutOptionsNotificationMode.visibility = if (isExpanded) View.GONE else View.VISIBLE
            binding.ivChevronNotificationMode.setImageResource(
                if (isExpanded) R.drawable.ic_chevron_down else R.drawable.ic_chevron_up
            )
        }

        binding.optionNotifTaskSpecific.setOnClickListener {
            selectNotificationMode("task_specific")
        }

        binding.optionNotifRitual.setOnClickListener {
            selectNotificationMode("fixed_ritual")
        }
    }

    private fun selectNotificationMode(mode: String) {
        val prevMode = prefs.getString("pref_notification_mode", "task_specific")
        if (prevMode != mode) {
            prefs.edit().putString("pref_notification_mode", mode).apply()
            scheduler.rescheduleAllAlarms()
        }
        updateNotificationModeUI(mode)
        // Auto-collapse options after selection
        binding.layoutOptionsNotificationMode.visibility = View.GONE
        binding.ivChevronNotificationMode.setImageResource(R.drawable.ic_chevron_down)
    }

    private fun updateNotificationModeUI(mode: String) {
        val isTaskSpecific = mode == "task_specific"
        binding.tvNotificationModeValue.text = if (isTaskSpecific) "Task-Specific Alarms" else "Fixed Daily Rituals"

        binding.optionNotifTaskSpecific.setBackgroundResource(
            if (isTaskSpecific) R.drawable.bg_setting_option_selected else R.drawable.bg_setting_card
        )
        binding.ivCheckTaskSpecific.visibility = if (isTaskSpecific) View.VISIBLE else View.GONE

        binding.optionNotifRitual.setBackgroundResource(
            if (!isTaskSpecific) R.drawable.bg_setting_option_selected else R.drawable.bg_setting_card
        )
        binding.ivCheckRitual.visibility = if (!isTaskSpecific) View.VISIBLE else View.GONE
    }

    private fun setupAlarmOffsetSection() {
        val currentOffset = prefs.getInt("pref_alarm_offset_minutes", 0)
        updateAlarmOffsetUI(currentOffset)

        binding.cardAlarmOffset.setOnClickListener {
            val isExpanded = binding.layoutOptionsAlarmOffset.visibility == View.VISIBLE
            binding.layoutOptionsAlarmOffset.visibility = if (isExpanded) View.GONE else View.VISIBLE
            binding.ivChevronAlarmOffset.setImageResource(
                if (isExpanded) R.drawable.ic_chevron_down else R.drawable.ic_chevron_up
            )
        }

        binding.optionOffset0.setOnClickListener { selectAlarmOffset(0) }
        binding.optionOffset5.setOnClickListener { selectAlarmOffset(5) }
        binding.optionOffset15.setOnClickListener { selectAlarmOffset(15) }
        binding.optionOffset60.setOnClickListener { selectAlarmOffset(60) }
        binding.optionOffset1440.setOnClickListener { selectAlarmOffset(1440) }
    }

    private fun selectAlarmOffset(offsetMinutes: Int) {
        val prevOffset = prefs.getInt("pref_alarm_offset_minutes", 0)
        if (prevOffset != offsetMinutes) {
            prefs.edit().putInt("pref_alarm_offset_minutes", offsetMinutes).apply()
            scheduler.rescheduleAllAlarms()
        }
        updateAlarmOffsetUI(offsetMinutes)
        // Auto-collapse options after selection
        binding.layoutOptionsAlarmOffset.visibility = View.GONE
        binding.ivChevronAlarmOffset.setImageResource(R.drawable.ic_chevron_down)
    }

    private fun updateAlarmOffsetUI(selectedOffset: Int) {
        val labelMap = mapOf(
            0 to "At due time",
            5 to "5 minutes before",
            15 to "15 minutes before",
            60 to "1 hour before",
            1440 to "1 day before"
        )
        binding.tvAlarmOffsetValue.text = labelMap[selectedOffset] ?: "At due time"

        val orangeColor = ContextCompat.getColor(requireContext(), R.color.vega_primary)
        val mutedColor = ContextCompat.getColor(requireContext(), R.color.vega_on_surface_muted)

        val options = listOf(
            Triple(0, binding.optionOffset0, Pair(binding.ivIconOffset0, binding.ivCheckOffset0)),
            Triple(5, binding.optionOffset5, Pair(binding.ivIconOffset5, binding.ivCheckOffset5)),
            Triple(15, binding.optionOffset15, Pair(binding.ivIconOffset15, binding.ivCheckOffset15)),
            Triple(60, binding.optionOffset60, Pair(binding.ivIconOffset60, binding.ivCheckOffset60)),
            Triple(1440, binding.optionOffset1440, Pair(binding.ivIconOffset1440, binding.ivCheckOffset1440))
        )

        for ((offset, layout, icons) in options) {
            val isSelected = offset == selectedOffset
            layout.setBackgroundResource(
                if (isSelected) R.drawable.bg_setting_option_selected else R.drawable.bg_setting_card
            )
            icons.first.setColorFilter(if (isSelected) orangeColor else mutedColor)
            icons.second.visibility = if (isSelected) View.VISIBLE else View.GONE
        }
    }

    private fun setupThemeSection() {
        binding.tvThemeValue.text = "Dark Mode (Vega)"
        binding.cardTheme.setOnClickListener {
            val isExpanded = binding.layoutOptionsTheme.visibility == View.VISIBLE
            binding.layoutOptionsTheme.visibility = if (isExpanded) View.GONE else View.VISIBLE
            binding.ivChevronTheme.setImageResource(
                if (isExpanded) R.drawable.ic_chevron_down else R.drawable.ic_chevron_up
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
