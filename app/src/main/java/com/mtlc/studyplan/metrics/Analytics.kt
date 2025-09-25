package com.mtlc.studyplan.metrics

import android.content.Context
import android.util.Log
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager

object Analytics {
    fun track(context: Context, name: String, props: Map<String, String> = emptyMap()) {
        try {
            val data = Data.Builder()
                .putString("event", name)
                .putString("props", props.entries.joinToString(";") { "${it.key}=${it.value}" })
                .build()
            val req = OneTimeWorkRequestBuilder<AnalyticsWorker>()
                .setInputData(data)
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build()
            WorkManager.getInstance(context).enqueue(req)
        } catch (t: Throwable) {
            // Last resort: log in debug builds; never crash due to metrics
            Log.d("Analytics", "track failed: $name", t)
        }
    }
}

