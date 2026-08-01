package com.sizesapp.ui.scan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sizesapp.ocr.ImageDownscaler
import com.sizesapp.ocr.LabelParser
import com.sizesapp.ocr.OcrTextRecognizer
import com.sizesapp.ocr.ParsedLabel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class ScanUiState(
    val isProcessing: Boolean = false,
    val error: String? = null,
    val result: ParsedLabel? = null,
)

class ScanViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ScanUiState())
    val uiState: StateFlow<ScanUiState> = _uiState.asStateFlow()

    fun processCapturedPhoto(photoFile: File) {
        _uiState.value = ScanUiState(isProcessing = true)
        viewModelScope.launch {
            runCatching {
                // Shrinks the file in place before OCR reads it -- this is also
                // the exact file that becomes the item's permanently stored
                // photo, so it keeps both OCR memory use and on-device storage down.
                withContext(Dispatchers.IO) { ImageDownscaler.downscaleInPlace(photoFile) }
                OcrTextRecognizer.recognize(photoFile)
            }
                .onSuccess { text -> _uiState.value = ScanUiState(result = LabelParser.parse(text)) }
                .onFailure { e -> _uiState.value = ScanUiState(error = e.message ?: "Couldn't read the label, try again.") }
        }
    }

    fun consumeResult() {
        _uiState.value = ScanUiState()
    }

    fun reportCaptureError(message: String) {
        _uiState.value = ScanUiState(error = message)
    }
}
