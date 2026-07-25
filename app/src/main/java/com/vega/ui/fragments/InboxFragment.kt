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
import com.vega.databinding.FragmentInboxBinding
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import android.graphics.Canvas
import com.vega.ui.adapters.TaskListAdapter
import com.vega.ui.viewmodels.InboxViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import java.util.Calendar

import com.vega.ui.activities.MainActivity
import com.google.android.material.snackbar.Snackbar

@AndroidEntryPoint
class InboxFragment : Fragment() {

    private var _binding: FragmentInboxBinding? = null
    private val binding get() = _binding!!

    private val viewModel: InboxViewModel by viewModels()
    private lateinit var adapter: TaskListAdapter

    private var allInboxTasks: List<Task> = emptyList()
    private var currentFilterPill: String = "ALL"
    private var currentSearchQuery: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInboxBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupSearchAndFilterPills()
        observeViewModel()
        setupMultiSelectActions()
    }

    private fun setupRecyclerView() {
        adapter = TaskListAdapter(
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
            },
            itemLayoutRes = R.layout.item_task_today
        )
        binding.rvInboxTasks.layoutManager = LinearLayoutManager(requireContext())
        binding.rvInboxTasks.adapter = adapter
        binding.rvInboxTasks.isNestedScrollingEnabled = false
        setupSwipeGestures()
    }

    private fun setupSearchAndFilterPills() {
        binding.btnTriggerSelect.setOnClickListener {
            if (adapter.currentList.isNotEmpty()) {
                adapter.enterSelectionMode(adapter.currentList.first().id)
            }
        }

        binding.etSearchInbox.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                currentSearchQuery = s?.toString()?.trim().orEmpty()
                filterAndSubmitList()
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        binding.btnFilterAll.setOnClickListener {
            currentFilterPill = "ALL"
            updatePillStyles()
            filterAndSubmitList()
        }

        binding.btnFilterPriority.setOnClickListener {
            currentFilterPill = "PRIORITY"
            updatePillStyles()
            filterAndSubmitList()
        }

        binding.btnFilterTags.setOnClickListener {
            currentFilterPill = "TAGS"
            updatePillStyles()
            filterAndSubmitList()
        }
    }

    private fun updatePillStyles() {
        binding.btnFilterAll.setBackgroundResource(
            if (currentFilterPill == "ALL") R.drawable.bg_filter_pill_active else R.drawable.bg_filter_pill_inactive
        )
        binding.btnFilterPriority.setBackgroundResource(
            if (currentFilterPill == "PRIORITY") R.drawable.bg_filter_pill_active else R.drawable.bg_filter_pill_inactive
        )
        binding.btnFilterTags.setBackgroundResource(
            if (currentFilterPill == "TAGS") R.drawable.bg_filter_pill_active else R.drawable.bg_filter_pill_inactive
        )
    }

    private fun filterAndSubmitList() {
        var filtered = allInboxTasks
        if (currentSearchQuery.isNotBlank()) {
            filtered = filtered.filter { it.title.contains(currentSearchQuery, ignoreCase = true) }
        }
        when (currentFilterPill) {
            "PRIORITY" -> filtered = filtered.filter { it.priority != com.vega.data.database.TaskPriority.NONE.name }
            "TAGS" -> filtered = filtered.filter { task -> adapter.getTaskTagsMap()[task.id]?.isNotEmpty() == true }
        }
        adapter.submitList(filtered)
        binding.tvInboxSubtitle.text = "${filtered.size} tasks pending schedule"
        if (filtered.isEmpty()) {
            binding.layoutEmptyState.visibility = View.VISIBLE
            binding.rvInboxTasks.visibility = View.GONE
        } else {
            binding.layoutEmptyState.visibility = View.GONE
            binding.rvInboxTasks.visibility = View.VISIBLE
        }
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
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                if (position == RecyclerView.NO_POSITION) return
                val task = adapter.currentList[position]
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
                        val viewHolderToday = viewHolder as? TaskListAdapter.TaskViewHolder
                        viewHolderToday?.let {
                            if (dX > 0) {
                                it.itemView.translationX = 0f
                            } else {
                                it.itemView.translationX = dX
                            }
                        }
                    }
                } else {
                    super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
                }
            }
        }
        val itemTouchHelper = ItemTouchHelper(swipeHandler)
        itemTouchHelper.attachToRecyclerView(binding.rvInboxTasks)
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
                    viewModel.taskTagsMap.collect { map ->
                        adapter.setTaskTagsMap(map)
                    }
                }

                launch {
                    viewModel.inboxTasks.collect { tasks ->
                        allInboxTasks = tasks
                        val isInitialEmission = !hasAnimatedListEntrance && tasks.isNotEmpty()
                        filterAndSubmitList()
                        if (isInitialEmission && tasks.isNotEmpty()) {
                            runEntranceStaggerAnimation(binding.rvInboxTasks)
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
                // Show contextual selection top header & hide normal title layout
                binding.layoutNormalHeader.visibility = View.GONE
                binding.layoutSelectionHeader.visibility = View.VISIBLE
                binding.tvSelectedCount.text = "$count Selected"

                // Hide floating bottom bar in MainActivity so bottom sheet presents cleanly
                (activity as? MainActivity)?.setFloatingBottomBarVisible(false)

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
            } else {
                // Restore normal Inbox title layout & hide selection top header
                binding.layoutNormalHeader.visibility = View.VISIBLE
                binding.layoutSelectionHeader.visibility = View.GONE

                // Restore floating bottom bar in MainActivity
                (activity as? MainActivity)?.setFloatingBottomBarVisible(true)

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

        binding.btnClearSelection.setOnClickListener {
            adapter.exitSelectionMode()
        }

        binding.btnSelectAll.setOnClickListener {
            adapter.selectAll()
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
            showBulkRecurrenceDialog(adapter.getSelectedTaskIds())
        }
    }

    private fun showBulkDueDateDialog(taskIds: List<String>) {
        if (taskIds.isEmpty()) return
        val calendar = Calendar.getInstance()

        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                val selectedCalendar = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                }

                TimePickerDialog(
                    requireContext(),
                    { _, hourOfDay, minute ->
                        selectedCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                        selectedCalendar.set(Calendar.MINUTE, minute)
                        selectedCalendar.set(Calendar.SECOND, 0)

                        viewModel.updateMultipleTasks(
                            taskIds = taskIds,
                            dueDate = selectedCalendar.timeInMillis,
                            dueDateUpdated = true
                        )
                        adapter.exitSelectionMode()
                    },
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    false
                ).show()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).apply {
            setButton(DatePickerDialog.BUTTON_NEUTRAL, "Clear Date") { _, _ ->
                viewModel.updateMultipleTasks(
                    taskIds = taskIds,
                    dueDate = null,
                    dueDateUpdated = true
                )
                adapter.exitSelectionMode()
            }
        }.show()
    }

    private fun showBulkPriorityDialog(taskIds: List<String>) {
        if (taskIds.isEmpty()) return
        val popup = PopupMenu(requireContext(), binding.layoutMultiSelect.btnActionPriority)
        popup.menu.add(0, 0, 0, "None")
        popup.menu.add(0, 1, 1, "Low")
        popup.menu.add(0, 2, 2, "Medium")
        popup.menu.add(0, 3, 3, "High")

        popup.setOnMenuItemClickListener { item ->
            val priority = when (item.itemId) {
                1 -> com.vega.data.database.TaskPriority.LOW
                2 -> com.vega.data.database.TaskPriority.MEDIUM
                3 -> com.vega.data.database.TaskPriority.HIGH
                else -> com.vega.data.database.TaskPriority.NONE
            }
            viewModel.updateMultipleTasks(taskIds = taskIds, priority = priority)
            adapter.exitSelectionMode()
            true
        }
        popup.show()
    }

    private fun showBulkStateDialog(taskIds: List<String>) {
        if (taskIds.isEmpty()) return
        val popup = PopupMenu(requireContext(), binding.layoutMultiSelect.btnActionState)
        popup.menu.add(0, 0, 0, "Inbox")
        popup.menu.add(0, 1, 1, "Today")
        popup.menu.add(0, 2, 2, "Upcoming")
        popup.menu.add(0, 3, 3, "Done")

        popup.setOnMenuItemClickListener { item ->
            val state = when (item.itemId) {
                1 -> com.vega.data.database.TaskState.TODAY
                2 -> com.vega.data.database.TaskState.UPCOMING
                3 -> com.vega.data.database.TaskState.DONE
                else -> com.vega.data.database.TaskState.INBOX
            }
            viewModel.updateMultipleTasks(taskIds = taskIds, state = state)
            adapter.exitSelectionMode()
            true
        }
        popup.show()
    }

    private fun showBulkRecurrenceDialog(taskIds: List<String>) {
        if (taskIds.isEmpty()) return
        val popup = PopupMenu(requireContext(), binding.layoutMultiSelect.btnActionRecurrence)
        popup.menu.add(0, 0, 0, "None")
        popup.menu.add(0, 1, 1, "Daily")
        popup.menu.add(0, 2, 2, "Weekly")
        popup.menu.add(0, 3, 3, "Monthly")

        popup.setOnMenuItemClickListener { item ->
            val recurrenceStr = when (item.itemId) {
                1 -> "DAILY"
                2 -> "WEEKLY"
                3 -> "MONTHLY"
                else -> null
            }
            viewModel.updateMultipleTasks(
                taskIds = taskIds,
                recurrence = recurrenceStr,
                recurrenceUpdated = true
            )
            adapter.exitSelectionMode()
            true
        }
        popup.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
