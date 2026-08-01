package com.sizesapp

import android.app.Application
import com.sizesapp.data.backup.DriveBackupManager
import com.sizesapp.data.backup.GoogleAuthManager
import com.sizesapp.data.db.AppDatabase
import com.sizesapp.data.repository.ClosetRepository
import com.sizesapp.data.sizing.SizeRecommender

/** Hand-rolled DI container -- deliberately no DI framework for an app this size. */
class AppContainer(private val app: Application) {
    val database by lazy { AppDatabase.getInstance(app) }
    val repository by lazy { ClosetRepository(database.closetDao()) }
    val recommender by lazy { SizeRecommender(repository) }
    val authManager by lazy { GoogleAuthManager(app) }
    val driveBackupManager by lazy { DriveBackupManager(app, authManager, database) }
}

class SizesApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
