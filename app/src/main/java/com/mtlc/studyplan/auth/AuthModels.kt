package com.mtlc.studyplan.auth

data class User(
    val id: String,
    val email: String,
    val username: String,
    val xp: Int = 0,
    val streak: Int = 0,
    val awards: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)

data class FriendRequest(
    val id: String,
    val fromUserId: String,
    val fromEmail: String,
    val fromUsername: String,
    val toEmail: String,
    val status: FriendRequestStatus = FriendRequestStatus.PENDING,
    val createdAt: Long = System.currentTimeMillis()
)

enum class FriendRequestStatus {
    PENDING, ACCEPTED, REJECTED
}

data class FriendRelation(
    val id: String,
    val userId: String,
    val friendId: String,
    val friendEmail: String,
    val friendUsername: String,
    val friendXp: Int = 0,
    val friendStreak: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)