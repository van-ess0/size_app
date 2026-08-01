package com.sizesapp.ui.settings

import android.app.Application
import android.app.PendingIntent
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sizesapp.data.backup.AuthorizationOutcome
import com.sizesapp.data.backup.BackupWorker
import com.sizesapp.data.backup.DriveBackupManager
import com.sizesapp.data.backup.GoogleAuthManager
import com.sizesapp.data.backup.modifiedInstantOrNull
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private enum class PendingAction { BACKUP, RESTORE }

data class SettingsUiState(
    val isWorking: Boolean = false,
    val message: String? = null,
    val lastBackupAt: String? = null,
    val periodicBackupEnabled: Boolean = false,
)

class SettingsViewModel(
    private val app: Application,
    private val authManager: GoogleAuthManager,
    private val driveBackupManager: DriveBackupManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _consentRequests = Channel<PendingIntent>(Channel.CONFLATED)
    val consentRequests = _consentRequests.receiveAsFlow()

    private var pendingAction: PendingAction? = null

    fun onBackupClick() {
        _uiState.value = _uiState.value.copy(isWorking = true, message = null)
        viewModelScope.launch { authorizeAndRun(PendingAction.BACKUP) }
    }

    fun onRestoreClick() {
        _uiState.value = _uiState.value.copy(isWorking = true, message = null)
        viewModelScope.launch { authorizeAndRun(PendingAction.RESTORE) }
    }

    fun setPeriodicBackupEnabled(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(periodicBackupEnabled = enabled)
        if (enabled) BackupWorker.enablePeriodicBackup(app) else BackupWorker.disablePeriodicBackup(app)
    }

    fun onConsentResult(resultCode: Int, data: Intent?) {
        viewModelScope.launch {
            val outcome = authManager.outcomeFromActivityResult(resultCode, data)
            val action = pendingAction
            pendingAction = null
            if (outcome is AuthorizationOutcome.Granted && action != null) {
                runAction(action, outcome.accessToken)
            } else {
                _uiState.value = _uiState.value.copy(isWorking = false, message = "Google Drive access was not granted.")
            }
        }
    }

    private suspend fun authorizeAndRun(action: PendingAction) {
        when (val outcome = authManager.requestDriveAuthorization()) {
            is AuthorizationOutcome.Granted -> runAction(action, outcome.accessToken)
            is AuthorizationOutcome.ConsentRequired -> {
                pendingAction = action
                _consentRequests.trySend(outcome.pendingIntent)
            }
            is AuthorizationOutcome.Failed -> {
                _uiState.value = _uiState.value.copy(isWorking = false, message = "Authorization failed: ${outcome.message}")
            }
        }
    }

    private suspend fun runAction(action: PendingAction, accessToken: String) {
        when (action) {
            PendingAction.BACKUP -> {
                driveBackupManager.backupNow(accessToken)
                    .onSuccess { file ->
                        val time = file.modifiedInstantOrNull()
                            ?.let { DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).withZone(java.time.ZoneId.systemDefault()).format(it) }
                        _uiState.value = _uiState.value.copy(isWorking = false, message = "Backup complete.", lastBackupAt = time)
                    }
                    .onFailure { e ->
                        _uiState.value = _uiState.value.copy(isWorking = false, message = "Backup failed: ${e.message}")
                    }
            }
            PendingAction.RESTORE -> {
                driveBackupManager.restoreLatest(accessToken)
                    .onSuccess {
                        _uiState.value = _uiState.value.copy(isWorking = false, message = "Restore complete. Restart the app to see restored items.")
                    }
                    .onFailure { e ->
                        _uiState.value = _uiState.value.copy(isWorking = false, message = "Restore failed: ${e.message}")
                    }
            }
        }
    }
}
