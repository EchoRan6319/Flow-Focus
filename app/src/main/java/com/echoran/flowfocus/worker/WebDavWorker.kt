package com.echoran.flowfocus.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.echoran.flowfocus.data.repository.SettingsRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

@HiltWorker
class WebDavWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val settingsRepository: SettingsRepository,
    private val okHttpClient: OkHttpClient
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val isEnabled = settingsRepository.isWebDavSyncEnabled.first()
        if (!isEnabled) return Result.success()

        val url = settingsRepository.webDavServerUrl.first()
        val user = settingsRepository.webDavUsername.first()
        val pass = settingsRepository.webDavPassword.first()

        if (url.isBlank() || user.isBlank() || pass.isBlank()) return Result.failure()

        val dbFile = applicationContext.getDatabasePath("flow_focus_database")
        if (!dbFile.exists()) return Result.success()

        // Nutstore requires the full path including filename
        val targetUrl = if (url.endsWith("/")) "${url}flow_focus_backup.db" else "$url/flow_focus_backup.db"

        val request = Request.Builder()
            .url(targetUrl)
            .put(dbFile.asRequestBody("application/octet-stream".toMediaType()))
            .header("Authorization", Credentials.basic(user, pass))
            .build()

        return try {
            val response = okHttpClient.newCall(request).execute()
            if (response.isSuccessful) {
                Result.success()
            } else {
                Result.retry()
            }
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
