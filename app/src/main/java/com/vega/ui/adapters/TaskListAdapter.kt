package com.vega.ui.adapters

import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.interpolator.view.animation.LinearOutSlowInInterpolator
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
    private val onItemLongClick: ((Task) -> Unit)? = null,
    private val itemLayoutRes: Int = R.layout.item_task
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
        val view = LayoutInflater.from(parent.context).inflate(itemLayoutRes, parent, false)
        val binding = ItemTaskBinding.bind(view)
        val context = parent.context

        if (itemLayoutRes == R.layout.item_task) {
            val cornerRadius = context.resources.getDimension(R.dimen.card_corner_radius)
            val gradientDrawable = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(
                    ContextCompat.getColor(context, R.color.vega_surface_elevated),
                    ContextCompat.getColor(context, R.color.vega_surface)
                )
            ).apply {
                setCornerRadius(cornerRadius)
            }
            binding.cardTask.background = gradientDrawable
            (binding.cardTask as? com.google.android.material.card.MaterialCardView)?.setCardBackgroundColor(Color.TRANSPARENT)
        }

        return TaskViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        holder.bind(getItem(position))
        val dividerView = holder.itemView.findViewById<View>(R.id.view_item_divider)
        if (dividerView != null) {
            dividerView.visibility = if (position == itemCount - 1) View.GONE else View.VISIBLE
        }
    }

    inner class TaskViewHolder(val binding: ItemTaskBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(task: Task) {
            binding.tvTaskTitle.text = task.title
            val context = binding.root.context
            
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
            val baseBorderColor = ContextCompat.getColor(context, R.color.vega_border)
            val borderWithAlpha = ColorUtils.setAlphaComponent(baseBorderColor, 102) // 40% alpha

            if (itemLayoutRes == R.layout.item_task_today) {
                (binding.cardTask as? com.google.android.material.card.MaterialCardView)?.strokeWidth = 0
            } else {
                if (isSelectionMode) {
                    val isSel = selectedTaskIds.contains(task.id)
                    (binding.cardTask as? com.google.android.material.card.MaterialCardView)?.strokeColor = if (isSel) ContextCompat.getColor(context, R.color.vega_primary) else borderWithAlpha
                    (binding.cardTask as? com.google.android.material.card.MaterialCardView)?.strokeWidth = dpToPx(context, if (isSel) 2 else 1)
                } else {
                    (binding.cardTask as? com.google.android.material.card.MaterialCardView)?.strokeColor = borderWithAlpha
                    (binding.cardTask as? com.google.android.material.card.MaterialCardView)?.strokeWidth = dpToPx(context, 1)
                }
            }

            // Bind Checkbox state
            binding.circularCheckbox.setOnCheckedChangeListener(null)
            binding.circularCheckbox.isSquare = (itemLayoutRes == R.layout.item_task_today)
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

            // Bind Tags
            val tags = taskTagsMap[task.id]
            if (!tags.isNullOrEmpty()) {
                binding.chipGroupTaskTags.removeAllViews()
                binding.chipGroupTaskTags.visibility = View.VISIBLE
                tags.forEach { tag ->
                    val chip = com.google.android.material.chip.Chip(context).apply {
                        text = tag.name
                        chipMinHeight = context.resources.getDimension(R.dimen.status_pill_height)
                        chipCornerRadius = context.resources.getDimension(R.dimen.pill_corner_radius)
                        textAppearance = R.style.TextAppearance_Vega_Label
                        isClickable = false
                        isFocusable = false
                        val colorInt = try { Color.parseColor(tag.colorHex) } catch (e: Exception) { ContextCompat.getColor(context, R.color.vega_accent_green) }
                        val bgWithAlpha = ColorUtils.setAlphaComponent(colorInt, 38) // ~15% alpha
                        chipBackgroundColor = android.content.res.ColorStateList.valueOf(bgWithAlpha)
                        setTextColor(colorInt)
                    }
                    binding.chipGroupTaskTags.addView(chip)
                }
            } else {
                binding.chipGroupTaskTags.visibility = View.GONE
            }

            // Bind Priority
            val priority = try {
                TaskPriority.valueOf(task.priority)
            } catch (e: Exception) {
                TaskPriority.NONE
            }

            if (priority != TaskPriority.NONE) {
                binding.chipPriority.visibility = View.VISIBLE
                
                val (colorRes, textRes) = when (priority) {
                    TaskPriority.HIGH -> Pair(R.color.vega_priority_high, R.string.priority_high)
                    TaskPriority.MEDIUM -> Pair(R.color.vega_priority_medium, R.string.priority_medium)
                    TaskPriority.LOW -> Pair(R.color.vega_priority_low, R.string.priority_low)
                    else -> Pair(android.R.color.transparent, R.string.priority_none)
                }
                val accentColor = ContextCompat.getColor(context, colorRes)
                val bgWithAlpha = ColorUtils.setAlphaComponent(accentColor, 38) // ~15% alpha fill
                binding.chipPriority.text = context.getString(textRes)
                binding.chipPriority.chipBackgroundColor = android.content.res.ColorStateList.valueOf(bgWithAlpha)
                binding.chipPriority.setTextColor(accentColor)
                binding.chipPriority.chipMinHeight = context.resources.getDimension(R.dimen.status_pill_height)
                binding.chipPriority.chipCornerRadius = context.resources.getDimension(R.dimen.pill_corner_radius)
            } else {
                binding.chipPriority.visibility = View.GONE
            }

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

