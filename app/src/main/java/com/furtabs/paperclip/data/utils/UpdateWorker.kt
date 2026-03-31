package com.furtabs.paperclip.data.utils

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.furtabs.paperclip.tracking.NotificationHelper

class UpdateWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val updateManager = UpdateManager()
        val notificationHelper = NotificationHelper(applicationContext)

        return try {
            val newRelease = updateManager.checkForUpdate()

            if (newRelease != null) {
                notificationHelper.showUpdateNotification(
                    version = newRelease.tagName,
                    url = newRelease.htmlUrl
                )
            }
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}