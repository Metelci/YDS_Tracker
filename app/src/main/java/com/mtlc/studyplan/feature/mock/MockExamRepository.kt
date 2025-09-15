package com.mtlc.studyplan.feature.mock

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.mockExamDataStore: DataStore<Preferences> by preferencesDataStore(name = "yds_mock_exam")

class MockExamRepository(private val ds: DataStore<Preferences>) {
    private object Keys {
        val REMAINING = intPreferencesKey("remaining_seconds")
        val CURRENT_INDEX = intPreferencesKey("current_index")
        val MARKED = stringSetPreferencesKey("marked_ids")
    }

    data class Snapshot(val remainingSeconds: Int, val currentIndex: Int, val marked: Set<Int>)

    val snapshotFlow: Flow<Snapshot> = ds.data.map { p ->
        Snapshot(
            remainingSeconds = p[Keys.REMAINING] ?: 180 * 60,
            currentIndex = p[Keys.CURRENT_INDEX] ?: 0,
            marked = (p[Keys.MARKED] ?: emptySet()).mapNotNull { it.toIntOrNull() }.toSet()
        )
    }

    suspend fun save(snapshot: Snapshot) {
        ds.edit { e ->
            e[Keys.REMAINING] = snapshot.remainingSeconds
            e[Keys.CURRENT_INDEX] = snapshot.currentIndex
            e[Keys.MARKED] = snapshot.marked.map { it.toString() }.toSet()
        }
    }
}

