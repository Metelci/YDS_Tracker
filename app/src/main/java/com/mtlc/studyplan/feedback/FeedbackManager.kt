package com.mtlc.studyplan.feedback

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FeedbackManager @Inject constructor() {

    fun provideFeedback(message: String) {
        // Simple feedback implementation
        // Could be expanded to show toast, haptic feedback, etc.
    }

    fun provideSuccessFeedback() {
        provideFeedback("Success!")
    }

    fun provideErrorFeedback(error: String) {
        provideFeedback("Error: $error")
    }
}