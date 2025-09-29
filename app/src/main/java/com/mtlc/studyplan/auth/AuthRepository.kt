package com.mtlc.studyplan.auth

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

private val Context.authDataStore: DataStore<Preferences> by preferencesDataStore(name = "auth")

class AuthRepository(private val context: Context) {

    private val dataStore = context.authDataStore

    private object Keys {
        val USER_ID = stringPreferencesKey("user_id")
        val EMAIL = stringPreferencesKey("email")
        val USERNAME = stringPreferencesKey("username")
        val XP = intPreferencesKey("xp")
        val STREAK = intPreferencesKey("streak")
        val CREATED_AT = longPreferencesKey("created_at")
    }

    val currentUser: Flow<User?> = dataStore.data.map { prefs ->
        val userId = prefs[Keys.USER_ID]
        val email = prefs[Keys.EMAIL]
        val username = prefs[Keys.USERNAME]

        if (userId != null && email != null && username != null) {
            User(
                id = userId,
                email = email,
                username = username,
                xp = prefs[Keys.XP] ?: 0,
                streak = prefs[Keys.STREAK] ?: 0,
                createdAt = prefs[Keys.CREATED_AT] ?: System.currentTimeMillis()
            )
        } else {
            null
        }
    }

    suspend fun login(email: String, username: String): Result<User> {
        return try {
            // In a real app, this would validate with a backend
            // For now, we'll create a local user
            val userId = UUID.randomUUID().toString()
            val user = User(
                id = userId,
                email = email,
                username = username,
                xp = 0,
                streak = 0,
                createdAt = System.currentTimeMillis()
            )

            dataStore.edit { prefs ->
                prefs[Keys.USER_ID] = user.id
                prefs[Keys.EMAIL] = user.email
                prefs[Keys.USERNAME] = user.username
                prefs[Keys.XP] = user.xp
                prefs[Keys.STREAK] = user.streak
                prefs[Keys.CREATED_AT] = user.createdAt
            }

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateStats(xp: Int, streak: Int) {
        dataStore.edit { prefs ->
            prefs[Keys.XP] = xp
            prefs[Keys.STREAK] = streak
        }
    }

    suspend fun logout() {
        dataStore.edit { prefs ->
            prefs.clear()
        }
    }

    companion object {
        fun isValidEmail(email: String): Boolean {
            return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
        }

        fun isValidUsername(username: String): Boolean {
            return username.length >= 3 && username.matches(Regex("^[a-zA-Z0-9_]+$"))
        }
    }
}