package com.svd.svdagencies.utils

import android.content.Context
import android.util.Log
import androidx.work.*
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class KeepAliveWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val client = OkHttpClient.Builder()
            .connectTimeout(50, TimeUnit.SECONDS)
            .readTimeout(50, TimeUnit.SECONDS)
            .protocols(listOf(Protocol.HTTP_1_1))
            .build()
            
        val request = Request.Builder()
            .url("https://svd-dqw3.onrender.com/")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Log.d("KeepAliveWorker", "Successfully pinged base URL")
                } else {
                    Log.d("KeepAliveWorker", "Ping failed with code: ${response.code}")
                }
            }
            
            if (!isStopped) {
                // Schedule next execution in 14 minutes
                schedule(applicationContext)
            }
            Result.success()
        } catch (e: Exception) {
            if (isStopped) {
                Log.d("KeepAliveWorker", "Worker stopped, not rescheduling")
                return@withContext Result.failure()
            }
            
            Log.e("KeepAliveWorker", "Error pinging base URL", e)
            // Still schedule next even if this one failed
            schedule(applicationContext)
            Result.failure()
        }
    }

    companion object {
        private const val WORK_NAME = "KeepAliveWork"

        fun schedule(context: Context) {
            val workRequest = OneTimeWorkRequestBuilder<KeepAliveWorker>()
                .setInitialDelay(14, TimeUnit.MINUTES)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()
            
            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.APPEND_OR_REPLACE,
                workRequest
            )
        }
        
        fun startNow(context: Context) {
             val workRequest = OneTimeWorkRequestBuilder<KeepAliveWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()
            
            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.KEEP, // Don't interrupt if already running/scheduled
                workRequest
            )
        }

        fun stop(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            Log.d("KeepAliveWorker", "Stopped keep-alive pings")
        }
    }
}
