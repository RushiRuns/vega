package com.vega.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vega.data.database.Tag
import com.vega.data.repository.TagRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TagViewModel @Inject constructor(
    private val repository: TagRepository
) : ViewModel() {

    val allTags: StateFlow<List<Tag>> = repository.allTags
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun createTag(name: String, colorHex: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.createTag(name.trim(), colorHex)
        }
    }

    fun updateTag(tag: Tag, newName: String, newColorHex: String) {
        if (newName.isBlank()) return
        viewModelScope.launch {
            repository.updateTag(tag.copy(name = newName.trim(), colorHex = newColorHex))
        }
    }

    fun deleteTag(tag: Tag) {
        viewModelScope.launch {
            repository.deleteTag(tag)
        }
    }
}
