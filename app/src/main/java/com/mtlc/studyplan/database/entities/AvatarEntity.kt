package com.mtlc.studyplan.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "avatars")
data class AvatarEntity(
    @PrimaryKey
    val id: String,
    val userId: String, // For multi-user support
    val fileName: String,
    val filePath: String,
    val originalUri: String,
    val fileSize: Long,
    val width: Int,
    val height: Int,
    val mimeType: String,
    val uploadedAt: Long,
    val isActive: Boolean = true
)