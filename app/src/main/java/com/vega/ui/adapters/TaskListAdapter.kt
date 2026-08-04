package com.vega.ui.adapters

import android.graphics.Color
import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.interpolator.view.animation.LinearOutSlowInInterpolator
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.vega.R
import com.vega.data.database.Task
import com.vega.data.database.TaskPriority
import com.vega.data.database.TaskState
import com.vega.databinding.ItemTaskBinding
import com.vega.ui.theme.ThemeManager
import com.vega.ui.views.StatusPillView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class TaskListAdapter(
    private val onCompleteClick: (Task) -> Unit,
    private val onPostponeClick: (Task) -> Unit,
    private val onDeleteClick: (Task) -> Unit,
    private val onItemClick: ((Task) -> Unit)? = null,
    private val onItemLongClick: ((Task) -> Unit)? = null,
    private val layoutResId: Int = R.layout.item_task
) : ListAdapter<Task, TaskListAdapter.TaskViewHolder>(TaskDiffCallback()) {

    private val revealedTaskIds = mutableSetOf<String>()

    private var isSelectionMode = false
    private val selectedTaskIds = mutableSetOf<String>()
    private var onSelectionChangedListener: ((Int) -> Unit)? = null

    private var taskTagsMap = emptyMap<String, List<com.vega.data.database.Tag>>()

    fun setTaskTagsMap(map: Map<String, List<com.vega.data.database.Tag>>) {
        taskTagsMap = map
        notifyDataSetChanged()
    }

    fun getTaskTagsMap(): Map<String, List<com.vega.data.database.Tag>> = taskTagsMap

    fun setOnSelectionChangedListener(listener: (Int) -> Unit) {
        onSelectionChangedListener = listener
    }

    fun isSelectionMode(): Boolean = isSelectionMode

    fun getSelectedTaskIds(): List<String> = selectedTaskIds.toList()

    fun enterSelectionMode(firstTaskId: String) {
        isSelectionMode = true
        selectedTaskIds.clear()
        selectedTaskIds.add(firstTaskId)
        notifyDataSetChanged()
        onSelectionChangedListener?.invoke(selectedTaskIds.size)
    }

    fun selectAll() {
        isSelectionMode = true
        selectedTaskIds.clear()
        selectedTaskIds.addAll(currentList.map { it.id })
        notifyDataSetChanged()
        onSelectionChangedListener?.invoke(selectedTaskIds.size)
    }

    fun exitSelectionMode() {
        isSelectionMode = false
        selectedTaskIds.clear()
        notifyDataSetChanged()
        onSelectionChangedListener?.invoke(0)
    }

    fun clearSelection() {
        selectedTaskIds.clear()
        notifyDataSetChanged()
        onSelectionChangedListener?.invoke(0)
    }

    fun toggleSelection(taskId: String) {
        if (selectedTaskIds.contains(taskId)) {
            selectedTaskIds.remove(taskId)
        } else {
            selectedTaskIds.add(taskId)
        }
        val pos = currentList.indexOfFirst { it.id == taskId }
        if (pos != -1) {
            notifyItemChanged(pos)
        }
        if (selectedTaskIds.isEmpty()) {
            isSelectionMode = false
            notifyDataSetChanged()
        }
        onSelectionChangedListener?.invoke(selectedTaskIds.size)
    }

    private fun dpToPx(context: android.content.Context, dp: Int): Int {
        return (dp * context.resources.displayMetrics.density).toInt()
    }

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
        val view = LayoutInflater.from(parent.context).inflate(layoutResId, parent, false)
        val binding = ItemTaskBinding.bind(view)
        return TaskViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TaskViewHolder(val binding: ItemTaskBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(task: Task) {
            val context = binding.root.context
            
            binding.tvTaskTitle.text = task.title
            
            // Reset visual state modified by completion animation
            binding.tvTaskTitle.paintFlags = binding.tvTaskTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            binding.tvTaskTitle.alpha = 1.0f
            binding.root.alpha = 1.0f
            
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
            } else {
                binding.layoutTaskRecurrence.visibility = View.GONE
            }

            // Stroke based on selection
            val baseBorderColor = ThemeManager.border(context)
            val borderWithAlpha = ColorUtils.setAlphaComponent(baseBorderColor, 102) // 40% alpha

            if (isSelectionMode) {
                val isSel = selectedTaskIds.contains(task.id)
                binding.cardTask.strokeColor = if (isSel) ThemeManager.accentBlue(context) else borderWithAlpha
                binding.cardTask.strokeWidth = dpToPx(context, if (isSel) 2 else 1)
            } else {
                binding.cardTask.strokeColor = borderWithAlpha
                binding.cardTask.strokeWidth = dpToPx(context, 1)
            }

            // Bind Checkbox state
            binding.circularCheckbox.setOnCheckedChangeListener(null)
            binding.circularCheckbox.isSelectionMode = isSelectionMode
            if (isSelectionMode) {
                binding.circularCheckbox.setChecked(selectedTaskIds.contains(task.id), animate = false)
                binding.circularCheckbox.setOnCheckedChangeListener { _ ->
                    toggleSelection(task.id)
                }
            } else {
                val isDone = (task.state == TaskState.DONE.name)
                binding.circularCheckbox.setChecked(isDone, animate = false)
                binding.circularCheckbox.setOnCheckedChangeListener { isChecked ->
                    if (isChecked && task.state != TaskState.DONE.name) {
                        animateCompletion(task)
                    }
                }
            }

            // Bind Tags using StatusPillView dynamically
            val tags = taskTagsMap[task.id]
            if (!tags.isNullOrEmpty()) {
                binding.chipGroupTaskTags.removeAllViews()
                binding.chipGroupTaskTags.visibility = View.VISIBLE
                tags.forEach { tag ->
                    val pill = StatusPillView(context).apply {
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply {
                            marginEnd = dpToPx(context, 4)
                        }
                        setCustomTag(tag.name, tag.colorHex)
                    }
                    binding.chipGroupTaskTags.addView(pill)
                }
            } else {
                binding.chipGroupTaskTags.visibility = View.GONE
            }

            // Bind Priority using StatusPillView (already in XML)
            val priority = try {
                TaskPriority.valueOf(task.priority)
            } catch (e: Exception) {
                TaskPriority.NONE
            }
            binding.chipPriority.setPriority(priority)

            // Handle actions overlay visibility
            val showOverlay = isActionsRevealed(task.id)
            if (showOverlay) {
                binding.layoutActionsOverlay.visibility = View.VISIBLE
                binding.layoutForeground.visibility = View.INVISIBLE
            } else {
                binding.layoutActionsOverlay.visibility = View.GONE
                binding.layoutForeground.visibility = View.VISIBLE
            }

            // Foreground click/long-press
            binding.layoutForeground.setOnClickListener {
                if (isSelectionMode) {
                    toggleSelection(task.id)
                } else {
                    onItemClick?.invoke(task)
                }
            }

            binding.layoutForeground.setOnLongClickListener {
                if (isSelectionMode) {
                    toggleSelection(task.id)
                } else {
                    onItemLongClick?.invoke(task)
                }
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

            binding.layoutActionsOverlay.setOnClickListener {
                hideTaskActions(task.id)
            }
        }

        private fun animateCompletion(task: Task) {
            binding.tvTaskTitle.animate().cancel()
            binding.root.animate().cancel()

            binding.tvTaskTitle.paintFlags = binding.tvTaskTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            
            binding.tvTaskTitle.animate()
                .alpha(0.5f)
                .setDuration(300)
                .setInterpolator(LinearOutSlowInInterpolator())
                .start()

            binding.root.animate()
                .alpha(0.6f)
                .setDuration(300)
                .setInterpolator(LinearOutSlowInInterpolator())
                .withEndAction {
                    binding.root.postDelayed({
                        onCompleteClick(task)
                    }, 600)
                }
                .start()
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
