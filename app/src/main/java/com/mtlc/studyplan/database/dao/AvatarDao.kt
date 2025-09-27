package com.mtlc.studyplan.database.dao

import androidx.room.*
import com.mtlc.studyplan.database.entities.AvatarEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AvatarDao {

    @Query("SELECT * FROM avatars WHERE userId = :userId AND isActive = 1 ORDER BY uploadedAt DESC LIMIT 1")
    fun getActiveAvatar(userId: String): Flow<AvatarEntity?>

    @Query("SELECT * FROM avatars WHERE userId = :userId ORDER BY uploadedAt DESC")
    fun getAllAvatars(userId: String): Flow<List<AvatarEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAvatar(avatar: AvatarEntity)

    @Query("UPDATE avatars SET isActive = 0 WHERE userId = :userId")
    suspend fun deactivateAllAvatars(userId: String)

    @Query("UPDATE avatars SET isActive = 1 WHERE id = :avatarId")
    suspend fun activateAvatar(avatarId: String)

    @Delete
    suspend fun deleteAvatar(avatar: AvatarEntity)

    @Query("DELETE FROM avatars WHERE userId = :userId AND isActive = 0")
    suspend fun cleanupInactiveAvatars(userId: String)
}