package com.sizesapp.data.backup

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.sizesapp.SizesApplication
import java.util.concurrent.TimeUnit

private const val PERIODIC_BACKUP_WORK_NAME = "periodic_drive_backup"

class BackupWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val container = (applicationContext as SizesApplication).container
        val authOutcome = container.authManager.requestDriveAuthorization()
        val token = (authOutcome as? AuthorizationOutcome.Granted)?.accessToken
            ?: return Result.retry() // e.g. consent needs re-granting in the foreground UI

        val backupResult = container.driveBackupManager.backupNow(token)
        return if (backupResult.isSuccess) Result.success() else Result.retry()
    }

    companion object {
        fun enablePeriodicBackup(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.UNMETERED)
                .build()
            val request = PeriodicWorkRequestBuilder<BackupWorker>(1, TimeUnit.DAYS)
                .setConstraints(constraints)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC_BACKUP_WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request,
            )
        }

        fun disablePeriodicBackup(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(PERIODIC_BACKUP_WORK_NAME)
        }
    }
}
