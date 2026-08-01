package com.sizesapp.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sizesapp.data.db.ClosetItem
import com.sizesapp.data.repository.ClosetRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(private val repository: ClosetRepository) : ViewModel() {

    val items: StateFlow<List<ClosetItem>> = repository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun delete(item: ClosetItem) {
        viewModelScope.launch { repository.delete(item) }
    }
}
