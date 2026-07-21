package com.vega.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.vega.R
import com.vega.data.database.Task
import com.vega.databinding.FragmentUpcomingBinding
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import android.graphics.Canvas
import com.vega.ui.adapters.UpcomingTasksAdapter
import com.vega.ui.models.UpcomingListItem
import com.vega.ui.viewmodels.UpcomingViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import java.util.Calendar

import com.google.android.material.snackbar.Snackbar

@AndroidEntryPoint
class UpcomingFragment : Fragment() {

    private var _binding: FragmentUpcomingBinding? = null
    private val binding get() = _binding!!

    private val viewModel: UpcomingViewModel by viewModels()
    private lateinit var adapter: UpcomingTasksAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUpcomingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observeViewModel()
        setupMultiSelectActions()
    }

    private fun setupRecyclerView() {
        adapter = UpcomingTasksAdapter(
            onCompleteClick = { task ->
                viewModel.completeTask(task)
            },
            onPostponeClick = { task ->
                viewModel.postponeTask(task)
            },
            onDeleteClick = { task ->
                showDeleteConfirmation(task)
            },
            onItemClick = { task ->
                if (adapter.isSelectionMode()) {
                    adapter.toggleSelection(task.id)
                } else {
                    showTaskDetailSheet(task.id)
                }
            },
            onItemLongClick = { task ->
                if (adapter.isSelectionMode()) {
                    adapter.toggleSelection(task.id)
                } else {
                    adapter.enterSelectionMode(task.id)
                }
            }
        )
        binding.rvUpcomingTasks.layoutManager = LinearLayoutManager(requireContext())
        binding.rvUpcomingTasks.adapter = adapter
        setupSwipeGestures()
    }

    private fun showDeleteConfirmation(task: Task) {
        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Task")
            .setMessage("Are you sure you want to delete this task?")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteTask(task)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showTaskDetailSheet(taskId: String) {
        TaskDetailFragment.newInstance(taskId).show(childFragmentManager, TaskDetailFragment.TAG)
    }

    private fun setupSwipeGestures() {
        val swipeHandler = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun getMovementFlags(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ): Int {
                val position = viewHolder.adapterPosition
                if (position == RecyclerView.NO_POSITION) return makeMovementFlags(0, 0)
                val item = adapter.currentList[position]
                if (item is UpcomingListItem.Header) {
                    return makeMovementFlags(0, 0)
                }
                return super.getMovementFlags(recyclerView, viewHolder)
            }

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                if (position == RecyclerView.NO_POSITION) return
                val item = adapter.currentList[position]
                if (item !is UpcomingListItem.TaskItem) return
                val task = item.task
                if (direction == ItemTouchHelper.RIGHT) {
                    if (adapter.isActionsRevealed(task.id)) {
                        adapter.hideTaskActions(task.id)
                    } else {
                        viewModel.completeTask(task)
                    }
                } else if (direction == ItemTouchHelper.LEFT) {
                    adapter.revealTaskActions(task.id)
                }
            }

            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                    val position = viewHolder.adapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        val item = adapter.currentList[position]
                        if (item !is UpcomingListItem.TaskItem) {
                            super.onChildDraw(c, recyclerView, viewHolder, 0f, dY, actionState, isCurrentlyActive)
                            return
                        }
                        if (dX < 0) {
                            super.onChildDraw(c, recyclerView, viewHolder, 0f, dY, actionState, isCurrentlyActive)
                        } else {
                            val alpha = 1.0f - dX / viewHolder.itemView.width
                            viewHolder.itemView.alpha = alpha
                            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
                        }
                    } else {
                        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
                    }
                } else {
                    super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
                }
            }

            override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                super.clearView(recyclerView, viewHolder)
                viewHolder.itemView.alpha = 1.0f
            }
        }
        val itemTouchHelper = ItemTouchHelper(swipeHandler)
        itemTouchHelper.attachToRecyclerView(binding.rvUpcomingTasks)
    }

    private var hasAnimatedListEntrance = false

    private fun runEntranceStaggerAnimation(recyclerView: RecyclerView) {
        if (hasAnimatedListEntrance) return
        hasAnimatedListEntrance = true

        recyclerView.post {
            val childCount = recyclerView.childCount
            for (i in 0 until minOf(childCount, 5)) {
                val child = recyclerView.getChildAt(i) ?: continue
                child.translationY = 60f
                child.alpha = 0f
                child.animate()
                    .translationY(0f)
                    .alpha(1f)
                    .setStartDelay(i * 30L)
                    .setDuration(150)
                    .setInterpolator(android.view.animation.DecelerateInterpolator())
                    .start()
            }
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.upcomingTasks.collect { tasks ->
                        val isInitialEmission = !hasAnimatedListEntrance && tasks.isNotEmpty()
                        adapter.submitList(tasks) {
                            if (isInitialEmission) {
                                runEntranceStaggerAnimation(binding.rvUpcomingTasks)
                            }
                        }
                        if (tasks.isEmpty()) {
                            binding.layoutEmptyState.visibility = View.VISIBLE
                            binding.rvUpcomingTasks.visibility = View.GONE
                        } else {
                            binding.layoutEmptyState.visibility = View.GONE
                            binding.rvUpcomingTasks.visibility = View.VISIBLE
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

    private fun setupMultiSelectActions() {
        adapter.setOnSelectionChangedListener { count ->
            val bar = binding.layoutMultiSelect.cardMultiSelectActions
            if (count > 0) {
                if (bar.visibility != View.VISIBLE) {
                    bar.visibility = View.VISIBLE
                    bar.translationY = 100f
                    bar.alpha = 0f
                    bar.animate()
                        .translationY(0f)
                        .alpha(1f)
                        .setDuration(200)
                        .setInterpolator(androidx.interpolator.view.animation.FastOutSlowInInterpolator())
                        .start()
                }
                binding.layoutMultiSelect.tvSelectedCount.text = "$count selected"
            } else {
                if (bar.visibility == View.VISIBLE) {
                    bar.animate()
                        .translationY(100f)
                        .alpha(0f)
                        .setDuration(150)
                        .withEndAction { bar.visibility = View.GONE }
                        .start()
                }
            }
        }

        binding.layoutMultiSelect.btnClearSelection.setOnClickListener {
            adapter.exitSelectionMode()
        }

        binding.layoutMultiSelect.btnActionDueDate.setOnClickListener {
            showBulkDueDateDialog(adapter.getSelectedTaskIds())
        }

        binding.layoutMultiSelect.btnActionPriority.setOnClickListener {
            showBulkPriorityDialog(adapter.getSelectedTaskIds())
        }

        binding.layoutMultiSelect.btnActionState.setOnClickListener {
            showBulkStateDialog(adapter.getSelectedTaskIds())
        }

        binding.layoutMultiSelect.btnActionRecurrence.setOnClickListener {
            showBulkRecurrenceDialog()
        }

        childFragmentManager.setFragmentResultListener(
            "bulk_recurrence_upcoming",
            viewLifecycleOwner
        ) { _, bundle ->
            val ruleJson = bundle.getString(RepeatsDialogFragment.RESULT_KEY_RULE_JSON)
            if (adapter.isSelectionMode()) {
                viewModel.updateMultipleTasks(
                    adapter.getSelectedTaskIds(),
                    recurrence = ruleJson,
                    recurrenceUpdated = true
                )
                adapter.exitSelectionMode()
            }
        }
    }

    private fun showBulkDueDateDialog(selectedTaskIds: List<String>) {
        val options = arrayOf("Today", "Tomorrow", "Choose Date & Time...", "Clear Due Date")
        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setTitle("Set Due Date")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> { // Today
                        val calendar = Calendar.getInstance().apply {
                            set(Calendar.HOUR_OF_DAY, 9)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        viewModel.updateMultipleTasks(selectedTaskIds, dueDate = calendar.timeInMillis, dueDateUpdated = true)
                        adapter.exitSelectionMode()
                    }
                    1 -> { // Tomorrow
                        val calendar = Calendar.getInstance().apply {
                            add(Calendar.DAY_OF_YEAR, 1)
                            set(Calendar.HOUR_OF_DAY, 9)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        viewModel.updateMultipleTasks(selectedTaskIds, dueDate = calendar.timeInMillis, dueDateUpdated = true)
                        adapter.exitSelectionMode()
                    }
                    2 -> { // Choose Date & Time
                        showBulkDatePicker(selectedTaskIds)
                    }
                    3 -> { // Clear Due Date
                        viewModel.updateMultipleTasks(selectedTaskIds, dueDate = null, dueDateUpdated = true)
                        adapter.exitSelectionMode()
                    }
                }
            }
            .show()
    }

    private fun showBulkDatePicker(selectedTaskIds: List<String>) {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                showBulkTimePicker(selectedTaskIds, calendar)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun showBulkTimePicker(selectedTaskIds: List<String>, calendar: Calendar) {
        TimePickerDialog(
            requireContext(),
            { _, hourOfDay, minute ->
                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                calendar.set(Calendar.MINUTE, minute)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)

                viewModel.updateMultipleTasks(selectedTaskIds, dueDate = calendar.timeInMillis, dueDateUpdated = true)
                adapter.exitSelectionMode()
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            false
        ).show()
    }

    private fun showBulkPriorityDialog(selectedTaskIds: List<String>) {
        val priorities = arrayOf("None", "Low", "Medium", "High")
        val priorityValues = arrayOf(
            com.vega.data.database.TaskPriority.NONE,
            com.vega.data.database.TaskPriority.LOW,
            com.vega.data.database.TaskPriority.MEDIUM,
            com.vega.data.database.TaskPriority.HIGH
        )
        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setTitle("Set Priority")
            .setItems(priorities) { _, which ->
                viewModel.updateMultipleTasks(selectedTaskIds, priority = priorityValues[which])
                adapter.exitSelectionMode()
            }
            .show()
    }

    private fun showBulkStateDialog(selectedTaskIds: List<String>) {
        val states = arrayOf("Inbox", "Today", "Upcoming", "Done")
        val stateValues = arrayOf(
            com.vega.data.database.TaskState.INBOX,
            com.vega.data.database.TaskState.TODAY,
            com.vega.data.database.TaskState.UPCOMING,
            com.vega.data.database.TaskState.DONE
        )
        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setTitle("Set State")
            .setItems(states) { _, which ->
                viewModel.updateMultipleTasks(selectedTaskIds, state = stateValues[which])
                adapter.exitSelectionMode()
            }
            .show()
    }

    private fun showBulkRecurrenceDialog() {
        val dialog = RepeatsDialogFragment.newInstance(null, null, "bulk_recurrence_upcoming")
        dialog.show(childFragmentManager, "BulkRepeatsDialog")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
