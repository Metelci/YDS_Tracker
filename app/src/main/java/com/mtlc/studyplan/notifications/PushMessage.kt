package com.mtlc.studyplan.notifications

import com.google.firebase.messaging.RemoteMessage

/**
 * Data class representing a push notification message
 */
data class PushMessage(
    val id: String?,
    val type: PushMessageType,
    val title: String?,
    val body: String?,
    val data: Map<String, String>,
    val priority: PushPriority = PushPriority.NORMAL,
    val ttl: Int? = null,
    val collapseKey: String? = null
) {

    companion object {
        fun fromRemoteMessage(remoteMessage: RemoteMessage): PushMessage {
            val notification = remoteMessage.notification
            val data = remoteMessage.data

            // Determine message type from data or notification
            val type = determineMessageType(data, notification?.title, notification?.body)

            return PushMessage(
                id = remoteMessage.messageId,
                type = type,
                title = notification?.title,
                body = notification?.body,
                data = data,
                priority = PushPriority.NORMAL, // Default to normal priority
                ttl = remoteMessage.ttl,
                collapseKey = remoteMessage.collapseKey
            )
        }

        private fun determineMessageType(
            data: Map<String, String>,
            title: String?,
            body: String?
        ): PushMessageType {
            // Check data payload first
            data["type"]?.let { typeString ->
                return PushMessageType.fromString(typeString)
            }

            // Fallback to content analysis
            return when {
                title?.contains("achievement", ignoreCase = true) == true ||
                body?.contains("achievement", ignoreCase = true) == true -> PushMessageType.ACHIEVEMENT

                title?.contains("exam", ignoreCase = true) == true ||
                body?.contains("exam", ignoreCase = true) == true -> PushMessageType.EXAM_UPDATE

                title?.contains("study", ignoreCase = true) == true ||
                body?.contains("study", ignoreCase = true) == true -> PushMessageType.STUDY_REMINDER

                title?.contains("motivation", ignoreCase = true) == true ||
                body?.contains("motivation", ignoreCase = true) == true -> PushMessageType.MOTIVATIONAL

                else -> PushMessageType.CUSTOM
            }
        }
    }
}

/**
 * Enum representing different types of push messages
 */
enum class PushMessageType {
    STUDY_REMINDER,
    ACHIEVEMENT,
    EXAM_UPDATE,
    MOTIVATIONAL,
    SYSTEM,
    CUSTOM;

    companion object {
        fun fromString(value: String): PushMessageType {
            return try {
                valueOf(value.uppercase())
            } catch (e: IllegalArgumentException) {
                // Invalid enum value - log and return default
                android.util.Log.w("PushMessageType", "Invalid message type: $value, defaulting to CUSTOM")
                CUSTOM // Explicit fallback for invalid values
            }
        }
    }
}

/**
 * Enum representing push message priority
 */
enum class PushPriority {
    NORMAL,
    HIGH
}
