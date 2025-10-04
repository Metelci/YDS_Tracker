package com.mtlc.studyplan.testutil

import com.google.firebase.messaging.RemoteMessage

// Provide missing putData as an alias to addData for test sources
fun RemoteMessage.Builder.putData(key: String, value: String?): RemoteMessage.Builder {
    return if (value == null) this else this.addData(key, value)
}

