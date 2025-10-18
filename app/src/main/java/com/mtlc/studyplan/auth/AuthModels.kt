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
