package com.mtlc.studyplan.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AppPrefsRepository(private val dataStore: DataStore<Preferences>) {
    private object Keys {
        val HAS_SEEN_WELCOME = booleanPreferencesKey("has_seen_welcome")
    }

    val hasSeenWelcome: Flow<Boolean> = dataStore.data.map { it[Keys.HAS_SEEN_WELCOME] ?: false }

    suspend fun setSeenWelcome() {
        dataStore.edit { it[Keys.HAS_SEEN_WELCOME] = true }
    }
}

