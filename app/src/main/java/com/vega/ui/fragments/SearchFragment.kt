package com.vega.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.vega.databinding.FragmentSearchBinding
import com.vega.ui.adapters.SearchResultAdapter
import com.vega.ui.viewmodels.SearchViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

import com.google.android.material.snackbar.Snackbar

@AndroidEntryPoint
class SearchFragment : Fragment() {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SearchViewModel by viewModels()
    private lateinit var adapter: SearchResultAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupListeners()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        adapter = SearchResultAdapter(
            onItemClick = { task ->
                showTaskDetailSheet(task.id)
            }
        )
        binding.rvSearchResults.layoutManager = LinearLayoutManager(requireContext())
        binding.rvSearchResults.adapter = adapter
    }

    private fun setupListeners() {
        binding.etSearch.doAfterTextChanged { text ->
            viewModel.setSearchQuery(text?.toString().orEmpty())
        }
    }

    private fun showTaskDetailSheet(taskId: String) {
        TaskDetailFragment.newInstance(taskId).show(childFragmentManager, TaskDetailFragment.TAG)
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Observe Search Results
                launch {
                    viewModel.searchResults.collect { tasks ->
                        adapter.submitList(tasks)
                        
                        val query = viewModel.searchQuery.value
                        if (query.isNotBlank() && tasks.isEmpty() && !viewModel.isLoading.value) {
                            binding.layoutEmptySearch.visibility = View.VISIBLE
                            binding.rvSearchResults.visibility = View.GONE
                        } else {
                            binding.layoutEmptySearch.visibility = View.GONE
                            binding.rvSearchResults.visibility = View.VISIBLE
                        }
                    }
                }

                // Observe Loading state
                launch {
                    viewModel.isLoading.collect { isLoading ->
                        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
                        
                        // Recheck empty state when loading finishes
                        if (!isLoading) {
                            val query = viewModel.searchQuery.value
                            val tasks = viewModel.searchResults.value
                            if (query.isNotBlank() && tasks.isEmpty()) {
                                binding.layoutEmptySearch.visibility = View.VISIBLE
                                binding.rvSearchResults.visibility = View.GONE
                            } else {
                                binding.layoutEmptySearch.visibility = View.GONE
                                binding.rvSearchResults.visibility = View.VISIBLE
                            }
                        }
                    }
                }

                // Observe Errors
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
