package com.mtlc.studyplan.data.social

import kotlinx.coroutines.flow.StateFlow

/**
 * Repository contract for the Social Hub. Provides reactive flows so UI can
 * stay in sync while keeping implementation swap-friendly.
 */
interface SocialRepository {
    val profile: StateFlow<SocialProfile>
    val ranks: StateFlow<List<RankEntry>>
    val groups: StateFlow<List<Group>>
    val friends: StateFlow<List<Friend>>
    val awards: StateFlow<List<Award>>

    suspend fun toggleGroupMembership(groupId: String)
    suspend fun shareGroup(groupId: String)
    suspend fun selectAvatar(avatarId: String)
    suspend fun updateWeeklyGoal(hours: Int)
}
