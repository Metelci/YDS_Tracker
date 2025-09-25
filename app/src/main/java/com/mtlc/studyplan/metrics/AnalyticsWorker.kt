package com.mtlc.studyplan.metrics

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import android.content.pm.ApplicationInfo

class AnalyticsWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val name = inputData.getString("event") ?: return Result.success()
        val props = inputData.getString("props") ?: ""
        val isDebug = (applicationContext.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        if (isDebug) {
            Log.d("Analytics", "event=$name props=$props")
        }
        // In release, intentionally do nothing (no PII, no external I/O).
        return Result.success()
    }
}
