package com.vega.ui.fragments

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar
import com.vega.R
import com.vega.data.database.TaskPriority
import com.vega.data.database.TaskState
import com.vega.databinding.BottomSheetTaskDetailBinding
import com.vega.databinding.PopupPriorityBinding
import com.vega.databinding.PopupRecurrenceBinding
import com.vega.databinding.PopupStateBinding
import com.vega.ui.viewmodels.TaskDetailViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@AndroidEntryPoint
class TaskDetailFragment : BottomSheetDialogFragment() {

    private var _binding: BottomSheetTaskDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TaskDetailViewModel by viewModels()
    private var taskId: String? = null
    private var selectedDueDate: Long? = null
    private var selectedRecurrence: String? = null
    private var selectedPriority: TaskPriority = TaskPriority.NONE
    private var selectedState: TaskState = TaskState.INBOX

    // Active popup windows
    private var priorityPopup: PopupWindow? = null
    private var statePopup: PopupWindow? = null
    private var recurrencePopup: PopupWindow? = null

    override fun getTheme(): Int = R.style.Style_Vega_BottomSheet

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        taskId = arguments?.getString(ARG_TASK_ID)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetTaskDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (taskId == null) {
            dismiss()
            return
        }

        setupUI()
        observeViewModel()
        viewModel.loadTask(taskId!!)
    }

    private fun setupUI() {
        // Manage tags
        binding.btnManageTagsDetail.setOnClickListener {
            dismissAllPopups()
            TagManagementDialogFragment().show(parentFragmentManager, TagManagementDialogFragment.TAG)
        }

        // Save button
        binding.btnSave.setOnClickListener {
            dismissAllPopups()
            val title = binding.etTitle.text?.toString().orEmpty()
            val notes = binding.etNotes.text?.toString()
            viewModel.saveTask(
                title = title,
                dueDate = selectedDueDate,
                priority = selectedPriority,
                state = selectedState,
                recurrence = selectedRecurrence,
                notes = notes,
                selectedTagIds = getSelectedTagIds()
            )
        }

        // Dismiss popups on outside taps
        binding.scrollRoot.setOnClickListener { dismissAllPopups() }
        binding.layoutRootContainer.setOnClickListener { dismissAllPopups() }
        binding.etTitle.setOnFocusChangeListener { _, hasFocus -> if (hasFocus) dismissAllPopups() }
        binding.etNotes.setOnFocusChangeListener { _, hasFocus -> if (hasFocus) dismissAllPopups() }

        // Due Date
        binding.cardDueDate.setOnClickListener {
            dismissAllPopups()
            showDatePicker()
        }
        binding.btnClearDueDate.setOnClickListener {
            dismissAllPopups()
            selectedDueDate = null
            binding.tvDueDateValue.text = "No Date Assigned"
            binding.btnClearDueDate.visibility = View.GONE
            updateStateBasedOnDueDate()
        }

        // Priority dropdown
        binding.cardPriority.setOnClickListener {
            if (priorityPopup?.isShowing == true) {
                dismissAllPopups()
            } else {
                dismissAllPopups()
                showPriorityPopup()
            }
        }

        // State dropdown
        binding.cardState.setOnClickListener {
            if (statePopup?.isShowing == true) {
                dismissAllPopups()
            } else {
                dismissAllPopups()
                showStatePopup()
            }
        }

        // Recurrence dropdown
        binding.cardRecurrence.setOnClickListener {
            if (recurrencePopup?.isShowing == true) {
                dismissAllPopups()
            } else {
                dismissAllPopups()
                showRecurrencePopup()
            }
        }

        // Fragment result from RepeatsDialogFragment
        parentFragmentManager.setFragmentResultListener(
            RepeatsDialogFragment.REQUEST_KEY_RECURRENCE,
            viewLifecycleOwner
        ) { _, bundle ->
            val ruleJson = bundle.getString(RepeatsDialogFragment.RESULT_KEY_RULE_JSON)
            selectedRecurrence = ruleJson
            updateRecurrenceText(ruleJson)
        }
    }

    // ── PopupWindow helpers ──────────────────────────────────────────────────

    private fun showPriorityPopup() {
        val anchor = binding.cardPriority
        val popupBinding = PopupPriorityBinding.inflate(LayoutInflater.from(requireContext()))

        // Mark current selection
        val views = listOf(
            Triple(TaskPriority.NONE,   popupBinding.optionPriorityNone,   popupBinding.ivCheckPriorityNone),
            Triple(TaskPriority.HIGH,   popupBinding.optionPriorityHigh,   popupBinding.ivCheckPriorityHigh),
            Triple(TaskPriority.MEDIUM, popupBinding.optionPriorityMedium, popupBinding.ivCheckPriorityMedium),
            Triple(TaskPriority.LOW,    popupBinding.optionPriorityLow,    popupBinding.ivCheckPriorityLow)
        )
        for ((prio, row, check) in views) {
            val sel = prio == selectedPriority
            row.setBackgroundResource(if (sel) R.drawable.bg_setting_option_selected else R.drawable.bg_setting_card)
            check.visibility = if (sel) View.VISIBLE else View.GONE
        }

        val popup = buildPopup(popupBinding.root, anchor.width)
        priorityPopup = popup

        popupBinding.optionPriorityNone.setOnClickListener   { updatePriority(TaskPriority.NONE);   dismissAllPopups() }
        popupBinding.optionPriorityHigh.setOnClickListener   { updatePriority(TaskPriority.HIGH);   dismissAllPopups() }
        popupBinding.optionPriorityMedium.setOnClickListener { updatePriority(TaskPriority.MEDIUM); dismissAllPopups() }
        popupBinding.optionPriorityLow.setOnClickListener    { updatePriority(TaskPriority.LOW);    dismissAllPopups() }

        showPopupBelow(popup, anchor)
        binding.ivChevronPriority.setImageResource(R.drawable.ic_chevron_up)
    }

    private fun showStatePopup() {
        val anchor = binding.cardState
        val popupBinding = PopupStateBinding.inflate(LayoutInflater.from(requireContext()))

        val views = listOf(
            Triple(TaskState.INBOX,    popupBinding.optionStateInbox,    popupBinding.ivCheckStateInbox),
            Triple(TaskState.TODAY,    popupBinding.optionStateToday,    popupBinding.ivCheckStateToday),
            Triple(TaskState.UPCOMING, popupBinding.optionStateUpcoming, popupBinding.ivCheckStateUpcoming),
            Triple(TaskState.DONE,     popupBinding.optionStateDone,     popupBinding.ivCheckStateDone)
        )
        for ((state, row, check) in views) {
            val sel = state == selectedState
            row.setBackgroundResource(if (sel) R.drawable.bg_setting_option_selected else R.drawable.bg_setting_card)
            check.visibility = if (sel) View.VISIBLE else View.GONE
        }

        val popup = buildPopup(popupBinding.root, anchor.width)
        statePopup = popup

        popupBinding.optionStateInbox.setOnClickListener    { updateState(TaskState.INBOX);    dismissAllPopups() }
        popupBinding.optionStateToday.setOnClickListener    { updateState(TaskState.TODAY);    dismissAllPopups() }
        popupBinding.optionStateUpcoming.setOnClickListener { updateState(TaskState.UPCOMING); dismissAllPopups() }
        popupBinding.optionStateDone.setOnClickListener     { updateState(TaskState.DONE);     dismissAllPopups() }

        showPopupBelow(popup, anchor)
        binding.ivChevronState.setImageResource(R.drawable.ic_chevron_up)
    }

    private fun showRecurrencePopup() {
        val anchor = binding.cardRecurrence
        val popupBinding = PopupRecurrenceBinding.inflate(LayoutInflater.from(requireContext()))

        val isCustom = !selectedRecurrence.isNullOrBlank() &&
                selectedRecurrence != "DAILY" && selectedRecurrence != "WEEKLY" && selectedRecurrence != "MONTHLY"

        val views = listOf(
            Triple(null,       popupBinding.optionRecurrenceNone,    popupBinding.ivCheckRecurrenceNone),
            Triple("DAILY",    popupBinding.optionRecurrenceDaily,   popupBinding.ivCheckRecurrenceDaily),
            Triple("WEEKLY",   popupBinding.optionRecurrenceWeekly,  popupBinding.ivCheckRecurrenceWeekly),
            Triple("MONTHLY",  popupBinding.optionRecurrenceMonthly, popupBinding.ivCheckRecurrenceMonthly),
            Triple("CUSTOM",   popupBinding.optionRecurrenceCustom,  popupBinding.ivCheckRecurrenceCustom)
        )
        for ((key, row, check) in views) {
            val sel = if (key == "CUSTOM") isCustom else (key == selectedRecurrence)
            row.setBackgroundResource(if (sel) R.drawable.bg_setting_option_selected else R.drawable.bg_setting_card)
            check.visibility = if (sel) View.VISIBLE else View.GONE
        }

        val popup = buildPopup(popupBinding.root, anchor.width)
        recurrencePopup = popup

        popupBinding.optionRecurrenceNone.setOnClickListener    { updateRecurrence(null);      dismissAllPopups() }
        popupBinding.optionRecurrenceDaily.setOnClickListener   { updateRecurrence("DAILY");   dismissAllPopups() }
        popupBinding.optionRecurrenceWeekly.setOnClickListener  { updateRecurrence("WEEKLY");  dismissAllPopups() }
        popupBinding.optionRecurrenceMonthly.setOnClickListener { updateRecurrence("MONTHLY"); dismissAllPopups() }
        popupBinding.optionRecurrenceCustom.setOnClickListener  {
            dismissAllPopups()
            showRepeatsDialog()
        }

        showPopupBelow(popup, anchor)
        binding.ivChevronRecurrence.setImageResource(R.drawable.ic_chevron_up)
    }

    /** Build a PopupWindow that overlays other views */
    private fun buildPopup(contentView: View, widthPx: Int): PopupWindow {
        contentView.measure(
            View.MeasureSpec.makeMeasureSpec(widthPx, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        return PopupWindow(
            contentView,
            widthPx,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            true  // focusable — dismisses on outside touch
        ).apply {
            isOutsideTouchable = true
            elevation = 24f
            setOnDismissListener { resetChevrons() }
        }
    }

    /** Show popup anchored just below the anchor view, correctly handling scroll offsets */
    private fun showPopupBelow(popup: PopupWindow, anchor: View) {
        anchor.post {
            // showAsDropDown anchors relative to the view itself, so it works correctly
            // even when the anchor is inside a scrolled NestedScrollView
            popup.showAsDropDown(anchor, 0, 0)
        }
    }

    private fun dismissAllPopups() {
        priorityPopup?.dismiss()
        statePopup?.dismiss()
        recurrencePopup?.dismiss()
        priorityPopup = null
        statePopup = null
        recurrencePopup = null
        resetChevrons()
    }

    private fun resetChevrons() {
        if (priorityPopup?.isShowing != true)
            binding.ivChevronPriority.setImageResource(R.drawable.ic_chevron_down)
        if (statePopup?.isShowing != true)
            binding.ivChevronState.setImageResource(R.drawable.ic_chevron_down)
        if (recurrencePopup?.isShowing != true)
            binding.ivChevronRecurrence.setImageResource(R.drawable.ic_chevron_down)
    }

    // ── Priority / State / Recurrence update helpers ─────────────────────────

    private fun updatePriority(priority: TaskPriority) {
        selectedPriority = priority
        val (text, colorHex) = when (priority) {
            TaskPriority.HIGH   -> "High"   to "#E94560"
            TaskPriority.MEDIUM -> "Medium" to "#F2A65A"
            TaskPriority.LOW    -> "Low"    to "#3171C6"
            else                -> "None"   to "#8E9096"
        }
        binding.tvPriorityValue.text = text
        binding.tvPriorityValue.setTextColor(Color.parseColor(colorHex))
        binding.ivPriorityDot.backgroundTintList = ColorStateList.valueOf(Color.parseColor(colorHex))
    }

    private fun updateState(state: TaskState) {
        selectedState = state
        val (text, iconRes, colorHex) = when (state) {
            TaskState.TODAY    -> Triple("Today",    R.drawable.ic_nav_today,    "#F2A65A")
            TaskState.UPCOMING -> Triple("Upcoming", R.drawable.ic_nav_upcoming, "#A259FF")
            TaskState.DONE     -> Triple("Done",     R.drawable.ic_check,        "#4ECDC4")
            else               -> Triple("Inbox",    R.drawable.ic_nav_inbox,    "#3171C6")
        }
        binding.tvStateValue.text = text
        binding.ivStateIcon.setImageResource(iconRes)
        binding.ivStateIcon.setColorFilter(Color.parseColor(colorHex))
    }

    private fun updateRecurrence(ruleStr: String?) {
        selectedRecurrence = ruleStr
        updateRecurrenceText(ruleStr)
    }

    private fun updateRecurrenceText(ruleStr: String?) {
        val label = when (ruleStr?.uppercase()) {
            "DAILY"   -> "Every Day"
            "WEEKLY"  -> "Every Week"
            "MONTHLY" -> "Every Month"
            null, ""  -> "None"
            else      -> com.vega.utils.RecurrenceUtils.formatSummary(requireContext(), ruleStr)
        }
        binding.tvRecurrenceValue.text = label
    }

    // ── Date Pickers ─────────────────────────────────────────────────────────

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        selectedDueDate?.let { calendar.timeInMillis = it }

        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                showTimePicker(calendar)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun showTimePicker(calendar: Calendar) {
        TimePickerDialog(
            requireContext(),
            { _, hourOfDay, minute ->
                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                calendar.set(Calendar.MINUTE, minute)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                selectedDueDate = calendar.timeInMillis
                binding.tvDueDateValue.text = formatDueDate(selectedDueDate!!)
                binding.btnClearDueDate.visibility = View.VISIBLE
                updateStateBasedOnDueDate()
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            false
        ).show()
    }

    private fun showRepeatsDialog() {
        RepeatsDialogFragment.newInstance(selectedRecurrence, selectedDueDate)
            .show(parentFragmentManager, RepeatsDialogFragment.TAG)
    }

    // ── ViewModel observation ─────────────────────────────────────────────────

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                launch {
                    viewModel.task.collect { task ->
                        task?.let {
                            binding.etTitle.setText(it.title)
                            binding.etNotes.setText(it.notes.orEmpty())

                            selectedDueDate = it.dueDate
                            if (it.dueDate != null) {
                                binding.tvDueDateValue.text = formatDueDate(it.dueDate)
                                binding.btnClearDueDate.visibility = View.VISIBLE
                            } else {
                                binding.tvDueDateValue.text = "No Date Assigned"
                                binding.btnClearDueDate.visibility = View.GONE
                            }

                            selectedRecurrence = it.recurrence
                            updateRecurrenceText(it.recurrence)

                            val prio = runCatching { TaskPriority.valueOf(it.priority) }.getOrDefault(TaskPriority.NONE)
                            updatePriority(prio)

                            val st = runCatching { TaskState.valueOf(it.state) }.getOrDefault(TaskState.INBOX)
                            updateState(st)
                        }
                    }
                }

                launch {
                    combine(viewModel.allTags, viewModel.taskTags) { all, task -> Pair(all, task) }
                        .collect { (allTags, taskTags) ->
                            populateDetailTagChips(allTags, taskTags)
                        }
                }

                launch {
                    viewModel.saveSuccess.collect { success ->
                        if (success) {
                            Toast.makeText(requireContext(), "Task saved", Toast.LENGTH_SHORT).show()
                            dismiss()
                        }
                    }
                }

                launch {
                    viewModel.error.collect { errorMsg ->
                        errorMsg?.let {
                            Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
                            viewModel.clearError()
                        }
                    }
                }
            }
        }
    }

    // ── Tag chips ─────────────────────────────────────────────────────────────

    private fun populateDetailTagChips(
        allTags: List<com.vega.data.database.Tag>,
        assignedTags: List<com.vega.data.database.Tag>
    ) {
        val currentlySelectedIds = getSelectedTagIds().ifEmpty { assignedTags.map { it.id }.toSet() }
        binding.chipGroupDetailTags.removeAllViews()
        allTags.forEach { tagItem ->
            val isChecked = currentlySelectedIds.contains(tagItem.id)
            val tagColorHex = tagItem.colorHex.ifBlank { "#3171C6" }
            val parsedColor = runCatching { Color.parseColor(tagColorHex) }.getOrDefault(Color.parseColor("#3171C6"))

            val chip = Chip(requireContext()).apply {
                id = View.generateViewId()
                setTag(tagItem.id)
                text = tagItem.name
                isCheckable = true
                this.isChecked = isChecked

                if (isChecked) {
                    chipIcon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_check)
                    chipIconTint = ColorStateList.valueOf(parsedColor)
                    isChipIconVisible = true
                    chipBackgroundColor = ColorStateList.valueOf(Color.parseColor("#1C1D22"))
                    chipStrokeColor = ColorStateList.valueOf(parsedColor)
                    chipStrokeWidth = 1.5f.dpToPx()
                    setTextColor(parsedColor)
                } else {
                    val dotDrawable = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(parsedColor)
                        setSize(10f.dpToPx().toInt(), 10f.dpToPx().toInt())
                    }
                    chipIcon = dotDrawable
                    chipIconTint = null
                    isChipIconVisible = true
                    chipBackgroundColor = ColorStateList.valueOf(Color.parseColor("#1C1D22"))
                    chipStrokeColor = ColorStateList.valueOf(Color.parseColor("#2A2C34"))
                    chipStrokeWidth = 1f.dpToPx()
                    setTextColor(Color.parseColor("#FFFFFF"))
                }

                chipCornerRadius = 50f
                chipMinHeight = 28f.dpToPx()

                setOnClickListener {
                    dismissAllPopups()
                    this.isChecked = !isChecked
                    populateDetailTagChips(allTags, assignedTags)
                }
            }
            binding.chipGroupDetailTags.addView(chip)
        }
    }

    private fun getSelectedTagIds(): List<String> {
        val list = mutableListOf<String>()
        for (i in 0 until binding.chipGroupDetailTags.childCount) {
            val chip = binding.chipGroupDetailTags.getChildAt(i) as? Chip
            if (chip != null && chip.isChecked && chip.tag != null) {
                list.add(chip.tag.toString())
            }
        }
        return list
    }

    // ── Utilities ─────────────────────────────────────────────────────────────

    private fun Float.dpToPx(): Float = this * resources.displayMetrics.density

    private fun formatDueDate(timestamp: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        val hasTime = cal.get(Calendar.HOUR_OF_DAY) != 0 || cal.get(Calendar.MINUTE) != 0
        val pattern = if (hasTime) "MMM d 'at' h:mm a" else "MMM d, yyyy"
        return SimpleDateFormat(pattern, Locale.getDefault()).format(cal.time)
    }

    private fun updateStateBasedOnDueDate() {
        if (!selectedRecurrence.isNullOrBlank()) return
        val dueDate = selectedDueDate
        if (dueDate == null) { updateState(TaskState.INBOX); return }

        val todayEnd = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59);      set(Calendar.MILLISECOND, 999)
        }.timeInMillis

        updateState(if (dueDate <= todayEnd) TaskState.TODAY else TaskState.UPCOMING)
    }

    override fun onDestroyView() {
        dismissAllPopups()
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "TaskDetailFragment"
        private const val ARG_TASK_ID = "arg_task_id"

        fun newInstance(taskId: String) = TaskDetailFragment().apply {
            arguments = Bundle().apply { putString(ARG_TASK_ID, taskId) }
        }
    }
}
