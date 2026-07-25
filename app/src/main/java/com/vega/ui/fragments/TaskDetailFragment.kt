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
import androidx.core.content.ContextCompat
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
        // Close and Cancel buttons
        binding.btnCloseSheet.setOnClickListener { dismiss() }
        binding.btnCancel.setOnClickListener { dismiss() }

        // Manage tags trigger
        binding.btnManageTagsDetail.setOnClickListener {
            TagManagementDialogFragment().show(parentFragmentManager, TagManagementDialogFragment.TAG)
        }

        // Save button
        binding.btnSave.setOnClickListener {
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

        // Due Date Picker & Clear
        binding.cardDueDate.setOnClickListener { showDatePicker() }
        binding.btnClearDueDate.setOnClickListener {
            selectedDueDate = null
            binding.tvDueDateValue.text = "No Date Assigned"
            binding.btnClearDueDate.visibility = View.GONE
            updateStateBasedOnDueDate()
        }

        // Priority Dropdown Toggles
        binding.cardPriority.setOnClickListener {
            val isExpanded = binding.layoutDropdownPriority.visibility == View.VISIBLE
            binding.layoutDropdownPriority.visibility = if (isExpanded) View.GONE else View.VISIBLE
            binding.ivChevronPriority.setImageResource(if (isExpanded) R.drawable.ic_chevron_down else R.drawable.ic_chevron_up)
        }
        binding.optionPriorityNone.setOnClickListener { updatePriority(TaskPriority.NONE) }
        binding.optionPriorityHigh.setOnClickListener { updatePriority(TaskPriority.HIGH) }
        binding.optionPriorityMedium.setOnClickListener { updatePriority(TaskPriority.MEDIUM) }
        binding.optionPriorityLow.setOnClickListener { updatePriority(TaskPriority.LOW) }

        // State Dropdown Toggles
        binding.cardState.setOnClickListener {
            val isExpanded = binding.layoutDropdownState.visibility == View.VISIBLE
            binding.layoutDropdownState.visibility = if (isExpanded) View.GONE else View.VISIBLE
            binding.ivChevronState.setImageResource(if (isExpanded) R.drawable.ic_chevron_down else R.drawable.ic_chevron_up)
        }
        binding.optionStateInbox.setOnClickListener { updateState(TaskState.INBOX) }
        binding.optionStateToday.setOnClickListener { updateState(TaskState.TODAY) }
        binding.optionStateUpcoming.setOnClickListener { updateState(TaskState.UPCOMING) }
        binding.optionStateDone.setOnClickListener { updateState(TaskState.DONE) }

        // Recurrence Dropdown Toggles
        binding.cardRecurrence.setOnClickListener {
            val isExpanded = binding.layoutDropdownRecurrence.visibility == View.VISIBLE
            binding.layoutDropdownRecurrence.visibility = if (isExpanded) View.GONE else View.VISIBLE
            binding.ivChevronRecurrence.setImageResource(if (isExpanded) R.drawable.ic_chevron_down else R.drawable.ic_chevron_up)
        }
        binding.optionRecurrenceNone.setOnClickListener { updateRecurrence(null) }
        binding.optionRecurrenceDaily.setOnClickListener { updateRecurrence("DAILY") }
        binding.optionRecurrenceWeekly.setOnClickListener { updateRecurrence("WEEKLY") }
        binding.optionRecurrenceMonthly.setOnClickListener { updateRecurrence("MONTHLY") }
    }

    private fun updatePriority(priority: TaskPriority) {
        selectedPriority = priority
        binding.layoutDropdownPriority.visibility = View.GONE
        binding.ivChevronPriority.setImageResource(R.drawable.ic_chevron_down)

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

    private fun updateState(state: TaskState) {
        selectedState = state
        binding.layoutDropdownState.visibility = View.GONE
        binding.ivChevronState.setImageResource(R.drawable.ic_chevron_down)

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

    private fun updateRecurrence(ruleStr: String?) {
        selectedRecurrence = ruleStr
        binding.layoutDropdownRecurrence.visibility = View.GONE
        binding.ivChevronRecurrence.setImageResource(R.drawable.ic_chevron_down)

        val label = when (ruleStr?.uppercase()) {
            "DAILY" -> "Every Day"
            "WEEKLY" -> "Every Week"
            "MONTHLY" -> "Every Month"
            else -> "None"
        }
        binding.tvRecurrenceValue.text = label

        binding.optionRecurrenceNone.setBackgroundResource(if (ruleStr == null) R.drawable.bg_setting_option_selected else R.drawable.bg_setting_card)
        binding.optionRecurrenceDaily.setBackgroundResource(if (ruleStr == "DAILY") R.drawable.bg_setting_option_selected else R.drawable.bg_setting_card)
        binding.optionRecurrenceWeekly.setBackgroundResource(if (ruleStr == "WEEKLY") R.drawable.bg_setting_option_selected else R.drawable.bg_setting_card)
        binding.optionRecurrenceMonthly.setBackgroundResource(if (ruleStr == "MONTHLY") R.drawable.bg_setting_option_selected else R.drawable.bg_setting_card)
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
                            updateRecurrence(it.recurrence)

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

                setOnCheckedChangeListener { _, _ ->
                    populateDetailTagChips(allTags, assignedTags)
                }
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
