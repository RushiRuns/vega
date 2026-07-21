package com.vega.ui.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import com.vega.R
import com.vega.alarms.TaskAlarmScheduler
import com.vega.databinding.FragmentSettingsBinding
import com.vega.ui.theme.ThemeManager
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

        setupSwitches()
        setupThemeDropdown()
        setupNotificationModeDropdown()
        setupAlarmOffsetDropdown()
    }

    private fun setupSwitches() {
        binding.switchEndOfDay.isChecked = prefs.getBoolean("pref_end_of_day_reminders", true)
        binding.switchDailyReminder.isChecked = prefs.getBoolean("pref_daily_reminders", true)
        binding.switchWeeklyReview.isChecked = prefs.getBoolean("pref_weekly_review", true)

        binding.switchEndOfDay.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("pref_end_of_day_reminders", isChecked).apply()
        }

        binding.switchDailyReminder.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("pref_daily_reminders", isChecked).apply()
        }

        binding.switchWeeklyReview.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("pref_weekly_review", isChecked).apply()
        }
    }

    private fun setupNotificationModeDropdown() {
        val modeOptions = listOf(
            getString(R.string.notification_mode_ritual) to "fixed_ritual",
            getString(R.string.notification_mode_task_specific) to "task_specific"
        )

        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            modeOptions.map { it.first }
        )
        binding.actvNotificationMode.setAdapter(adapter)

        val currentMode = prefs.getString("pref_notification_mode", "fixed_ritual")
        val currentModeLabel = modeOptions.firstOrNull { it.second == currentMode }?.first.orEmpty()
        binding.actvNotificationMode.setText(currentModeLabel, false)

        updateVisibilityForMode(currentMode)

        binding.actvNotificationMode.setOnItemClickListener { _, _, position, _ ->
            val selectedOption = modeOptions[position]
            val prevMode = prefs.getString("pref_notification_mode", "fixed_ritual")
            if (prevMode != selectedOption.second) {
                prefs.edit().putString("pref_notification_mode", selectedOption.second).apply()
                updateVisibilityForMode(selectedOption.second)
                scheduler.rescheduleAllAlarms()
            }
        }
    }

    private fun updateVisibilityForMode(mode: String?) {
        if (mode == "task_specific") {
            binding.tilAlarmOffset.visibility = View.VISIBLE
            binding.layoutRitualSettings.visibility = View.GONE
        } else {
            binding.tilAlarmOffset.visibility = View.GONE
            binding.layoutRitualSettings.visibility = View.VISIBLE
        }
    }

    private fun setupAlarmOffsetDropdown() {
        val offsetOptions = listOf(
            getString(R.string.alarm_offset_0) to 0,
            getString(R.string.alarm_offset_5) to 5,
            getString(R.string.alarm_offset_10) to 10,
            getString(R.string.alarm_offset_15) to 15,
            getString(R.string.alarm_offset_30) to 30
        )

        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            offsetOptions.map { it.first }
        )
        binding.actvAlarmOffset.setAdapter(adapter)

        val currentOffset = prefs.getInt("pref_alarm_offset_minutes", 5)
        val currentOffsetLabel = offsetOptions.firstOrNull { it.second == currentOffset }?.first.orEmpty()
        binding.actvAlarmOffset.setText(currentOffsetLabel, false)

        binding.actvAlarmOffset.setOnItemClickListener { _, _, position, _ ->
            val selectedOption = offsetOptions[position]
            val prevOffset = prefs.getInt("pref_alarm_offset_minutes", 5)
            if (prevOffset != selectedOption.second) {
                prefs.edit().putInt("pref_alarm_offset_minutes", selectedOption.second).apply()
                scheduler.rescheduleAllAlarms()
            }
        }
    }

    private fun setupThemeDropdown() {
        binding.actvTheme.setText("Dark Mode (Vega Canvas)")
        binding.tilTheme.isEnabled = false
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
