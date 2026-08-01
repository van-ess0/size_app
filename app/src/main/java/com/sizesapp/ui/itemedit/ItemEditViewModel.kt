package com.sizesapp.ui.itemedit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sizesapp.data.db.ClosetItem
import com.sizesapp.data.db.ClothingCategory
import com.sizesapp.data.db.FitRating
import com.sizesapp.data.db.SizeSystem
import com.sizesapp.data.repository.ClosetRepository
import com.sizesapp.ui.navigation.Destinations
import com.sizesapp.ui.navigation.Destinations.decodeArg
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ItemEditUiState(
    val itemId: Long? = null,
    val category: ClothingCategory = ClothingCategory.SHOES,
    val brand: String = "",
    val sizeLabel: String = "",
    val sizeSystem: SizeSystem = SizeSystem.EU,
    val fitRating: FitRating = FitRating.TRUE_TO_SIZE,
    val notes: String = "",
    val rawOcrText: String? = null,
    val photoPath: String? = null,
    val isSaved: Boolean = false,
)

class ItemEditViewModel(
    private val repository: ClosetRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _uiState = MutableStateFlow(initialStateFrom(savedStateHandle))
    val uiState: StateFlow<ItemEditUiState> = _uiState.asStateFlow()

    init {
        val itemId = savedStateHandle.get<String>(Destinations.ARG_ITEM_ID)?.toLongOrNull()
        if (itemId != null) {
            viewModelScope.launch {
                repository.getById(itemId)?.let { item ->
                    _uiState.value = ItemEditUiState(
                        itemId = item.id,
                        category = item.category,
                        brand = item.brand,
                        sizeLabel = item.sizeLabel,
                        sizeSystem = item.sizeSystem,
                        fitRating = item.fitRating,
                        notes = item.notes,
                        rawOcrText = item.rawOcrText,
                        photoPath = item.photoPath,
                    )
                }
            }
        }
    }

    fun updateCategory(category: ClothingCategory) { _uiState.value = _uiState.value.copy(category = category) }
    fun updateBrand(brand: String) { _uiState.value = _uiState.value.copy(brand = brand) }
    fun updateSizeLabel(sizeLabel: String) { _uiState.value = _uiState.value.copy(sizeLabel = sizeLabel) }
    fun updateSizeSystem(sizeSystem: SizeSystem) { _uiState.value = _uiState.value.copy(sizeSystem = sizeSystem) }
    fun updateFitRating(fitRating: FitRating) { _uiState.value = _uiState.value.copy(fitRating = fitRating) }
    fun updateNotes(notes: String) { _uiState.value = _uiState.value.copy(notes = notes) }

    fun save() {
        val state = _uiState.value
        if (state.brand.isBlank() || state.sizeLabel.isBlank()) return
        viewModelScope.launch {
            repository.save(
                ClosetItem(
                    id = state.itemId ?: 0,
                    category = state.category,
                    brand = state.brand.trim(),
                    sizeLabel = state.sizeLabel.trim(),
                    sizeSystem = state.sizeSystem,
                    fitRating = state.fitRating,
                    notes = state.notes.trim(),
                    rawOcrText = state.rawOcrText,
                    photoPath = state.photoPath,
                ),
            )
            _uiState.value = state.copy(isSaved = true)
        }
    }

    fun delete() {
        val state = _uiState.value
        val id = state.itemId ?: return
        viewModelScope.launch {
            repository.getById(id)?.let { repository.delete(it) }
            _uiState.value = state.copy(isSaved = true)
        }
    }

    companion object {
        private fun initialStateFrom(savedStateHandle: SavedStateHandle): ItemEditUiState {
            val brand = savedStateHandle.get<String>(Destinations.ARG_BRAND)?.decodeArg().orEmpty()
            val sizeLabel = savedStateHandle.get<String>(Destinations.ARG_SIZE_LABEL)?.decodeArg().orEmpty()
            val sizeSystem = savedStateHandle.get<String>(Destinations.ARG_SIZE_SYSTEM)?.decodeArg()
                ?.let { runCatching { SizeSystem.valueOf(it) }.getOrNull() } ?: SizeSystem.EU
            val rawText = savedStateHandle.get<String>(Destinations.ARG_RAW_TEXT)?.decodeArg()
            val photoPath = savedStateHandle.get<String>(Destinations.ARG_PHOTO_PATH)?.decodeArg()
            return ItemEditUiState(
                brand = brand,
                sizeLabel = sizeLabel,
                sizeSystem = sizeSystem,
                rawOcrText = rawText,
                photoPath = photoPath,
            )
        }
    }
}
