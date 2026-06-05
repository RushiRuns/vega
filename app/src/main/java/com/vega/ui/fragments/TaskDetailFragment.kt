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

        // Save button
        binding.btnSave.setOnClickListener {
            val title = binding.etTitle.text?.toString().orEmpty()
            val notes = binding.etNotes.text?.toString()

            val priorityOptions = getPriorityOptions()
            val stateOptions = getStateOptions()
            val recurrenceOptions = getRecurrenceOptions()

            val priorityStr = binding.actvPriority.text.toString()
            val stateStr = binding.actvState.text.toString()
            val recurrenceStr = binding.actvRecurrence.text.toString()

            val selectedPriority = priorityOptions.firstOrNull { it.first == priorityStr }?.second ?: TaskPriority.NONE
            val selectedState = stateOptions.firstOrNull { it.first == stateStr }?.second ?: TaskState.INBOX
            val selectedRecurrence = recurrenceOptions.firstOrNull { it.first == recurrenceStr }?.second

            viewModel.saveTask(title, selectedDueDate, selectedPriority, selectedState, selectedRecurrence, notes)
        }

        // Date Picker Trigger
        binding.etDueDate.setOnClickListener {
            showDatePicker()
        }

        // End icon for Due Date TextInputLayout (Clear button)
        binding.tilDueDate.setEndIconOnClickListener {
            selectedDueDate = null
            binding.etDueDate.setText("")
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

                            setupDropdowns(it.priority, it.state, it.recurrence)
                        }
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

    private fun setupDropdowns(currentPriority: String, currentState: String, currentRecurrence: String?) {
        val priorityOptions = getPriorityOptions()
        val stateOptions = getStateOptions()
        val recurrenceOptions = getRecurrenceOptions()

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

        // Setup Recurrence dropdown
        val recurrenceAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            recurrenceOptions.map { it.first }
        )
        binding.actvRecurrence.setAdapter(recurrenceAdapter)
        val currentRecurrenceText = recurrenceOptions.firstOrNull { it.second == currentRecurrence }?.first ?: getString(R.string.recurrence_none)
        binding.actvRecurrence.setText(currentRecurrenceText, false)
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

    private fun getRecurrenceOptions() = listOf(
        getString(R.string.recurrence_none) to null,
        getString(R.string.recurrence_daily) to "DAILY",
        getString(R.string.recurrence_weekdays) to "WEEKDAYS",
        getString(R.string.recurrence_weekly) to "WEEKLY",
        getString(R.string.recurrence_monthly) to "MONTHLY"
    )

    private fun formatDueDate(timestamp: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        val hasTime = cal.get(Calendar.HOUR_OF_DAY) != 0 || cal.get(Calendar.MINUTE) != 0
        val pattern = if (hasTime) "MMM d, yyyy 'at' h:mm a" else "MMM d, yyyy"
        return SimpleDateFormat(pattern, Locale.getDefault()).format(cal.time)
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
