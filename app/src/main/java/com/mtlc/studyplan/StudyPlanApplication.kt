package com.mtlc.studyplan

import android.app.Application

class StudyPlanApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        @Volatile
        private var instance: StudyPlanApplication? = null

        fun getInstance(): StudyPlanApplication {
            return instance ?: throw IllegalStateException("StudyPlanApplication not initialized")
        }
    }
}