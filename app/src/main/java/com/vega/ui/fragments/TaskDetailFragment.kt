package com.vega.ui.fragments

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar
import com.vega.R
import com.vega.data.database.TaskPriority
import com.vega.data.database.TaskState
import com.vega.databinding.BottomSheetTaskDetailBinding
import com.vega.ui.viewmodels.TaskDetailViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@AndroidEntryPoint
class TaskDetailFragment : BottomSheetDialogFragment() {

    private var _binding: BottomSheetTaskDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TaskDetailViewModel by viewModels()
    private var taskId: String? = null
    private var selectedDueDate: Long? = null
    private var selectedRecurrence: String? = null
    private var selectedPriority: TaskPriority = TaskPriority.NONE
    private var selectedState: TaskState = TaskState.INBOX

    override fun getTheme(): Int = R.style.Style_Vega_BottomSheet

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        taskId = arguments?.getString(ARG_TASK_ID)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetTaskDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (taskId == null) {
            dismiss()
            return
        }

        setupUI()
        observeViewModel()

        viewModel.loadTask(taskId!!)
    }

    private fun setupUI() {
        // Manage tags trigger
        binding.btnManageTagsDetail.setOnClickListener {
            closeAllDropdowns()
            TagManagementDialogFragment().show(parentFragmentManager, TagManagementDialogFragment.TAG)
        }

        // Save button in top header
        binding.btnSave.setOnClickListener {
            closeAllDropdowns()
            val title = binding.etTitle.text?.toString().orEmpty()
            val notes = binding.etNotes.text?.toString()

            viewModel.saveTask(
                title = title,
                dueDate = selectedDueDate,
                priority = selectedPriority,
                state = selectedState,
                recurrence = selectedRecurrence,
                notes = notes,
                selectedTagIds = getSelectedTagIds()
            )
        }

        // Tap outside area closes all dropdowns
        binding.layoutRootContainer.setOnClickListener { closeAllDropdowns() }
        binding.scrollRoot.setOnClickListener { closeAllDropdowns() }
        binding.etTitle.setOnFocusChangeListener { _, hasFocus -> if (hasFocus) closeAllDropdowns() }
        binding.etNotes.setOnFocusChangeListener { _, hasFocus -> if (hasFocus) closeAllDropdowns() }

        // Due Date Picker & Clear
        binding.cardDueDate.setOnClickListener {
            closeAllDropdowns()
            showDatePicker()
        }
        binding.btnClearDueDate.setOnClickListener {
            closeAllDropdowns()
            selectedDueDate = null
            binding.tvDueDateValue.text = "No Date Assigned"
            binding.btnClearDueDate.visibility = View.GONE
            updateStateBasedOnDueDate()
        }

        // Priority Dropdown Toggles
        binding.cardPriority.setOnClickListener {
            val isExpanded = binding.layoutDropdownPriority.visibility == View.VISIBLE
            closeAllDropdowns()
            if (!isExpanded) {
                refreshPriorityDropdownUI()
                binding.layoutDropdownPriority.visibility = View.VISIBLE
                binding.ivChevronPriority.setImageResource(R.drawable.ic_chevron_up)
            }
        }
        binding.optionPriorityNone.setOnClickListener { updatePriority(TaskPriority.NONE) }
        binding.optionPriorityHigh.setOnClickListener { updatePriority(TaskPriority.HIGH) }
        binding.optionPriorityMedium.setOnClickListener { updatePriority(TaskPriority.MEDIUM) }
        binding.optionPriorityLow.setOnClickListener { updatePriority(TaskPriority.LOW) }

        // State Dropdown Toggles
        binding.cardState.setOnClickListener {
            val isExpanded = binding.layoutDropdownState.visibility == View.VISIBLE
            closeAllDropdowns()
            if (!isExpanded) {
                refreshStateDropdownUI()
                binding.layoutDropdownState.visibility = View.VISIBLE
                binding.ivChevronState.setImageResource(R.drawable.ic_chevron_up)
            }
        }
        binding.optionStateInbox.setOnClickListener { updateState(TaskState.INBOX) }
        binding.optionStateToday.setOnClickListener { updateState(TaskState.TODAY) }
        binding.optionStateUpcoming.setOnClickListener { updateState(TaskState.UPCOMING) }
        binding.optionStateDone.setOnClickListener { updateState(TaskState.DONE) }

        // Recurrence Dropdown Toggles
        binding.cardRecurrence.setOnClickListener {
            val isExpanded = binding.layoutDropdownRecurrence.visibility == View.VISIBLE
            closeAllDropdowns()
            if (!isExpanded) {
                refreshRecurrenceDropdownUI()
                binding.layoutDropdownRecurrence.visibility = View.VISIBLE
                binding.ivChevronRecurrence.setImageResource(R.drawable.ic_chevron_up)
            }
        }
        binding.optionRecurrenceNone.setOnClickListener { updateRecurrence(null) }
        binding.optionRecurrenceDaily.setOnClickListener { updateRecurrence("DAILY") }
        binding.optionRecurrenceWeekly.setOnClickListener { updateRecurrence("WEEKLY") }
        binding.optionRecurrenceMonthly.setOnClickListener { updateRecurrence("MONTHLY") }
        binding.optionRecurrenceCustom.setOnClickListener {
            closeAllDropdowns()
            showRepeatsDialog()
        }

        // Set Fragment Result Listener for Custom Recurrence
        parentFragmentManager.setFragmentResultListener(
            RepeatsDialogFragment.REQUEST_KEY_RECURRENCE,
            viewLifecycleOwner
        ) { _, bundle ->
            val ruleJson = bundle.getString(RepeatsDialogFragment.RESULT_KEY_RULE_JSON)
            selectedRecurrence = ruleJson
            updateRecurrenceText(ruleJson)
        }
    }

    private fun closeAllDropdowns() {
        binding.layoutDropdownPriority.visibility = View.GONE
        binding.ivChevronPriority.setImageResource(R.drawable.ic_chevron_down)

        binding.layoutDropdownState.visibility = View.GONE
        binding.ivChevronState.setImageResource(R.drawable.ic_chevron_down)

        binding.layoutDropdownRecurrence.visibility = View.GONE
        binding.ivChevronRecurrence.setImageResource(R.drawable.ic_chevron_down)
    }

    private fun refreshPriorityDropdownUI() {
        val options = listOf(
            Triple(TaskPriority.NONE, binding.optionPriorityNone, binding.ivCheckPriorityNone),
            Triple(TaskPriority.HIGH, binding.optionPriorityHigh, binding.ivCheckPriorityHigh),
            Triple(TaskPriority.MEDIUM, binding.optionPriorityMedium, binding.ivCheckPriorityMedium),
            Triple(TaskPriority.LOW, binding.optionPriorityLow, binding.ivCheckPriorityLow)
        )
        for (opt in options) {
            val isSelected = opt.first == selectedPriority
            opt.second.setBackgroundResource(
                if (isSelected) R.drawable.bg_setting_option_selected else R.drawable.bg_setting_card
            )
            opt.third.visibility = if (isSelected) View.VISIBLE else View.GONE
        }
    }

    private fun updatePriority(priority: TaskPriority) {
        selectedPriority = priority
        closeAllDropdowns()

        val (text, colorHex) = when (priority) {
            TaskPriority.HIGH -> "High" to "#E94560"
            TaskPriority.MEDIUM -> "Medium" to "#F2A65A"
            TaskPriority.LOW -> "Low" to "#3171C6"
            else -> "None" to "#8E9096"
        }

        binding.tvPriorityValue.text = text
        binding.tvPriorityValue.setTextColor(Color.parseColor(colorHex))
        binding.ivPriorityDot.backgroundTintList = ColorStateList.valueOf(Color.parseColor(colorHex))
    }

    private fun refreshStateDropdownUI() {
        val options = listOf(
            Triple(TaskState.INBOX, binding.optionStateInbox, binding.ivCheckStateInbox),
            Triple(TaskState.TODAY, binding.optionStateToday, binding.ivCheckStateToday),
            Triple(TaskState.UPCOMING, binding.optionStateUpcoming, binding.ivCheckStateUpcoming),
            Triple(TaskState.DONE, binding.optionStateDone, binding.ivCheckStateDone)
        )
        for (opt in options) {
            val isSelected = opt.first == selectedState
            opt.second.setBackgroundResource(
                if (isSelected) R.drawable.bg_setting_option_selected else R.drawable.bg_setting_card
            )
            opt.third.visibility = if (isSelected) View.VISIBLE else View.GONE
        }
    }

    private fun updateState(state: TaskState) {
        selectedState = state
        closeAllDropdowns()

        val (text, iconRes, colorHex) = when (state) {
            TaskState.TODAY -> Triple("Today", R.drawable.ic_nav_today, "#F2A65A")
            TaskState.UPCOMING -> Triple("Upcoming", R.drawable.ic_nav_upcoming, "#A259FF")
            TaskState.DONE -> Triple("Done", R.drawable.ic_check, "#4ECDC4")
            else -> Triple("Inbox", R.drawable.ic_nav_inbox, "#3171C6")
        }

        binding.tvStateValue.text = text
        binding.ivStateIcon.setImageResource(iconRes)
        binding.ivStateIcon.setColorFilter(Color.parseColor(colorHex))
    }

    private fun refreshRecurrenceDropdownUI() {
        val isCustom = !selectedRecurrence.isNullOrBlank() &&
                selectedRecurrence != "DAILY" && selectedRecurrence != "WEEKLY" && selectedRecurrence != "MONTHLY"

        val options = listOf(
            Triple(null, binding.optionRecurrenceNone, binding.ivCheckRecurrenceNone),
            Triple("DAILY", binding.optionRecurrenceDaily, binding.ivCheckRecurrenceDaily),
            Triple("WEEKLY", binding.optionRecurrenceWeekly, binding.ivCheckRecurrenceWeekly),
            Triple("MONTHLY", binding.optionRecurrenceMonthly, binding.ivCheckRecurrenceMonthly),
            Triple("CUSTOM", binding.optionRecurrenceCustom, binding.ivCheckRecurrenceCustom)
        )

        for (opt in options) {
            val isSelected = if (opt.first == "CUSTOM") isCustom else (opt.first == selectedRecurrence)
            opt.second.setBackgroundResource(
                if (isSelected) R.drawable.bg_setting_option_selected else R.drawable.bg_setting_card
            )
            opt.third.visibility = if (isSelected) View.VISIBLE else View.GONE
        }
    }

    private fun updateRecurrence(ruleStr: String?) {
        selectedRecurrence = ruleStr
        closeAllDropdowns()
        updateRecurrenceText(ruleStr)
    }

    private fun updateRecurrenceText(ruleStr: String?) {
        val label = when (ruleStr?.uppercase()) {
            "DAILY" -> "Every Day"
            "WEEKLY" -> "Every Week"
            "MONTHLY" -> "Every Month"
            null, "" -> "None"
            else -> com.vega.utils.RecurrenceUtils.formatSummary(requireContext(), ruleStr)
        }
        binding.tvRecurrenceValue.text = label
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        selectedDueDate?.let { calendar.timeInMillis = it }

        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                showTimePicker(calendar)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }

    private fun showTimePicker(calendar: Calendar) {
        val timePickerDialog = TimePickerDialog(
            requireContext(),
            { _, hourOfDay, minute ->
                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                calendar.set(Calendar.MINUTE, minute)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)

                selectedDueDate = calendar.timeInMillis
                binding.tvDueDateValue.text = formatDueDate(selectedDueDate!!)
                binding.btnClearDueDate.visibility = View.VISIBLE
                updateStateBasedOnDueDate()
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            false
        )
        timePickerDialog.show()
    }

    private fun showRepeatsDialog() {
        val dialog = RepeatsDialogFragment.newInstance(selectedRecurrence, selectedDueDate)
        dialog.show(parentFragmentManager, RepeatsDialogFragment.TAG)
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Observe Task details
                launch {
                    viewModel.task.collect { task ->
                        task?.let {
                            binding.etTitle.setText(it.title)
                            binding.etNotes.setText(it.notes.orEmpty())

                            selectedDueDate = it.dueDate
                            if (it.dueDate != null) {
                                binding.tvDueDateValue.text = formatDueDate(it.dueDate)
                                binding.btnClearDueDate.visibility = View.VISIBLE
                            } else {
                                binding.tvDueDateValue.text = "No Date Assigned"
                                binding.btnClearDueDate.visibility = View.GONE
                            }

                            selectedRecurrence = it.recurrence
                            updateRecurrenceText(it.recurrence)

                            val prio = try { TaskPriority.valueOf(it.priority) } catch (e: Exception) { TaskPriority.NONE }
                            updatePriority(prio)

                            val st = try { TaskState.valueOf(it.state) } catch (e: Exception) { TaskState.INBOX }
                            updateState(st)
                        }
                    }
                }

                // Observe task tags
                launch {
                    kotlinx.coroutines.flow.combine(
                        viewModel.allTags,
                        viewModel.taskTags
                    ) { allTags, taskTags ->
                        Pair(allTags, taskTags)
                    }.collect { (allTags, taskTags) ->
                        populateDetailTagChips(allTags, taskTags)
                    }
                }

                // Observe Save Success
                launch {
                    viewModel.saveSuccess.collect { success ->
                        if (success) {
                            Toast.makeText(requireContext(), "Task saved", Toast.LENGTH_SHORT).show()
                            dismiss()
                        }
                    }
                }

                // Observe Error
                launch {
                    viewModel.error.collect { errorMsg ->
                        errorMsg?.let {
                            Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
                            viewModel.clearError()
                        }
                    }
                }
            }
        }
    }

    private fun populateDetailTagChips(allTags: List<com.vega.data.database.Tag>, assignedTags: List<com.vega.data.database.Tag>) {
        val currentlySelectedIds = getSelectedTagIds().ifEmpty { assignedTags.map { it.id }.toSet() }
        binding.chipGroupDetailTags.removeAllViews()
        allTags.forEach { tagItem ->
            val chip = Chip(requireContext()).apply {
                id = View.generateViewId()
                setTag(tagItem.id)
                text = tagItem.name
                isCheckable = true
                isChecked = currentlySelectedIds.contains(tagItem.id)

                val (chipBg, chipBorder, textColorHex) = if (isChecked) {
                    Triple("#1B283A", "#3171C6", "#3171C6")
                } else {
                    Triple("#1C1D22", "#2A2C34", "#8E9096")
                }

                chipBackgroundColor = ColorStateList.valueOf(Color.parseColor(chipBg))
                chipStrokeColor = ColorStateList.valueOf(Color.parseColor(chipBorder))
                chipStrokeWidth = 1.5f.dpToPx()
                setTextColor(Color.parseColor(textColorHex))
                chipCornerRadius = 50f
                chipMinHeight = 28f.dpToPx()

                setOnClickListener { closeAllDropdowns() }
            }
            binding.chipGroupDetailTags.addView(chip)
        }
    }

    private fun getSelectedTagIds(): List<String> {
        val list = mutableListOf<String>()
        for (i in 0 until binding.chipGroupDetailTags.childCount) {
            val chip = binding.chipGroupDetailTags.getChildAt(i) as? Chip
            if (chip != null && chip.isChecked && chip.tag != null) {
                list.add(chip.tag.toString())
            }
        }
        return list
    }

    private fun Float.dpToPx(): Float = this * resources.displayMetrics.density

    private fun formatDueDate(timestamp: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        val hasTime = cal.get(Calendar.HOUR_OF_DAY) != 0 || cal.get(Calendar.MINUTE) != 0
        val pattern = if (hasTime) "MMM d 'at' h:mm a" else "MMM d, yyyy"
        return SimpleDateFormat(pattern, Locale.getDefault()).format(cal.time)
    }

    private fun updateStateBasedOnDueDate() {
        if (!selectedRecurrence.isNullOrBlank()) return
        val dueDate = selectedDueDate
        if (dueDate == null) {
            updateState(TaskState.INBOX)
            return
        }

        val todayEnd = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis

        val targetState = if (dueDate <= todayEnd) TaskState.TODAY else TaskState.UPCOMING
        updateState(targetState)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "TaskDetailFragment"
        private const val ARG_TASK_ID = "arg_task_id"

        fun newInstance(taskId: String): TaskDetailFragment {
            return TaskDetailFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_TASK_ID, taskId)
                }
            }
        }
    }
}
