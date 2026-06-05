package com.vega.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.vega.R
import com.vega.databinding.FragmentTodayBinding
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import android.graphics.Canvas
import com.vega.data.database.Task
import com.vega.ui.adapters.TaskListAdapter
import com.vega.ui.viewmodels.TodayViewModel
import com.vega.data.database.TaskPriority
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@AndroidEntryPoint
class TodayFragment : Fragment() {

    private var _binding: FragmentTodayBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TodayViewModel by viewModels()
    private lateinit var adapter: TaskListAdapter
    private var previousTaskCount = -1

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTodayBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupListeners()
        observeViewModel()
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
            onItemLongClick = { task ->
                showTaskDetailSheet(task.id)
            }
        )
        binding.rvTodayTasks.layoutManager = LinearLayoutManager(requireContext())
        binding.rvTodayTasks.adapter = adapter
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
                        val task = adapter.currentList[position]
                        if (dX < 0) {
                            // Swiping left: do NOT translate the view.
                            super.onChildDraw(c, recyclerView, viewHolder, 0f, dY, actionState, isCurrentlyActive)
                        } else {
                            // Swiping right: slide standard, and apply fade effect
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
        itemTouchHelper.attachToRecyclerView(binding.rvTodayTasks)
    }

    private fun setupListeners() {
        binding.chipOverflowIndicator.setOnClickListener {
            viewModel.toggleExpand()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Observe Next Best Action (T037)
                launch {
                    viewModel.nextBestAction.collect { task ->
                        if (task != null) {
                            binding.cardNextBestAction.tvNbaTitle.text = task.title
                            
                            if (task.dueDate != null) {
                                binding.cardNextBestAction.tvNbaDueDate.text = formatDueDate(task.dueDate)
                                binding.cardNextBestAction.tvNbaDueDate.visibility = View.VISIBLE
                            } else {
                                binding.cardNextBestAction.tvNbaDueDate.visibility = View.GONE
                            }

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
                                binding.cardNextBestAction.chipNbaPriority.text = context.getString(textRes)
                                binding.cardNextBestAction.chipNbaPriority.setChipBackgroundColorResource(colorRes)
                                binding.cardNextBestAction.chipNbaPriority.visibility = View.VISIBLE
                            } else {
                                binding.cardNextBestAction.chipNbaPriority.visibility = View.GONE
                            }

                            binding.cardNextBestAction.btnNbaDismiss.setOnClickListener {
                                viewModel.dismissNextBestAction(task.id)
                            }

                            binding.cardNextBestAction.root.visibility = View.VISIBLE
                        } else {
                            binding.cardNextBestAction.root.visibility = View.GONE
                        }
                    }
                }
                // Observe today tasks
                launch {
                    viewModel.todayTasks.collect { tasks ->
                        adapter.submitList(tasks)
                        
                        // Handle Empty State
                        if (tasks.isEmpty()) {
                            binding.layoutEmptyState.visibility = View.VISIBLE
                            binding.rvTodayTasks.visibility = View.GONE
                        } else {
                            binding.layoutEmptyState.visibility = View.GONE
                            binding.rvTodayTasks.visibility = View.VISIBLE
                        }
                    }
                }

                // Observe overflow indicator & soft cap warnings
                launch {
                    launch {
                        viewModel.isExpanded.collect { isExpanded ->
                            updateOverflowChip(isExpanded, viewModel.overflowCount.value)
                        }
                    }

                    launch {
                        viewModel.overflowCount.collect { overflowCount ->
                            updateOverflowChip(viewModel.isExpanded.value, overflowCount)
                        }
                    }
                }

                // Observe total tasks size from DB for soft cap warning (T033)
                launch {
                    // Let's get the full list count to check for soft cap warning
                    // Wait, we can count total tasks from the TodayViewModel's combined flow
                    // Since allTodayTasks is private, we can observe total size by adding it or calculating it
                    // Wait, todayTasks size is capped at 7. If isExpanded is false, todayTasks size is 7, overflowCount is N.
                    // So total size = todayTasks.size + overflowCount
                    // Let's compute this dynamically!
                    viewModel.todayTasks.collect { tasks ->
                        val totalTasks = tasks.size + viewModel.overflowCount.value
                        if (totalTasks >= 8 && previousTaskCount >= 0 && previousTaskCount < 8) {
                            Snackbar.make(
                                binding.root,
                                getString(R.string.warning_too_many_today),
                                Snackbar.LENGTH_LONG
                            ).show()
                        }
                        previousTaskCount = totalTasks
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

    private fun updateOverflowChip(isExpanded: Boolean, overflowCount: Int) {
        if (isExpanded) {
            binding.chipOverflowIndicator.text = getString(R.string.button_cancel) // reuse Cancel for collapse or "Show less"
            // Wait, cancel string is "Cancel". We can use a direct text or customize. Let's set it to "Show less" manually
            binding.chipOverflowIndicator.text = "Show less"
            binding.chipOverflowIndicator.visibility = View.VISIBLE
        } else {
            if (overflowCount > 0) {
                binding.chipOverflowIndicator.text = getString(R.string.overflow_more, overflowCount)
                binding.chipOverflowIndicator.visibility = View.VISIBLE
            } else {
                binding.chipOverflowIndicator.visibility = View.GONE
            }
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
}
