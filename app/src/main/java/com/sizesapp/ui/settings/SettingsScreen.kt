package com.sizesapp.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val state by viewModel.uiState.collectAsState()

    val consentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        viewModel.onConsentResult(result.resultCode, result.data)
    }

    LaunchedEffect(Unit) {
        viewModel.consentRequests.collect { pendingIntent ->
            consentLauncher.launch(IntentSenderRequest.Builder(pendingIntent.intentSender).build())
        }
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Settings") }) }) { padding ->
        Column(
            modifier = Modifier.fillMaxWidth().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Google Drive backup", style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
            Text(
                "Your closet is stored only on this device. Backing up saves an encrypted-in-transit copy " +
                    "to a hidden, app-only folder in your Google Drive -- like WhatsApp's chat backup, nobody " +
                    "else (including you, from the Drive app) can browse it directly.",
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = viewModel::onBackupClick, enabled = !state.isWorking) {
                    Text("Back up now")
                }
                OutlinedButton(onClick = viewModel::onRestoreClick, enabled = !state.isWorking) {
                    Text("Restore")
                }
            }

            if (state.isWorking) CircularProgressIndicator()
            state.message?.let { Text(it) }
            state.lastBackupAt?.let { Text("Last backup: $it") }

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Automatic daily backup")
                Switch(checked = state.periodicBackupEnabled, onCheckedChange = viewModel::setPeriodicBackupEnabled)
            }
        }
    }
}
