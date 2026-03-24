package com.quantumproperty.qcai.service

import android.content.Context
import android.util.Log
import androidx.work.*
import com.quantumproperty.qcai.data.ContextEngine
import com.quantumproperty.qcai.data.OpenClawService
import java.util.concurrent.TimeUnit

class BackgroundSyncWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Log.d("BackgroundSyncWorker", "Starting Context OS Background Sync")
        
        val contextEngine = ContextEngine.getInstance(applicationContext)
        val openClawService = OpenClawService.instance
        
        return try {
            val context = contextEngine.ingest()
            
            // Sync if connected
            if (openClawService.isConnected) {
                openClawService.syncContext(context)
                Log.d("BackgroundSyncWorker", "Sync successful")
            } else {
                Log.d("BackgroundSyncWorker", "Gateway not connected, skipping sync")
            }
            
            Result.success()
        } catch (e: Exception) {
            Log.e("BackgroundSyncWorker", "Sync failed: ${e.message}")
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "com.qcai.context.sync"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<BackgroundSyncWorker>(1, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
            Log.d("BackgroundSyncWorker", "Sync scheduled (Periodic 1h)")
        }
    }
}
