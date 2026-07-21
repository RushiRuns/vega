package com.vega.ui.fragments

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
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
        // Cancel button
        binding.btnCancel.setOnClickListener {
            dismiss()
        }

        // Manage tags trigger
        binding.btnManageTagsDetail.setOnClickListener {
            TagManagementDialogFragment().show(parentFragmentManager, TagManagementDialogFragment.TAG)
        }

        // Save button
        binding.btnSave.setOnClickListener {
            val title = binding.etTitle.text?.toString().orEmpty()
            val notes = binding.etNotes.text?.toString()

            val priorityOptions = getPriorityOptions()
            val stateOptions = getStateOptions()

            val priorityStr = binding.actvPriority.text.toString()
            val stateStr = binding.actvState.text.toString()

            val selectedPriority = priorityOptions.firstOrNull { it.first == priorityStr }?.second ?: TaskPriority.NONE
            val selectedState = stateOptions.firstOrNull { it.first == stateStr }?.second ?: TaskState.INBOX

            viewModel.saveTask(title, selectedDueDate, selectedPriority, selectedState, selectedRecurrence, notes, getSelectedTagIds())
        }

        // Date Picker Trigger
        binding.etDueDate.setOnClickListener {
            showDatePicker()
        }

        // End icon for Due Date TextInputLayout (Clear button)
        binding.tilDueDate.setEndIconOnClickListener {
            selectedDueDate = null
            binding.etDueDate.setText("")
            updateStateBasedOnDueDate()
        }

        // Recurrence Trigger
        binding.etRecurrence.setOnClickListener {
            showRepeatsDialog()
        }

        // End icon for Recurrence TextInputLayout (Clear button)
        binding.tilRecurrence.setEndIconOnClickListener {
            selectedRecurrence = null
            binding.etRecurrence.setText(com.vega.utils.RecurrenceUtils.formatSummary(requireContext(), null))
        }

        // Set Fragment Result Listener
        parentFragmentManager.setFragmentResultListener(
            RepeatsDialogFragment.REQUEST_KEY_RECURRENCE,
            viewLifecycleOwner
        ) { _, bundle ->
            val ruleJson = bundle.getString(RepeatsDialogFragment.RESULT_KEY_RULE_JSON)
            selectedRecurrence = ruleJson
            binding.etRecurrence.setText(com.vega.utils.RecurrenceUtils.formatSummary(requireContext(), ruleJson))
        }
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
                binding.etDueDate.setText(formatDueDate(selectedDueDate!!))
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
                                binding.etDueDate.setText(formatDueDate(it.dueDate))
                            } else {
                                binding.etDueDate.setText("")
                            }

                            selectedRecurrence = it.recurrence
                            binding.etRecurrence.setText(com.vega.utils.RecurrenceUtils.formatSummary(requireContext(), it.recurrence))

                            setupDropdowns(it.priority, it.state)
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

    private fun setupDropdowns(currentPriority: String, currentState: String) {
        val priorityOptions = getPriorityOptions()
        val stateOptions = getStateOptions()

        // Setup Priority dropdown
        val priorityAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            priorityOptions.map { it.first }
        )
        binding.actvPriority.setAdapter(priorityAdapter)
        val selectedPriorityName = try {
            TaskPriority.valueOf(currentPriority)
        } catch (e: Exception) {
            TaskPriority.NONE
        }
        val currentPriorityText = priorityOptions.firstOrNull { it.second == selectedPriorityName }?.first.orEmpty()
        binding.actvPriority.setText(currentPriorityText, false)

        // Setup State dropdown
        val stateAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            stateOptions.map { it.first }
        )
        binding.actvState.setAdapter(stateAdapter)
        val selectedStateName = try {
            TaskState.valueOf(currentState)
        } catch (e: Exception) {
            TaskState.INBOX
        }
        val currentStateText = stateOptions.firstOrNull { it.second == selectedStateName }?.first.orEmpty()
        binding.actvState.setText(currentStateText, false)
    }

    private fun getPriorityOptions() = listOf(
        getString(R.string.priority_none) to TaskPriority.NONE,
        getString(R.string.priority_low) to TaskPriority.LOW,
        getString(R.string.priority_medium) to TaskPriority.MEDIUM,
        getString(R.string.priority_high) to TaskPriority.HIGH
    )

    private fun getStateOptions() = listOf(
        getString(R.string.state_inbox) to TaskState.INBOX,
        getString(R.string.state_today) to TaskState.TODAY,
        getString(R.string.state_upcoming) to TaskState.UPCOMING,
        getString(R.string.state_done) to TaskState.DONE
    )

    private fun showRepeatsDialog() {
        val dialog = RepeatsDialogFragment.newInstance(selectedRecurrence, selectedDueDate)
        dialog.show(parentFragmentManager, RepeatsDialogFragment.TAG)
    }

    private fun populateDetailTagChips(allTags: List<com.vega.data.database.Tag>, assignedTags: List<com.vega.data.database.Tag>) {
        val currentlySelectedIds = getSelectedTagIds().ifEmpty { assignedTags.map { it.id }.toSet() }
        binding.chipGroupDetailTags.removeAllViews()
        allTags.forEach { tagItem ->
            val chip = com.google.android.material.chip.Chip(requireContext(), null, com.google.android.material.R.style.Widget_MaterialComponents_Chip_Filter).apply {
                id = View.generateViewId()
                setTag(tagItem.id)
                text = tagItem.name
                isCheckable = true
                isChecked = currentlySelectedIds.contains(tagItem.id)
                chipCornerRadius = 50f
                chipMinHeight = 24f.dpToPx()
            }
            binding.chipGroupDetailTags.addView(chip)
        }
    }

    private fun getSelectedTagIds(): List<String> {
        val list = mutableListOf<String>()
        for (i in 0 until binding.chipGroupDetailTags.childCount) {
            val chip = binding.chipGroupDetailTags.getChildAt(i) as? com.google.android.material.chip.Chip
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
        val pattern = if (hasTime) "MMM d, yyyy 'at' h:mm a" else "MMM d, yyyy"
        return SimpleDateFormat(pattern, Locale.getDefault()).format(cal.time)
    }

    private fun updateStateBasedOnDueDate() {
        if (!selectedRecurrence.isNullOrBlank()) {
            return
        }
        val dueDate = selectedDueDate
        val stateOptions = getStateOptions()
        if (dueDate == null) {
            val inboxText = stateOptions.firstOrNull { it.second == TaskState.INBOX }?.first.orEmpty()
            binding.actvState.setText(inboxText, false)
            return
        }

        val todayEnd = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis

        val targetState = if (dueDate <= todayEnd) {
            TaskState.TODAY
        } else {
            TaskState.UPCOMING
        }

        val stateText = stateOptions.firstOrNull { it.second == targetState }?.first.orEmpty()
        binding.actvState.setText(stateText, false)
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
