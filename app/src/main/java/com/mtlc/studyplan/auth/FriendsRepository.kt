package com.mtlc.studyplan.auth

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.util.UUID

private val Context.friendsDataStore: DataStore<Preferences> by preferencesDataStore(name = "friends")

@Serializable
private data class StoredFriendRequest(
    val id: String,
    val fromUserId: String,
    val fromEmail: String,
    val fromUsername: String,
    val toEmail: String,
    val status: String,
    val createdAt: Long
)

@Serializable
private data class StoredFriendRelation(
    val id: String,
    val userId: String,
    val friendId: String,
    val friendEmail: String,
    val friendUsername: String,
    val friendXp: Int,
    val friendStreak: Int,
    val createdAt: Long
)

class FriendsRepository(private val context: Context) {

    private val dataStore = context.friendsDataStore
    private val json = Json { ignoreUnknownKeys = true }

    private object Keys {
        val FRIEND_REQUESTS = stringPreferencesKey("friend_requests")
        val FRIENDS = stringPreferencesKey("friends")
    }

    val friendRequests: Flow<List<FriendRequest>> = dataStore.data.map { prefs ->
        val jsonString = prefs[Keys.FRIEND_REQUESTS] ?: "[]"
        try {
            val stored = json.decodeFromString<List<StoredFriendRequest>>(jsonString)
            stored.map { it.toFriendRequest() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    val friends: Flow<List<FriendRelation>> = dataStore.data.map { prefs ->
        val jsonString = prefs[Keys.FRIENDS] ?: "[]"
        try {
            val stored = json.decodeFromString<List<StoredFriendRelation>>(jsonString)
            stored.map { it.toFriendRelation() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun sendFriendRequest(
        currentUserId: String,
        currentUserEmail: String,
        currentUsername: String,
        friendEmail: String
    ): Result<FriendRequest> {
        return try {
            val request = FriendRequest(
                id = UUID.randomUUID().toString(),
                fromUserId = currentUserId,
                fromEmail = currentUserEmail,
                fromUsername = currentUsername,
                toEmail = friendEmail,
                status = FriendRequestStatus.PENDING,
                createdAt = System.currentTimeMillis()
            )

            dataStore.edit { prefs ->
                val current = prefs[Keys.FRIEND_REQUESTS] ?: "[]"
                val list = json.decodeFromString<List<StoredFriendRequest>>(current).toMutableList()
                list.add(request.toStored())
                prefs[Keys.FRIEND_REQUESTS] = json.encodeToString(list)
            }

            Result.success(request)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun acceptFriendRequest(requestId: String): Result<Unit> {
        return try {
            dataStore.edit { prefs ->
                // Update request status
                val requestsJson = prefs[Keys.FRIEND_REQUESTS] ?: "[]"
                val requests = json.decodeFromString<List<StoredFriendRequest>>(requestsJson).toMutableList()
                val index = requests.indexOfFirst { it.id == requestId }
                if (index >= 0) {
                    val request = requests[index]
                    requests[index] = request.copy(status = "ACCEPTED")
                    prefs[Keys.FRIEND_REQUESTS] = json.encodeToString(requests)

                    // Add to friends list
                    val friendsJson = prefs[Keys.FRIENDS] ?: "[]"
                    val friends = json.decodeFromString<List<StoredFriendRelation>>(friendsJson).toMutableList()
                    friends.add(
                        StoredFriendRelation(
                            id = UUID.randomUUID().toString(),
                            userId = request.fromUserId,
                            friendId = UUID.randomUUID().toString(), // In real app, get from backend
                            friendEmail = request.fromEmail,
                            friendUsername = request.fromUsername,
                            friendXp = 0,
                            friendStreak = 0,
                            createdAt = System.currentTimeMillis()
                        )
                    )
                    prefs[Keys.FRIENDS] = json.encodeToString(friends)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun rejectFriendRequest(requestId: String): Result<Unit> {
        return try {
            dataStore.edit { prefs ->
                val json = prefs[Keys.FRIEND_REQUESTS] ?: "[]"
                val list = this.json.decodeFromString<List<StoredFriendRequest>>(json).toMutableList()
                val index = list.indexOfFirst { it.id == requestId }
                if (index >= 0) {
                    list[index] = list[index].copy(status = "REJECTED")
                    prefs[Keys.FRIEND_REQUESTS] = this.json.encodeToString(list)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removeFriend(friendId: String): Result<Unit> {
        return try {
            dataStore.edit { prefs ->
                val json = prefs[Keys.FRIENDS] ?: "[]"
                val list = this.json.decodeFromString<List<StoredFriendRelation>>(json).toMutableList()
                list.removeIf { it.id == friendId }
                prefs[Keys.FRIENDS] = this.json.encodeToString(list)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun FriendRequest.toStored() = StoredFriendRequest(
        id = id,
        fromUserId = fromUserId,
        fromEmail = fromEmail,
        fromUsername = fromUsername,
        toEmail = toEmail,
        status = status.name,
        createdAt = createdAt
    )

    private fun StoredFriendRequest.toFriendRequest() = FriendRequest(
        id = id,
        fromUserId = fromUserId,
        fromEmail = fromEmail,
        fromUsername = fromUsername,
        toEmail = toEmail,
        status = FriendRequestStatus.valueOf(status),
        createdAt = createdAt
    )

    private fun StoredFriendRelation.toFriendRelation() = FriendRelation(
        id = id,
        userId = userId,
        friendId = friendId,
        friendEmail = friendEmail,
        friendUsername = friendUsername,
        friendXp = friendXp,
        friendStreak = friendStreak,
        createdAt = createdAt
    )
}