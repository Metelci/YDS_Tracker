package com.mtlc.studyplan.workflows

import android.content.Context
import com.mtlc.studyplan.shared.SharedAppViewModel
import com.mtlc.studyplan.data.Achievement
import com.mtlc.studyplan.data.social.SocialRepository
import com.mtlc.studyplan.utils.NetworkHelper
import com.mtlc.studyplan.utils.ToastManager
import com.mtlc.studyplan.offline.OfflineActionManager
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.util.UUID

/**
 * Complete Social Interaction Journey Implementation
 * Handles the entire workflow for social sharing with offline support
 */
class SocialInteractionWorkflow(
    private val sharedViewModel: SharedAppViewModel,
    private val socialRepository: SocialRepository,
    private val context: Context
) {

    suspend fun executeShareAchievement(achievement: Achievement): WorkflowResult {
        return try {
            // Step 1: Prepare sharing content
            val shareContent = prepareShareContent(achievement)
            showPreparationFeedback()

            if (NetworkHelper.isOnline()) {
                // Online flow
                executeOnlineSharing(shareContent, achievement)
            } else {
                // Offline flow
                executeOfflineSharing(shareContent, achievement)
            }
        } catch (e: NetworkException) {
            // Queue for later if offline
            queueForOfflineSharing(achievement)
            showOfflineQueuedFeedback()
            WorkflowResult.QueuedForLater
        } catch (e: Exception) {
            handleWorkflowError(e, "Social Sharing")
            WorkflowResult.Error(e.message ?: "Sharing failed")
        }
    }

    private suspend fun executeOnlineSharing(shareContent: ShareContent, achievement: Achievement): WorkflowResult {
        // Step 2: Post to social feed
        val postResult = socialRepository.shareAchievement(shareContent)
        delay(1000) // Simulate network delay

        // Step 3: Update social stats
        updateSocialStats(postResult)

        // Step 4: Notify friends
        notifyFriendsOfAchievement(achievement, postResult.id)

        // Step 5: Show success feedback
        showShareSuccessFeedback(postResult)

        return WorkflowResult.Success(postResult)
    }

    private suspend fun executeOfflineSharing(shareContent: ShareContent, achievement: Achievement): WorkflowResult {
        // Queue the sharing action for when online
        val offlineAction = OfflineAction.ShareAchievement(
            achievementId = achievement.id,
            shareContent = shareContent,
            timestamp = System.currentTimeMillis()
        )

        OfflineActionManager.queueAction(context, offlineAction)
        showOfflineQueuedFeedback()

        return WorkflowResult.QueuedForLater
    }

    suspend fun executeJoinGroup(groupId: String, groupName: String): WorkflowResult {
        return try {
            if (NetworkHelper.isOnline()) {
                // Step 1: Send join request
                showJoinGroupFeedback("Sending join request...")
                val joinResult = socialRepository.joinGroup(groupId)
                delay(800) // Simulate network delay

                // Step 2: Update local group membership
                updateLocalGroupMembership(groupId, true)

                // Step 3: Show success feedback
                showJoinGroupSuccessFeedback(groupName)

                WorkflowResult.Success(joinResult)
            } else {
                // Queue for offline
                val offlineAction = OfflineAction.JoinGroup(groupId, System.currentTimeMillis())
                OfflineActionManager.queueAction(context, offlineAction)
                showOfflineActionQueued("Group join request will be sent when online")
                WorkflowResult.QueuedForLater
            }
        } catch (e: Exception) {
            handleWorkflowError(e, "Group Join")
            WorkflowResult.Error(e.message ?: "Failed to join group")
        }
    }

    suspend fun executeAddFriend(friendId: String, friendName: String): WorkflowResult {
        return try {
            if (NetworkHelper.isOnline()) {
                // Step 1: Send friend request
                showAddFriendFeedback("Sending friend request...")
                val friendResult = socialRepository.addFriend(friendId)
                delay(600) // Simulate network delay

                // Step 2: Update local friends list
                updateLocalFriendsList(friendId, FriendStatus.PENDING)

                // Step 3: Show success feedback
                showAddFriendSuccessFeedback(friendName)

                WorkflowResult.Success(friendResult)
            } else {
                // Queue for offline
                val offlineAction = OfflineAction.AddFriend(friendId, System.currentTimeMillis())
                OfflineActionManager.queueAction(context, offlineAction)
                showOfflineActionQueued("Friend request will be sent when online")
                WorkflowResult.QueuedForLater
            }
        } catch (e: Exception) {
            handleWorkflowError(e, "Add Friend")
            WorkflowResult.Error(e.message ?: "Failed to add friend")
        }
    }

    private fun prepareShareContent(achievement: Achievement): ShareContent {
        val motivationalMessages = listOf(
            "Just achieved something amazing! 🎉",
            "Another milestone reached! 💪",
            "Progress never stops! 🚀",
            "Hard work paying off! ⭐",
            "Celebrating this victory! 🏆"
        )

        return ShareContent(
            id = UUID.randomUUID().toString(),
            achievementId = achievement.id,
            title = achievement.title,
            description = achievement.description,
            message = motivationalMessages.random(),
            timestamp = System.currentTimeMillis(),
            shareType = ShareType.ACHIEVEMENT
        )
    }

    private suspend fun updateSocialStats(postResult: SocialPostResult) {
        val currentStats = sharedViewModel.studyStats.value
        val updatedStats = currentStats.copy(
            totalXP = currentStats.totalXP + 10 // Bonus points for sharing
        )
        sharedViewModel.updateStudyStats(updatedStats)
    }

    private suspend fun notifyFriendsOfAchievement(achievement: Achievement, postId: String) {
        // This would typically send push notifications to friends
        // For now, we'll just log the action
        android.util.Log.d("SocialWorkflow", "Notified friends about achievement: ${achievement.title}")
    }

    private suspend fun updateLocalGroupMembership(groupId: String, isMember: Boolean) {
        // Update local group membership status
        // This would typically update the local database
        android.util.Log.d("SocialWorkflow", "Updated group membership: $groupId = $isMember")
    }

    private suspend fun updateLocalFriendsList(friendId: String, status: FriendStatus) {
        // Update local friends list
        // This would typically update the local database
        android.util.Log.d("SocialWorkflow", "Updated friend status: $friendId = $status")
    }

    private suspend fun queueForOfflineSharing(achievement: Achievement) {
        val offlineAction = OfflineAction.ShareAchievement(
            achievementId = achievement.id,
            shareContent = prepareShareContent(achievement),
            timestamp = System.currentTimeMillis()
        )
        OfflineActionManager.queueAction(context, offlineAction)
    }

    // Feedback methods
    private fun showPreparationFeedback() {
        ToastManager.showInfo("Preparing to share... 📤")
    }

    private fun showShareSuccessFeedback(postResult: SocialPostResult) {
        ToastManager.showSuccess("Achievement shared successfully! 🎉\n${postResult.likes} friends will see this")
    }

    private fun showOfflineQueuedFeedback() {
        ToastManager.showWarning("Offline: Achievement will be shared when you're back online 📱")
    }

    private fun showJoinGroupFeedback(message: String) {
        ToastManager.showInfo(message)
    }

    private fun showJoinGroupSuccessFeedback(groupName: String) {
        ToastManager.showSuccess("Successfully joined $groupName! 👥")
    }

    private fun showAddFriendFeedback(message: String) {
        ToastManager.showInfo(message)
    }

    private fun showAddFriendSuccessFeedback(friendName: String) {
        ToastManager.showSuccess("Friend request sent to $friendName! 👋")
    }

    private fun showOfflineActionQueued(message: String) {
        ToastManager.showWarning(message)
    }

    private fun handleWorkflowError(error: Exception, operation: String) {
        val errorMessage = "Failed to complete $operation: ${error.message ?: "Unknown error"}"
        ToastManager.showError(errorMessage)
        android.util.Log.e("SocialInteractionWorkflow", errorMessage, error)
    }
}

// Data classes and enums for social workflows
data class ShareContent(
    val id: String,
    val achievementId: String,
    val title: String,
    val description: String,
    val message: String,
    val timestamp: Long,
    val shareType: ShareType
)

data class SocialPostResult(
    val id: String,
    val shareContent: ShareContent,
    val likes: Int = 0,
    val comments: Int = 0,
    val shares: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)

enum class ShareType {
    ACHIEVEMENT, PROGRESS, GOAL_COMPLETION, STREAK_MILESTONE
}

enum class FriendStatus {
    PENDING, ACCEPTED, DECLINED, BLOCKED
}

sealed class OfflineAction {
    data class ShareAchievement(
        val achievementId: String,
        val shareContent: ShareContent,
        val timestamp: Long
    ) : OfflineAction()

    data class JoinGroup(
        val groupId: String,
        val timestamp: Long
    ) : OfflineAction()

    data class AddFriend(
        val friendId: String,
        val timestamp: Long
    ) : OfflineAction()
}

class NetworkException(message: String, cause: Throwable? = null) : Exception(message, cause)