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
import com.vega.databinding.ItemUpcomingHeaderBinding
import com.vega.ui.models.UpcomingListItem
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class UpcomingTasksAdapter(
    private val onCompleteClick: (Task) -> Unit,
    private val onPostponeClick: (Task) -> Unit,
    private val onDeleteClick: (Task) -> Unit,
    private val onItemClick: ((Task) -> Unit)? = null,
    private val onItemLongClick: ((Task) -> Unit)? = null
) : ListAdapter<UpcomingListItem, RecyclerView.ViewHolder>(UpcomingListItemDiffCallback()) {

    private val revealedTaskIds = mutableSetOf<String>()

    fun isActionsRevealed(taskId: String): Boolean = revealedTaskIds.contains(taskId)

    fun revealTaskActions(taskId: String) {
        val previousIds = revealedTaskIds.toList()
        revealedTaskIds.clear()
        revealedTaskIds.add(taskId)
        for (id in previousIds) {
            val pos = currentList.indexOfFirst { it is UpcomingListItem.TaskItem && it.task.id == id }
            if (pos != -1) notifyItemChanged(pos)
        }
        val newPos = currentList.indexOfFirst { it is UpcomingListItem.TaskItem && it.task.id == taskId }
        if (newPos != -1) notifyItemChanged(newPos)
    }

    fun hideTaskActions(taskId: String) {
        if (revealedTaskIds.remove(taskId)) {
            val pos = currentList.indexOfFirst { it is UpcomingListItem.TaskItem && it.task.id == taskId }
            if (pos != -1) notifyItemChanged(pos)
        }
    }

    fun clearAllRevealedActions() {
        if (revealedTaskIds.isNotEmpty()) {
            val previousIds = revealedTaskIds.toList()
            revealedTaskIds.clear()
            for (id in previousIds) {
                val pos = currentList.indexOfFirst { it is UpcomingListItem.TaskItem && it.task.id == id }
                if (pos != -1) notifyItemChanged(pos)
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is UpcomingListItem.Header -> TYPE_HEADER
            is UpcomingListItem.TaskItem -> TYPE_TASK
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_HEADER -> {
                val binding = ItemUpcomingHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                HeaderViewHolder(binding)
            }
            TYPE_TASK -> {
                val binding = ItemTaskBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                TaskViewHolder(binding)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is UpcomingListItem.Header -> (holder as HeaderViewHolder).bind(item)
            is UpcomingListItem.TaskItem -> (holder as TaskViewHolder).bind(item.task)
        }
    }

    inner class HeaderViewHolder(private val binding: ItemUpcomingHeaderBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(header: UpcomingListItem.Header) {
            binding.tvHeaderTitle.text = header.title
        }
    }

    inner class TaskViewHolder(val binding: ItemTaskBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(task: Task) {
            binding.tvTaskTitle.text = task.title
            val context = binding.root.context
            
            // Bind Due Date
            if (task.dueDate != null) {
                binding.tvTaskDueDate.text = formatDueDate(task.dueDate)
                binding.tvTaskDueDate.visibility = View.VISIBLE
            } else {
                binding.tvTaskDueDate.visibility = View.GONE
            }

            // Bind Recurrence
            if (!task.recurrence.isNullOrBlank()) {
                binding.layoutTaskRecurrence.visibility = View.VISIBLE
                binding.tvTaskRecurrence.text = com.vega.utils.RecurrenceUtils.formatSummary(context, task.recurrence)
                binding.cardTask.setCardBackgroundColor(context.getColor(R.color.task_card_background))
                binding.cardTask.strokeColor = context.getColor(R.color.task_card_stroke)
            } else {
                binding.layoutTaskRecurrence.visibility = View.GONE
                binding.cardTask.setCardBackgroundColor(context.getColor(R.color.task_card_background))
                binding.cardTask.strokeColor = context.getColor(R.color.task_card_stroke)
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

    class UpcomingListItemDiffCallback : DiffUtil.ItemCallback<UpcomingListItem>() {
        override fun areItemsTheSame(oldItem: UpcomingListItem, newItem: UpcomingListItem): Boolean {
            return when {
                oldItem is UpcomingListItem.Header && newItem is UpcomingListItem.Header -> {
                    oldItem.id == newItem.id
                }
                oldItem is UpcomingListItem.TaskItem && newItem is UpcomingListItem.TaskItem -> {
                    oldItem.task.id == newItem.task.id
                }
                else -> false
            }
        }

        override fun areContentsTheSame(oldItem: UpcomingListItem, newItem: UpcomingListItem): Boolean {
            return oldItem == newItem
        }
    }

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_TASK = 1
    }
}
