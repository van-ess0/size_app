package com.sizesapp.ui.common

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.sizesapp.SizesApplication
import com.sizesapp.ui.home.HomeViewModel
import com.sizesapp.ui.itemedit.ItemEditViewModel
import com.sizesapp.ui.recommend.RecommendViewModel
import com.sizesapp.ui.scan.ScanViewModel
import com.sizesapp.ui.settings.SettingsViewModel

object AppViewModelProvider {
    val Factory = viewModelFactory {
        initializer {
            HomeViewModel(sizesApplication().container.repository)
        }
        initializer {
            ScanViewModel()
        }
        initializer {
            ItemEditViewModel(sizesApplication().container.repository, createSavedStateHandle())
        }
        initializer {
            RecommendViewModel(
                sizesApplication().container.recommender,
                sizesApplication().container.repository,
            )
        }
        initializer {
            SettingsViewModel(
                sizesApplication(),
                sizesApplication().container.authManager,
                sizesApplication().container.driveBackupManager,
            )
        }
    }
}

fun CreationExtras.sizesApplication(): SizesApplication =
    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as SizesApplication
