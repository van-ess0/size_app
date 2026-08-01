package com.sizesapp.ui.recommend

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sizesapp.data.db.ClothingCategory
import com.sizesapp.data.repository.ClosetRepository
import com.sizesapp.data.sizing.Recommendation
import com.sizesapp.data.sizing.SizeRecommender
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class RecommendUiState(
    val category: ClothingCategory = ClothingCategory.SHOES,
    val brand: String = "",
    val isLoading: Boolean = false,
    val result: Recommendation? = null,
)

class RecommendViewModel(
    private val recommender: SizeRecommender,
    repository: ClosetRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecommendUiState())
    val uiState: StateFlow<RecommendUiState> = _uiState.asStateFlow()

    val knownBrands: StateFlow<List<String>> = repository.observeKnownBrands()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun updateCategory(category: ClothingCategory) {
        _uiState.value = _uiState.value.copy(category = category, result = null)
    }

    fun updateBrand(brand: String) {
        _uiState.value = _uiState.value.copy(brand = brand, result = null)
    }

    fun getRecommendation() {
        val state = _uiState.value
        if (state.brand.isBlank()) return
        _uiState.value = state.copy(isLoading = true)
        viewModelScope.launch {
            val result = recommender.recommend(state.category, state.brand.trim())
            _uiState.value = _uiState.value.copy(isLoading = false, result = result)
        }
    }
}
