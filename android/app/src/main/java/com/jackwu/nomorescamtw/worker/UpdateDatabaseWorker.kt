package com.jackwu.nomorescamtw.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.jackwu.nomorescamtw.MyApplication

class UpdateDatabaseWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val app = applicationContext as MyApplication
        val repository = app.repository

        val result = repository.updateDatabase()
        
        return if (result.isSuccess) {
            val count = result.getOrNull() ?: 0
            applicationContext.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                .edit()
                .putInt("total_entries", count)
                .putLong("last_db_update_ts", System.currentTimeMillis())
                .apply()
            Result.success()
        } else {
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }
}
