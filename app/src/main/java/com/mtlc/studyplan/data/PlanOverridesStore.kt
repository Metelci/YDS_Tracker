package com.mtlc.studyplan.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class PlanOverridesStore(
    private val dataStore: DataStore<Preferences>,
    private val json: Json = Json { ignoreUnknownKeys = true }
) {
    private object Keys {
        val PLAN_OVERRIDES = stringPreferencesKey("plan_overrides_json")
    }

    val overridesFlow: Flow<UserPlanOverrides> = dataStore.data.map { prefs ->
        prefs[Keys.PLAN_OVERRIDES]
            ?.let { runCatching { json.decodeFromString<UserPlanOverrides>(it) }.getOrNull() }
            ?: UserPlanOverrides()
    }

    suspend fun update(transform: (UserPlanOverrides) -> UserPlanOverrides) {
        dataStore.edit { prefs ->
            val current = prefs[Keys.PLAN_OVERRIDES]
                ?.let { runCatching { json.decodeFromString<UserPlanOverrides>(it) }.getOrNull() }
                ?: UserPlanOverrides()
            val next = transform(current)
            prefs[Keys.PLAN_OVERRIDES] = json.encodeToString(next)
        }
    }
}

