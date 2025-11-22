package com.mtlc.studyplan.utils

import android.content.Context

/**
 * Lightweight holder for application context so non-Android classes can reach localized resources.
 */
object ApplicationContextProvider {
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    fun getString(resId: Int): String? = appContext?.getString(resId)
}
