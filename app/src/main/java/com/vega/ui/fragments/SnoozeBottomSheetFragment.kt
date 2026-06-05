package com.vega.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.vega.R
import com.vega.data.database.Task
import com.vega.data.database.TaskPriority
import com.vega.databinding.FragmentSnoozeBottomSheetBinding
import com.vega.ui.viewmodels.SnoozeViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

import com.google.android.material.snackbar.Snackbar

@AndroidEntryPoint
class SnoozeBottomSheetFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentSnoozeBottomSheetBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SnoozeViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSnoozeBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        observeViewModel()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.currentTask.collect { task ->
                        if (task == null) {
                            dismiss()
                        } else {
                            bindTask(task)
                        }
                    }
                }

                launch {
                    viewModel.error.collect { message ->
                        if (message != null) {
                            Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
                            viewModel.clearError()
                        }
                    }
                }
            }
        }
    }

    private fun bindTask(task: Task) {
        binding.tvSnoozeTaskTitle.text = task.title

        // Bind due date
        if (task.dueDate != null) {
            binding.chipSnoozeDueDate.text = formatDueDate(task.dueDate)
            binding.chipSnoozeDueDate.visibility = View.VISIBLE
        } else {
            binding.chipSnoozeDueDate.visibility = View.GONE
        }

        // Bind priority
        val priority = try {
            TaskPriority.valueOf(task.priority)
        } catch (e: Exception) {
            TaskPriority.NONE
        }

        if (priority != TaskPriority.NONE) {
            val context = requireContext()
            val (colorRes, textRes) = when (priority) {
                TaskPriority.HIGH -> Pair(R.color.error, R.string.priority_high)
                TaskPriority.MEDIUM -> Pair(R.color.secondary, R.string.priority_medium)
                TaskPriority.LOW -> Pair(R.color.primary, R.string.priority_low)
                else -> Pair(android.R.color.transparent, R.string.priority_none)
            }
            binding.chipSnoozePriority.text = context.getString(textRes)
            binding.chipSnoozePriority.setChipBackgroundColorResource(colorRes)
            binding.chipSnoozePriority.visibility = View.VISIBLE
        } else {
            binding.chipSnoozePriority.visibility = View.GONE
        }

        // Action click listeners
        binding.btnSnoozeMoveTomorrow.setOnClickListener {
            viewModel.moveToTomorrow(task)
        }
        binding.btnSnoozeKeepToday.setOnClickListener {
            viewModel.keepInToday(task)
        }
        binding.btnSnoozeMarkDone.setOnClickListener {
            viewModel.markAsDone(task)
        }
        binding.btnSnoozeDelete.setOnClickListener {
            viewModel.deleteTask(task)
        }
    }

    private fun formatDueDate(timestamp: Long): String {
        val calendar = Calendar.getInstance().apply { timeInMillis = timestamp }
        val hasTime = calendar.get(Calendar.HOUR_OF_DAY) != 0 || calendar.get(Calendar.MINUTE) != 0
        val pattern = if (hasTime) "MMM d 'at' h:mm a" else "MMM d"
        return SimpleDateFormat(pattern, Locale.getDefault()).format(calendar.time)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(): SnoozeBottomSheetFragment {
            return SnoozeBottomSheetFragment()
        }
    }
}
