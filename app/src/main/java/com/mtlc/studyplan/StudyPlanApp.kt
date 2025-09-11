package com.mtlc.studyplan

import android.app.Application
import com.mtlc.studyplan.metrics.Analytics

class StudyPlanApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Analytics.track(this, "app_open")
    }
}

