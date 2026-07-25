package com.vega.ui.fragments

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.vega.R
import com.vega.data.database.Tag
import com.vega.databinding.DialogAddEditTagBinding
import com.vega.databinding.DialogManageTagsBinding
import com.vega.databinding.ItemTagManageBinding
import com.vega.ui.viewmodels.TagViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class TagManagementDialogFragment : BottomSheetDialogFragment() {

    private var _binding: DialogManageTagsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TagViewModel by viewModels()
    private lateinit var adapter: TagListAdapter

    override fun getTheme(): Int = R.style.Style_Vega_BottomSheet

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogManageTagsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observeTags()

        binding.btnAddNewTag.setOnClickListener {
            showAddEditTagDialog(null)
        }

        binding.btnQuickAdd.setOnClickListener {
            val name = binding.etQuickTagName.text?.toString()?.trim().orEmpty()
            if (name.isNotBlank()) {
                viewModel.createTag(name, "#3171C6")
                binding.etQuickTagName.setText("")
            } else {
                Toast.makeText(requireContext(), "Please enter a tag name", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnDoneManageTags.setOnClickListener {
            dismiss()
        }
    }

    private fun setupRecyclerView() {
        adapter = TagListAdapter(
            onEditClick = { tag -> showAddEditTagDialog(tag) },
            onDeleteClick = { tag -> showDeleteConfirmationDialog(tag) }
        )
        binding.rvManageTags.layoutManager = LinearLayoutManager(requireContext())
        binding.rvManageTags.adapter = adapter
    }

    private fun observeTags() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.allTags.collect { tags ->
                    adapter.submitList(tags)
                    if (tags.isEmpty()) {
                        binding.tvEmptyTags.visibility = View.VISIBLE
                        binding.rvManageTags.visibility = View.GONE
                    } else {
                        binding.tvEmptyTags.visibility = View.GONE
                        binding.rvManageTags.visibility = View.VISIBLE
                    }
                }
            }
        }
    }

    private fun showAddEditTagDialog(tagToEdit: Tag?) {
        val dialogBinding = DialogAddEditTagBinding.inflate(layoutInflater)
        if (tagToEdit != null) {
            dialogBinding.tvDialogTitle.text = "Edit Tag"
            dialogBinding.etTagName.setText(tagToEdit.name)
            matchColorToChip(dialogBinding, tagToEdit.colorHex)
        } else {
            dialogBinding.tvDialogTitle.text = "New Tag"
            dialogBinding.chipColorTeal.isChecked = true
        }

        MaterialAlertDialogBuilder(requireContext())
            .setView(dialogBinding.root)
            .setPositiveButton(if (tagToEdit != null) "Save" else "Create") { _, _ ->
                val name = dialogBinding.etTagName.text.toString()
                val colorHex = getSelectedColorHex(dialogBinding)
                if (tagToEdit != null) {
                    viewModel.updateTag(tagToEdit, name, colorHex)
                } else {
                    viewModel.createTag(name, colorHex)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun matchColorToChip(binding: DialogAddEditTagBinding, colorHex: String) {
        when (colorHex.uppercase()) {
            "#4ECDC4" -> binding.chipColorTeal.isChecked = true
            "#E94560" -> binding.chipColorRed.isChecked = true
            "#F2A65A" -> binding.chipColorAmber.isChecked = true
            "#6C7CE7" -> binding.chipColorPurple.isChecked = true
            else -> binding.chipColorGray.isChecked = true
        }
    }

    private fun getSelectedColorHex(binding: DialogAddEditTagBinding): String {
        return when (binding.chipGroupTagColors.checkedChipId) {
            R.id.chip_color_red -> "#E94560"
            R.id.chip_color_amber -> "#F2A65A"
            R.id.chip_color_purple -> "#6C7CE7"
            R.id.chip_color_gray -> "#6B7080"
            else -> "#4ECDC4"
        }
    }

    private fun showDeleteConfirmationDialog(tag: Tag) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Tag")
            .setMessage("Are you sure you want to delete tag '${tag.name}'?")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteTag(tag)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "TagManagementDialogFragment"
    }

    private class TagListAdapter(
        private val onEditClick: (Tag) -> Unit,
        private val onDeleteClick: (Tag) -> Unit
    ) : androidx.recyclerview.widget.ListAdapter<Tag, TagListAdapter.ViewHolder>(
        object : androidx.recyclerview.widget.DiffUtil.ItemCallback<Tag>() {
            override fun areItemsTheSame(oldItem: Tag, newItem: Tag): Boolean = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: Tag, newItem: Tag): Boolean = oldItem == newItem
        }
    ) {
        class ViewHolder(val binding: ItemTagManageBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemTagManageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val tag = getItem(position)
            holder.binding.tvTagName.text = tag.name
            
            val dotDrawable = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(try { Color.parseColor(tag.colorHex) } catch (e: Exception) { Color.parseColor("#3171C6") })
            }
            holder.binding.viewTagColorDot.background = dotDrawable

            holder.binding.btnEditTag.setOnClickListener { onEditClick(tag) }
            holder.binding.btnDeleteTag.setOnClickListener { onDeleteClick(tag) }
        }
    }
}
