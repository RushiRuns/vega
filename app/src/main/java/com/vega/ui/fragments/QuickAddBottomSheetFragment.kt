package com.vega.ui.fragments

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.vega.R
import com.vega.data.database.TaskPriority
import com.vega.databinding.BottomSheetQuickAddBinding
import com.vega.ui.viewmodels.QuickAddUiState
import com.vega.ui.viewmodels.QuickAddViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@AndroidEntryPoint
class QuickAddBottomSheetFragment : BottomSheetDialogFragment() {

    private var _binding: BottomSheetQuickAddBinding? = null
    private val binding get() = _binding!!

    private val viewModel: QuickAddViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetQuickAddBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Reset view model state to make sure we start fresh
        viewModel.clearState()

        // Focus input and show keyboard
        binding.inputTask.requestFocus()
        binding.inputTask.postDelayed({
            val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(binding.inputTask, InputMethodManager.SHOW_IMPLICIT)
        }, 200)

        setupListeners()
        observeViewModel()
    }

    private fun setupListeners() {
        binding.inputTask.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                viewModel.parseInput(s?.toString() ?: "")
            }
        })

        binding.chipDatePreview.setOnCloseIconClickListener {
            viewModel.updateDueDate(null)
        }

        binding.chipPriorityPreview.setOnCloseIconClickListener {
            viewModel.updatePriority(TaskPriority.NONE)
        }

        binding.chipRecurrencePreview.setOnCloseIconClickListener {
            viewModel.updateRecurrence(null)
        }

        binding.buttonCancel.setOnClickListener {
            dismiss()
        }

        binding.buttonConfirm.setOnClickListener {
            val title = viewModel.parseResult.value.title
            if (title.isNullOrBlank()) {
                binding.layoutTaskInput.error = getString(R.string.error_empty_title)
            } else {
                viewModel.createTask()
                // Optimistic UI: dismiss immediately on confirmation
                dismiss()
            }
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.parseResult.collect { result ->
                        // Update Date Chip
                        if (result.dueDate != null) {
                            binding.chipDatePreview.text = formatDueDate(result.dueDate)
                            binding.chipDatePreview.visibility = View.VISIBLE
                        } else {
                            binding.chipDatePreview.visibility = View.GONE
                        }

                        // Update Priority Chip
                        if (result.priority != TaskPriority.NONE) {
                            binding.chipPriorityPreview.text = getPriorityString(result.priority)
                            binding.chipPriorityPreview.visibility = View.VISIBLE
                        } else {
                            binding.chipPriorityPreview.visibility = View.GONE
                        }

                        // Update Recurrence Chip
                        if (result.recurrence != null) {
                            binding.chipRecurrencePreview.text = getRecurrenceString(result.recurrence)
                            binding.chipRecurrencePreview.visibility = View.VISIBLE
                        } else {
                            binding.chipRecurrencePreview.visibility = View.GONE
                        }

                        // Update visibility of the container elements
                        val hasPreview = result.dueDate != null || result.priority != TaskPriority.NONE || result.recurrence != null
                        binding.textPreviewLabel.visibility = if (hasPreview) View.VISIBLE else View.GONE
                        binding.groupChipsPreview.visibility = if (hasPreview) View.VISIBLE else View.GONE
                    }
                }

                launch {
                    viewModel.uiState.collect { state ->
                        when (state) {
                            is QuickAddUiState.Error -> {
                                // Since we might have optimistically dismissed, showing Toast on application context
                                Toast.makeText(
                                    requireContext().applicationContext,
                                    state.message,
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            is QuickAddUiState.Success -> {
                                Toast.makeText(
                                    requireContext().applicationContext,
                                    "Task captured",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            else -> { /* No-op */ }
                        }
                    }
                }
            }
        }
    }

    private fun formatDueDate(timestamp: Long): String {
        val calendar = Calendar.getInstance().apply { timeInMillis = timestamp }
        val hasTime = calendar.get(Calendar.HOUR_OF_DAY) != 0 || calendar.get(Calendar.MINUTE) != 0
        
        return if (hasTime) {
            val sdf = SimpleDateFormat("EEE, MMM d 'at' h:mm a", Locale.getDefault())
            sdf.format(calendar.time)
        } else {
            val sdf = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
            sdf.format(calendar.time)
        }
    }

    private fun getPriorityString(priority: TaskPriority): String {
        return when (priority) {
            TaskPriority.LOW -> getString(R.string.priority_low)
            TaskPriority.MEDIUM -> getString(R.string.priority_medium)
            TaskPriority.HIGH -> getString(R.string.priority_high)
            else -> getString(R.string.priority_none)
        }
    }

    private fun getRecurrenceString(recurrence: String): String {
        return when (recurrence.uppercase()) {
            "DAILY" -> getString(R.string.recurrence_daily)
            "WEEKLY" -> getString(R.string.recurrence_weekly)
            "WEEKDAYS" -> getString(R.string.recurrence_weekdays)
            "MONTHLY" -> getString(R.string.recurrence_monthly)
            else -> recurrence
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onDismiss(dialog: android.content.DialogInterface) {
        super.onDismiss(dialog)
        (activity as? com.vega.ui.activities.QuickAddActivity)?.finish()
    }
}
