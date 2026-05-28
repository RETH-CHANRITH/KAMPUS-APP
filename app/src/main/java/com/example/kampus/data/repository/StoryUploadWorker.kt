package com.example.kampus.data.repository

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.net.Uri

class StoryUploadWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val fileUri = inputData.getString("fileUri") ?: return@withContext Result.failure()
            val caption = inputData.getString("caption") ?: ""
            val privacy = inputData.getString("privacy") ?: "friends"

            val repo = StoryRepository()
            val res = repo.uploadImageStory(applicationContext, Uri.parse(fileUri), caption, privacy) { /* progress ignored in background */ }
            return@withContext if (res.isSuccess) Result.success() else Result.retry()
        } catch (e: Exception) {
            return@withContext Result.retry()
        }
    }
}
