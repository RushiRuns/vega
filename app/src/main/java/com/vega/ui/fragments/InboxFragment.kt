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

import com.google.android.material.snackbar.Snackbar

@AndroidEntryPoint
class InboxFragment : Fragment() {

    private var _binding: FragmentInboxBinding? = null
    private val binding get() = _binding!!

    private val viewModel: InboxViewModel by viewModels()
    private lateinit var adapter: TaskListAdapter

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
        binding.rvInboxTasks.layoutManager = LinearLayoutManager(requireContext())
        binding.rvInboxTasks.adapter = adapter
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
        itemTouchHelper.attachToRecyclerView(binding.rvInboxTasks)
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.inboxTasks.collect { tasks ->
                        adapter.submitList(tasks)
                        if (tasks.isEmpty()) {
                            binding.layoutEmptyState.visibility = View.VISIBLE
                            binding.rvInboxTasks.visibility = View.GONE
                        } else {
                            binding.layoutEmptyState.visibility = View.GONE
                            binding.rvInboxTasks.visibility = View.VISIBLE
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
