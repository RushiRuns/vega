package com.vega.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.vega.R
import com.vega.data.database.Task
import com.vega.data.database.TaskPriority
import com.vega.data.database.TaskState
import com.vega.databinding.ItemTaskBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class TaskListAdapter(
    private val onCompleteClick: (Task) -> Unit,
    private val onPostponeClick: (Task) -> Unit,
    private val onDeleteClick: (Task) -> Unit,
    private val onItemClick: ((Task) -> Unit)? = null,
    private val onItemLongClick: ((Task) -> Unit)? = null
) : ListAdapter<Task, TaskListAdapter.TaskViewHolder>(TaskDiffCallback()) {

    private val revealedTaskIds = mutableSetOf<String>()

    fun isActionsRevealed(taskId: String): Boolean = revealedTaskIds.contains(taskId)

    fun revealTaskActions(taskId: String) {
        val previousIds = revealedTaskIds.toList()
        revealedTaskIds.clear()
        revealedTaskIds.add(taskId)
        for (id in previousIds) {
            val pos = currentList.indexOfFirst { it.id == id }
            if (pos != -1) notifyItemChanged(pos)
        }
        val newPos = currentList.indexOfFirst { it.id == taskId }
        if (newPos != -1) notifyItemChanged(newPos)
    }

    fun hideTaskActions(taskId: String) {
        if (revealedTaskIds.remove(taskId)) {
            val pos = currentList.indexOfFirst { it.id == taskId }
            if (pos != -1) notifyItemChanged(pos)
        }
    }

    fun clearAllRevealedActions() {
        if (revealedTaskIds.isNotEmpty()) {
            val previousIds = revealedTaskIds.toList()
            revealedTaskIds.clear()
            for (id in previousIds) {
                val pos = currentList.indexOfFirst { it.id == id }
                if (pos != -1) notifyItemChanged(pos)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val binding = ItemTaskBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TaskViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TaskViewHolder(val binding: ItemTaskBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(task: Task) {
            binding.tvTaskTitle.text = task.title
            
            // Bind Due Date
            if (task.dueDate != null) {
                binding.tvTaskDueDate.text = formatDueDate(task.dueDate)
                binding.tvTaskDueDate.visibility = View.VISIBLE
            } else {
                binding.tvTaskDueDate.visibility = View.GONE
            }

            // Bind Checkbox state
            binding.cbComplete.setOnCheckedChangeListener(null)
            binding.cbComplete.isChecked = (task.state == TaskState.DONE.name)
            binding.cbComplete.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked && task.state != TaskState.DONE.name) {
                    onCompleteClick(task)
                }
            }

            // Bind Priority
            val priority = try {
                TaskPriority.valueOf(task.priority)
            } catch (e: Exception) {
                TaskPriority.NONE
            }

            if (priority != TaskPriority.NONE) {
                binding.chipPriority.visibility = View.VISIBLE
                
                // Color based on priority
                val context = binding.root.context
                val (colorRes, textRes) = when (priority) {
                    TaskPriority.HIGH -> Pair(R.color.error, R.string.priority_high)
                    TaskPriority.MEDIUM -> Pair(R.color.secondary, R.string.priority_medium)
                    TaskPriority.LOW -> Pair(R.color.primary, R.string.priority_low)
                    else -> Pair(android.R.color.transparent, R.string.priority_none)
                }
                binding.chipPriority.text = context.getString(textRes)
                binding.chipPriority.setChipBackgroundColorResource(colorRes)
            } else {
                binding.chipPriority.visibility = View.GONE
            }

            // Handle actions overlay visibility
            val showOverlay = isActionsRevealed(task.id)
            if (showOverlay) {
                binding.layoutActionsOverlay.visibility = View.VISIBLE
                binding.layoutForeground.visibility = View.INVISIBLE // prevent clicks on foreground
            } else {
                binding.layoutActionsOverlay.visibility = View.GONE
                binding.layoutForeground.visibility = View.VISIBLE
            }

            // Foreground click/long-press
            binding.layoutForeground.setOnClickListener {
                onItemClick?.invoke(task)
            }

            binding.layoutForeground.setOnLongClickListener {
                onItemLongClick?.invoke(task)
                true
            }

            // Background Actions click listeners
            binding.btnActionPostpone.setOnClickListener {
                hideTaskActions(task.id)
                onPostponeClick(task)
            }

            binding.btnActionDelete.setOnClickListener {
                hideTaskActions(task.id)
                onDeleteClick(task)
            }

            // Tapping actions overlay outer area dismisses overlay
            binding.layoutActionsOverlay.setOnClickListener {
                hideTaskActions(task.id)
            }
        }

        private fun formatDueDate(timestamp: Long): String {
            val calendar = Calendar.getInstance().apply { timeInMillis = timestamp }
            val hasTime = calendar.get(Calendar.HOUR_OF_DAY) != 0 || calendar.get(Calendar.MINUTE) != 0
            
            val pattern = if (hasTime) "MMM d 'at' h:mm a" else "MMM d"
            return SimpleDateFormat(pattern, Locale.getDefault()).format(calendar.time)
        }
    }

    class TaskDiffCallback : DiffUtil.ItemCallback<Task>() {
        override fun areItemsTheSame(oldItem: Task, newItem: Task): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Task, newItem: Task): Boolean {
            return oldItem == newItem
        }
    }
}
