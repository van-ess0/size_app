package com.sizesapp.data.backup

import android.content.Context
import com.sizesapp.data.db.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.time.Instant

private const val BACKUP_FILE_NAME = "sizes_closet_backup.db"
private const val DRIVE_FILES_URL = "https://www.googleapis.com/drive/v3/files"
private const val DRIVE_UPLOAD_URL = "https://www.googleapis.com/upload/drive/v3/files"

@Serializable
private data class DriveFileListResponse(val files: List<DriveFile> = emptyList())

@Serializable
data class DriveFile(val id: String, val name: String? = null, val modifiedTime: String? = null)

data class BackupStatus(val existingBackup: DriveFile?)

/**
 * Backs up the local SQLite database to this app's hidden Drive "appDataFolder"
 * (invisible in the user's normal Drive UI, deleted automatically if the app
 * is uninstalled -- the same mechanism WhatsApp-style backups use) and can
 * restore it back down. One file is kept and overwritten each backup; this is
 * not a multi-version history, just a single always-current snapshot.
 */
class DriveBackupManager(
    private val context: Context,
    private val authManager: GoogleAuthManager,
    private val database: AppDatabase,
) {
    private val client = OkHttpClient()
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun findExistingBackup(accessToken: String): DriveFile? = withContext(Dispatchers.IO) {
        val url = DRIVE_FILES_URL.toHttpUrl().newBuilder()
            .addQueryParameter("spaces", "appDataFolder")
            .addQueryParameter("q", "name = '$BACKUP_FILE_NAME'")
            .addQueryParameter("fields", "files(id,name,modifiedTime)")
            .build()
        val request = Request.Builder().url(url).header("Authorization", "Bearer $accessToken").get().build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@withContext null
            val body = response.body?.string() ?: return@withContext null
            json.decodeFromString<DriveFileListResponse>(body).files.firstOrNull()
        }
    }

    suspend fun backupNow(accessToken: String): Result<DriveFile> = withContext(Dispatchers.IO) {
        runCatching {
            database.checkpoint()
            val dbFile = context.getDatabasePath(AppDatabase.DB_FILE_NAME)
            check(dbFile.exists()) { "Local database file not found, nothing to back up yet." }

            val existing = findExistingBackup(accessToken)
            val boundary = "sizesapp-${System.currentTimeMillis()}"
            val metadataJson = if (existing == null) {
                """{"name":"$BACKUP_FILE_NAME","parents":["appDataFolder"]}"""
            } else {
                """{"name":"$BACKUP_FILE_NAME"}"""
            }

            val body = MultipartBody.Builder(boundary)
                .setType("multipart/related".toMediaType())
                .addPart(metadataJson.toRequestBody("application/json; charset=UTF-8".toMediaType()))
                .addPart(dbFile.asRequestBody("application/x-sqlite3".toMediaType()))
                .build()

            val url = if (existing == null) {
                "$DRIVE_UPLOAD_URL?uploadType=multipart"
            } else {
                "$DRIVE_UPLOAD_URL/${existing.id}?uploadType=multipart"
            }
            val requestBuilder = Request.Builder().url(url).header("Authorization", "Bearer $accessToken")
            val request = if (existing == null) requestBuilder.post(body).build() else requestBuilder.patch(body).build()

            client.newCall(request).execute().use { response ->
                check(response.isSuccessful) { "Drive upload failed: HTTP ${response.code} ${response.body?.string()}" }
                val responseBody = response.body?.string().orEmpty()
                json.decodeFromString<DriveFile>(responseBody)
            }
        }
    }

    suspend fun restoreLatest(accessToken: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val existing = findExistingBackup(accessToken)
                ?: error("No backup found in Drive for this account.")

            val request = Request.Builder()
                .url("$DRIVE_FILES_URL/${existing.id}?alt=media")
                .header("Authorization", "Bearer $accessToken")
                .get()
                .build()

            val downloaded = File.createTempFile("restore_", ".db", context.cacheDir)
            client.newCall(request).execute().use { response ->
                check(response.isSuccessful) { "Drive download failed: HTTP ${response.code}" }
                response.body?.byteStream()?.use { input ->
                    downloaded.outputStream().use { output -> input.copyTo(output) }
                } ?: error("Empty response body from Drive")
            }

            AppDatabase.replaceWithRestoredFile(context, downloaded)
            downloaded.delete()
            Unit
        }
    }

    suspend fun status(accessToken: String): BackupStatus = withContext(Dispatchers.IO) {
        BackupStatus(existingBackup = findExistingBackup(accessToken))
    }
}

fun DriveFile.modifiedInstantOrNull(): Instant? = modifiedTime?.let { runCatching { Instant.parse(it) }.getOrNull() }
