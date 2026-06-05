package com.vega.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.vega.R
import com.vega.data.database.Task
import com.vega.data.database.TaskPriority
import com.vega.data.database.TaskState
import com.vega.databinding.ItemSearchResultBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class SearchResultAdapter(
    private val onItemClick: (Task) -> Unit
) : ListAdapter<Task, SearchResultAdapter.SearchResultViewHolder>(TaskDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchResultViewHolder {
        val binding = ItemSearchResultBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SearchResultViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SearchResultViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class SearchResultViewHolder(private val binding: ItemSearchResultBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(task: Task) {
            binding.tvTaskTitle.text = task.title

            // Due Date
            if (task.dueDate != null) {
                binding.tvTaskDueDate.text = formatDueDate(task.dueDate)
                binding.tvTaskDueDate.visibility = View.VISIBLE
            } else {
                binding.tvTaskDueDate.visibility = View.GONE
            }

            // Priority
            val priority = try {
                TaskPriority.valueOf(task.priority)
            } catch (e: Exception) {
                TaskPriority.NONE
            }

            if (priority != TaskPriority.NONE) {
                binding.chipPriority.visibility = View.VISIBLE
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

            // State Badge
            val state = try {
                TaskState.valueOf(task.state)
            } catch (e: Exception) {
                TaskState.INBOX
            }

            val context = binding.root.context
            val (stateColorRes, stateTextRes) = when (state) {
                TaskState.INBOX -> Pair(R.color.state_inbox, R.string.state_inbox)
                TaskState.TODAY -> Pair(R.color.state_today, R.string.state_today)
                TaskState.UPCOMING -> Pair(R.color.state_upcoming, R.string.state_upcoming)
                TaskState.DONE -> Pair(R.color.state_done, R.string.state_done)
            }

            binding.chipState.text = context.getString(stateTextRes)
            binding.chipState.setChipBackgroundColorResource(stateColorRes)
            binding.chipState.setTextColor(ContextCompat.getColor(context, android.R.color.white))

            binding.root.setOnClickListener {
                onItemClick(task)
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
