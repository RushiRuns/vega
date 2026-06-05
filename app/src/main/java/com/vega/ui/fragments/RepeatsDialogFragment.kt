package com.vega.ui.fragments

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.DialogFragment
import com.vega.R
import com.vega.data.database.RecurrenceRule
import com.vega.databinding.DialogRepeatsBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class RepeatsDialogFragment : DialogFragment() {

    private var _binding: DialogRepeatsBinding? = null
    private val binding get() = _binding!!

    private var startDate: Long = System.currentTimeMillis()
    private var endDate: Long? = null
    private var selectedFrequency = "DAILY"

    // Mapping chip ID to Calendar day
    private val chipIdToWeekdayMap = mapOf(
        R.id.chip_sun to Calendar.SUNDAY,
        R.id.chip_mon to Calendar.MONDAY,
        R.id.chip_tue to Calendar.TUESDAY,
        R.id.chip_wed to Calendar.WEDNESDAY,
        R.id.chip_thu to Calendar.THURSDAY,
        R.id.chip_fri to Calendar.FRIDAY,
        R.id.chip_sat to Calendar.SATURDAY
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Style as full screen dialog
        setStyle(STYLE_NORMAL, R.style.Theme_Vega_FullScreenDialog)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogRepeatsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Close button
        binding.btnClose.setOnClickListener {
            dismiss()
        }

        // Done button
        binding.btnDone.setOnClickListener {
            val rule = buildRule()
            if (rule != null) {
                val bundle = Bundle().apply {
                    putString(RESULT_KEY_RULE_JSON, rule.toJson())
                }
                parentFragmentManager.setFragmentResult(REQUEST_KEY_RECURRENCE, bundle)
            } else {
                parentFragmentManager.setFragmentResult(REQUEST_KEY_RECURRENCE, Bundle())
            }
            dismiss()
        }

        // Spinner Frequency setup
        setupFrequencySpinner()

        // End Options Radio Group listener
        binding.rgEndOptions.setOnCheckedChangeListener { _, checkedId ->
            binding.tilEndDate.isEnabled = (checkedId == R.id.rb_end_on_date)
            binding.tilEndOccurrences.isEnabled = (checkedId == R.id.rb_end_after_occurrences)
        }

        // Click listeners for picking dates
        binding.etStartDate.setOnClickListener {
            showStartDatePicker()
        }
        binding.etEndDate.setOnClickListener {
            binding.rbEndOnDate.isChecked = true
            showEndDatePicker()
        }

        // Check RadioButton when typing occurrences
        binding.etEndOccurrences.setOnClickListener {
            binding.rbEndAfterOccurrences.isChecked = true
        }
        binding.etEndOccurrences.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                binding.rbEndAfterOccurrences.isChecked = true
            }
        }

        // Watcher on interval to update spinner plural/singular units dynamically
        binding.etInterval.doAfterTextChanged { editable ->
            val interval = editable?.toString()?.toIntOrNull() ?: 1
            updateSpinnerAdapter(interval)
        }

        // Read initial rule argument if exists
        val initialRuleJson = arguments?.getString(ARG_RULE_JSON)
        val initialTaskDueDate = arguments?.getLong(ARG_TASK_DUE_DATE)
        
        startDate = initialTaskDueDate ?: System.currentTimeMillis()
        
        val initialRule = initialRuleJson?.let { RecurrenceRule.fromJson(it) }
        populateUi(initialRule)
    }

    private fun setupFrequencySpinner() {
        // We'll initialize with DAILY
        updateSpinnerAdapter(1)

        binding.spinnerFrequency.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedFrequency = when (position) {
                    0 -> "DAILY"
                    1 -> "WEEKLY"
                    2 -> "MONTHLY"
                    3 -> "YEARLY"
                    else -> "DAILY"
                }
                updateConditionalViews()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun updateSpinnerAdapter(interval: Int) {
        val selectedPos = binding.spinnerFrequency.selectedItemPosition
        val options = if (interval <= 1) {
            listOf(getString(R.string.unit_day), getString(R.string.unit_week), getString(R.string.unit_month), getString(R.string.unit_year))
        } else {
            listOf(getString(R.string.unit_days), getString(R.string.unit_weeks), getString(R.string.unit_months), getString(R.string.unit_years))
        }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, options).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        binding.spinnerFrequency.adapter = adapter
        if (selectedPos >= 0 && selectedPos < options.size) {
            binding.spinnerFrequency.setSelection(selectedPos)
        }
    }

    private fun updateConditionalViews() {
        binding.layoutWeekdays.visibility = if (selectedFrequency == "WEEKLY") View.VISIBLE else View.GONE
        binding.layoutMonthly.visibility = if (selectedFrequency == "MONTHLY") View.VISIBLE else View.GONE
    }

    private fun updateMonthlyOptionStrings(startDateMillis: Long) {
        val calendar = Calendar.getInstance().apply { timeInMillis = startDateMillis }
        val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

        // Calculate occurrence count of this weekday (1st, 2nd, 3rd, 4th, last)
        val tempCal = calendar.clone() as Calendar
        tempCal.set(Calendar.DAY_OF_MONTH, 1)
        var count = 0
        while (tempCal.get(Calendar.MONTH) == calendar.get(Calendar.MONTH)) {
            if (tempCal.get(Calendar.DAY_OF_WEEK) == dayOfWeek) {
                count++
                if (tempCal.get(Calendar.DAY_OF_MONTH) == dayOfMonth) {
                    break
                }
            }
            tempCal.add(Calendar.DAY_OF_MONTH, 1)
        }

        tempCal.timeInMillis = calendar.timeInMillis
        tempCal.add(Calendar.DAY_OF_MONTH, 7)
        val isLast = tempCal.get(Calendar.MONTH) != calendar.get(Calendar.MONTH)

        val occurrenceStr = when {
            isLast -> "Last"
            count == 1 -> "First"
            count == 2 -> "Second"
            count == 3 -> "Third"
            count == 4 -> "Fourth"
            else -> "First"
        }

        val symbols = java.text.DateFormatSymbols.getInstance(Locale.getDefault())
        val dayOfWeekName = symbols.weekdays[dayOfWeek]

        binding.rbMonthlyDayOfMonth.text = getString(R.string.monthly_day_of_month, dayOfMonth)
        binding.rbMonthlyDayOfWeek.text = getString(R.string.monthly_relative_day, occurrenceStr, dayOfWeekName)
    }

    private fun showStartDatePicker() {
        val calendar = Calendar.getInstance().apply { timeInMillis = startDate }
        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                showStartTimePicker(calendar)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun showStartTimePicker(calendar: Calendar) {
        android.app.TimePickerDialog(
            requireContext(),
            { _, hourOfDay, minute ->
                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                calendar.set(Calendar.MINUTE, minute)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)

                startDate = calendar.timeInMillis
                binding.etStartDate.setText(formatDate(startDate))
                updateMonthlyOptionStrings(startDate)
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            false
        ).show()
    }

    private fun showEndDatePicker() {
        val calendar = Calendar.getInstance()
        endDate?.let { calendar.timeInMillis = it }
        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                endDate = calendar.timeInMillis
                binding.etEndDate.setText(formatDate(endDate!!))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun formatDate(timestamp: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        val hasTime = cal.get(Calendar.HOUR_OF_DAY) != 0 || cal.get(Calendar.MINUTE) != 0
        val pattern = if (hasTime) "MMM d, yyyy 'at' h:mm a" else "MMM d, yyyy"
        return SimpleDateFormat(pattern, Locale.getDefault()).format(cal.time)
    }

    private fun populateUi(rule: RecurrenceRule?) {
        // Set starts date
        binding.etStartDate.setText(formatDate(startDate))
        updateMonthlyOptionStrings(startDate)

        if (rule == null) {
            binding.etInterval.setText("1")
            binding.spinnerFrequency.setSelection(0) // DAILY
            binding.rbEndNever.isChecked = true
            return
        }

        // Set interval
        binding.etInterval.setText(rule.interval.toString())

        // Set frequency spinner
        val freqPos = when (rule.frequency.uppercase()) {
            "DAILY" -> 0
            "WEEKLY" -> 1
            "MONTHLY" -> 2
            "YEARLY" -> 3
            else -> 0
        }
        binding.spinnerFrequency.setSelection(freqPos)
        selectedFrequency = rule.frequency.uppercase()
        updateConditionalViews()

        // Set weekdays
        if (rule.frequency.uppercase() == "WEEKLY" && rule.weekdays != null) {
            rule.weekdays.forEach { day ->
                val chipId = chipIdToWeekdayMap.filterValues { it == day }.keys.firstOrNull()
                chipId?.let { id ->
                    binding.chipGroupWeekdays.findViewById<com.google.android.material.chip.Chip>(id)?.isChecked = true
                }
            }
        }

        // Set monthly selection
        if (rule.frequency.uppercase() == "MONTHLY") {
            if (rule.monthlyType == "DAY_OF_WEEK") {
                binding.rbMonthlyDayOfWeek.isChecked = true
            } else {
                binding.rbMonthlyDayOfMonth.isChecked = true
            }
        }

        // Set starts date (if rule has it)
        startDate = rule.startDate
        binding.etStartDate.setText(formatDate(startDate))
        updateMonthlyOptionStrings(startDate)

        // Set ends criteria
        when (rule.endType.uppercase()) {
            "NEVER" -> {
                binding.rbEndNever.isChecked = true
            }
            "ON_DATE" -> {
                binding.rbEndOnDate.isChecked = true
                rule.endDate?.let {
                    endDate = it
                    binding.etEndDate.setText(formatDate(it))
                }
            }
            "AFTER_OCCURRENCES" -> {
                binding.rbEndAfterOccurrences.isChecked = true
                rule.endOccurrences?.let {
                    binding.etEndOccurrences.setText(it.toString())
                }
            }
        }
    }

    private fun buildRule(): RecurrenceRule? {
        val interval = binding.etInterval.text?.toString()?.toIntOrNull() ?: 1
        
        val weekdays = if (selectedFrequency == "WEEKLY") {
            val list = mutableListOf<Int>()
            chipIdToWeekdayMap.forEach { (chipId, calendarDay) ->
                if (binding.chipGroupWeekdays.findViewById<com.google.android.material.chip.Chip>(chipId)?.isChecked == true) {
                    list.add(calendarDay)
                }
            }
            if (list.isEmpty()) null else list
        } else null

        var monthlyType: String? = null
        var dayOfMonth: Int? = null
        var dayOfWeekOccurrence: Int? = null
        var dayOfWeek: Int? = null

        if (selectedFrequency == "MONTHLY") {
            if (binding.rbMonthlyDayOfWeek.isChecked) {
                monthlyType = "DAY_OF_WEEK"
                val calendar = Calendar.getInstance().apply { timeInMillis = startDate }
                dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
                
                // Recalculate weekday occurrence count (1st, 2nd, etc.)
                val tempCal = calendar.clone() as Calendar
                tempCal.set(Calendar.DAY_OF_MONTH, 1)
                var count = 0
                while (tempCal.get(Calendar.MONTH) == calendar.get(Calendar.MONTH)) {
                    if (tempCal.get(Calendar.DAY_OF_WEEK) == dayOfWeek) {
                        count++
                        if (tempCal.get(Calendar.DAY_OF_MONTH) == calendar.get(Calendar.DAY_OF_MONTH)) {
                            break
                        }
                    }
                    tempCal.add(Calendar.DAY_OF_MONTH, 1)
                }

                tempCal.timeInMillis = calendar.timeInMillis
                tempCal.add(Calendar.DAY_OF_MONTH, 7)
                val isLast = tempCal.get(Calendar.MONTH) != calendar.get(Calendar.MONTH)
                
                dayOfWeekOccurrence = if (isLast) -1 else count
            } else {
                monthlyType = "DAY_OF_MONTH"
                val calendar = Calendar.getInstance().apply { timeInMillis = startDate }
                dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
            }
        }

        val endType = when {
            binding.rbEndNever.isChecked -> "NEVER"
            binding.rbEndOnDate.isChecked -> "ON_DATE"
            binding.rbEndAfterOccurrences.isChecked -> "AFTER_OCCURRENCES"
            else -> "NEVER"
        }

        val finalEndDate = if (endType == "ON_DATE") endDate else null
        val finalEndOccurrences = if (endType == "AFTER_OCCURRENCES") {
            binding.etEndOccurrences.text?.toString()?.toIntOrNull() ?: 10
        } else null

        return RecurrenceRule(
            frequency = selectedFrequency,
            interval = interval,
            weekdays = weekdays,
            monthlyType = monthlyType,
            dayOfMonth = dayOfMonth,
            dayOfWeekOccurrence = dayOfWeekOccurrence,
            dayOfWeek = dayOfWeek,
            startDate = startDate,
            endType = endType,
            endDate = finalEndDate,
            endOccurrences = finalEndOccurrences
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "RepeatsDialogFragment"
        const val REQUEST_KEY_RECURRENCE = "request_key_recurrence"
        const val RESULT_KEY_RULE_JSON = "result_key_rule_json"
        
        const val ARG_RULE_JSON = "arg_rule_json"
        const val ARG_TASK_DUE_DATE = "arg_task_due_date"

        fun newInstance(ruleJson: String?, taskDueDate: Long?): RepeatsDialogFragment {
            return RepeatsDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_RULE_JSON, ruleJson)
                    if (taskDueDate != null) {
                        putLong(ARG_TASK_DUE_DATE, taskDueDate)
                    }
                }
            }
        }
    }
}
