package com.vega.ui.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import com.vega.R
import com.vega.databinding.FragmentSettingsBinding
import com.vega.ui.theme.ThemeManager

class SettingsFragment : Fragment() {

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

    private fun setupThemeDropdown() {
        val themeOptions = listOf(
            getString(R.string.theme_system) to "system",
            getString(R.string.theme_light) to "light",
            getString(R.string.theme_dark) to "dark"
        )

        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            themeOptions.map { it.first }
        )
        binding.actvTheme.setAdapter(adapter)

        val currentThemeValue = ThemeManager.getTheme(requireContext())
        val currentThemeLabel = themeOptions.firstOrNull { it.second == currentThemeValue }?.first.orEmpty()
        binding.actvTheme.setText(currentThemeLabel, false)

        binding.actvTheme.setOnItemClickListener { _, _, position, _ ->
            val selectedOption = themeOptions[position]
            ThemeManager.setTheme(requireContext(), selectedOption.second)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
