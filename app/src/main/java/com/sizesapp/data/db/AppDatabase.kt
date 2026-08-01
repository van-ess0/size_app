package com.sizesapp.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [ClosetItem::class], version = 1, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {

    abstract fun closetDao(): ClosetDao

    /** Flushes the WAL journal into the main db file so it's safe to copy for backup. */
    fun checkpoint() {
        openHelper.writableDatabase.execSQL("PRAGMA wal_checkpoint(FULL)")
    }

    companion object {
        const val DB_FILE_NAME = "sizes.db"

        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: build(context).also { instance = it }
            }

        private fun build(context: Context): AppDatabase = Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            DB_FILE_NAME,
        ).build()

        /** Closes the current instance so [restoredFile] can safely replace the db on disk, then reopens it. */
        @Synchronized
        fun replaceWithRestoredFile(context: Context, restoredFile: java.io.File): AppDatabase {
            instance?.close()
            val dbFile = context.getDatabasePath(DB_FILE_NAME)
            context.deleteDatabase(DB_FILE_NAME)
            restoredFile.copyTo(dbFile, overwrite = true)
            return build(context).also { instance = it }
        }
    }
}
