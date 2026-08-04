package com.vega.ui.fragments

import android.graphics.Canvas
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.vega.R
import com.vega.data.database.Task
import com.vega.databinding.FragmentTodayBinding
import com.vega.ui.adapters.TaskListAdapter
import com.vega.ui.viewmodels.TodayViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import com.vega.ui.theme.ThemeManager
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
    private var hasAnimatedListEntrance = false

    private fun runEntranceStaggerAnimation(recyclerView: RecyclerView) {
        if (hasAnimatedListEntrance) return
        hasAnimatedListEntrance = true

        recyclerView.post {
            val childCount = recyclerView.childCount
            for (i in 0 until minOf(childCount, 5)) {
                val child = recyclerView.getChildAt(i) ?: continue
                child.translationY = 80f
                child.alpha = 0f
                child.animate()
                    .translationY(0f)
                    .alpha(1f)
                    .setStartDelay(i * 50L)
                    .setDuration(300)
                    .setInterpolator(android.view.animation.OvershootInterpolator(1.0f))
                    .start()
            }
        }
    }

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

        setupHeaderDate()
        setupRecyclerView()
        observeViewModel()
    }

    private fun setupHeaderDate() {
        val calendar = Calendar.getInstance()
        val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
        val dateFormat = SimpleDateFormat("MMMM d\nyyyy", Locale.getDefault())

        binding.tvDayName.text = dayFormat.format(calendar.time)
        binding.tvDateFull.text = dateFormat.format(calendar.time)
        setupSummaryGreeting()
    }

    private fun setupSummaryGreeting() {
        val context = requireContext()
        val builder = android.text.SpannableStringBuilder()

        val mutedColor = ThemeManager.textTertiary(context)
        val whiteColor = ThemeManager.textPrimary(context)

        fun appendMuted(text: String) {
            val start = builder.length
            builder.append(text)
            builder.setSpan(
                android.text.style.ForegroundColorSpan(mutedColor),
                start,
                builder.length,
                android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        fun appendBoldWhite(text: String) {
            val start = builder.length
            builder.append(text)
            builder.setSpan(android.text.style.ForegroundColorSpan(whiteColor), start, builder.length, android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            builder.setSpan(android.text.style.StyleSpan(android.graphics.Typeface.BOLD), start, builder.length, android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        fun appendInlineIcon(drawableRes: Int, sizeDp: Int = 20) {
            val drawable = androidx.core.content.ContextCompat.getDrawable(context, drawableRes)?.mutate() ?: return
            val px = (sizeDp * resources.displayMetrics.density).toInt()
            drawable.setBounds(0, 0, px, px)
            val start = builder.length
            builder.append(" ")
            builder.setSpan(
                android.text.style.ImageSpan(drawable, android.text.style.ImageSpan.ALIGN_CENTER),
                start,
                builder.length,
                android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            builder.append(" ")
        }

        appendMuted("Good morning,")
        appendInlineIcon(R.drawable.ic_summary_avatar, 26)
        appendBoldWhite("Alexey. ")

        appendMuted("You have")
        appendInlineIcon(R.drawable.ic_inline_meetings, 18)
        appendBoldWhite("3 meetings, ")

        appendInlineIcon(R.drawable.ic_inline_tasks, 18)
        appendBoldWhite("2 tasks ")
        appendMuted("and")
        appendInlineIcon(R.drawable.ic_inline_habit, 18)
        appendBoldWhite("1 habit ")

        appendMuted("today. You're ")
        appendBoldWhite("mostly free ")

        appendBoldWhite("after 4 pm.")

        binding.tvSummaryGreeting.text = builder
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
            },
            layoutResId = R.layout.item_task_today
        )
        binding.rvTodayTasks.layoutManager = LinearLayoutManager(requireContext())
        binding.rvTodayTasks.adapter = adapter
        binding.rvTodayTasks.isNestedScrollingEnabled = false
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

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Observe task tags map
                launch {
                    viewModel.taskTagsMap.collect { map ->
                        adapter.setTaskTagsMap(map)
                    }
                }

                // Observe today tasks
                launch {
                    viewModel.todayTasks.collect { tasks ->
                        val isInitialEmission = !hasAnimatedListEntrance && tasks.isNotEmpty()
                        adapter.submitList(tasks) {
                            if (isInitialEmission) {
                                runEntranceStaggerAnimation(binding.rvTodayTasks)
                            }
                        }

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

                // Observe total tasks size for soft cap warning (T033)
                launch {
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

                // Observe errors
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
