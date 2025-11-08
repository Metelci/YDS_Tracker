package com.mtlc.studyplan.settings.data

import android.content.SharedPreferences
import androidx.annotation.VisibleForTesting
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.MutablePreferences
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * SharedPreferences implementation backed by DataStore.
 *
 * This lets legacy SharedPreferences-based code continue to function while the underlying
 * persistence is handled by DataStore. Changes are propagated through the usual callback
 * mechanism so existing listeners keep working.
 */
class DataStoreBackedPreferences(
    private val dataStore: DataStore<Preferences>
) : SharedPreferences {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    @Volatile
    private var cache: Map<String, Any?> = emptyMap()

    private val listeners = CopyOnWriteArraySet<SharedPreferences.OnSharedPreferenceChangeListener>()
    private val initialized = AtomicBoolean(false)

    init {
        scope.launch {
            var previousSnapshot: Map<String, Any?> = emptyMap()
            dataStore.data
                .catch { emit(emptyPreferences()) }
                .map { prefs -> prefs.asMap().mapKeys { it.key.name } }
                .collect { snapshot ->
                    cache = snapshot
                    if (initialized.getAndSet(true)) {
                        val changedKeys = (snapshot.keys + previousSnapshot.keys)
                            .distinct()
                            .filter { snapshot[it] != previousSnapshot[it] }
                        if (changedKeys.isNotEmpty()) {
                            notifyListeners(changedKeys)
                        }
                    }
                    previousSnapshot = snapshot
                }
        }
    }

    override fun getAll(): MutableMap<String, *> = cache.toMutableMap()

    override fun getString(key: String?, defValue: String?): String? {
        key ?: return defValue
        return cache[key] as? String ?: defValue
    }

    override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? {
        key ?: return defValues
        val value = cache[key] as? Set<*>
        return value?.filterIsInstance<String>()?.toMutableSet() ?: defValues
    }

    override fun getInt(key: String?, defValue: Int): Int {
        key ?: return defValue
        return (cache[key] as? Int) ?: defValue
    }

    override fun getLong(key: String?, defValue: Long): Long {
        key ?: return defValue
        return (cache[key] as? Long) ?: defValue
    }

    override fun getFloat(key: String?, defValue: Float): Float {
        key ?: return defValue
        return when (val value = cache[key]) {
            is Float -> value
            is Double -> value.toFloat()
            else -> defValue
        }
    }

    override fun getBoolean(key: String?, defValue: Boolean): Boolean {
        key ?: return defValue
        return (cache[key] as? Boolean) ?: defValue
    }

    override fun contains(key: String?): Boolean {
        key ?: return false
        return cache.containsKey(key)
    }

    override fun edit(): SharedPreferences.Editor = Editor()

    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {
        listener ?: return
        listeners.add(listener)
    }

    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {
        listener ?: return
        listeners.remove(listener)
    }

    fun close() {
        scope.cancel()
        listeners.clear()
    }

    @VisibleForTesting
    internal fun snapshot(): Map<String, Any?> = cache.toMap()

    private fun notifyListeners(keys: List<String>) {
        for (listener in listeners) {
            for (key in keys) {
                listener.onSharedPreferenceChanged(this, key)
            }
        }
    }

    private inner class Editor : SharedPreferences.Editor {
        private val pending = LinkedHashMap<String, Any?>()
        private var clearRequested = false

        override fun putString(key: String?, value: String?): SharedPreferences.Editor = applyPending(key, value)

        override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor =
            applyPending(key, values?.toSet())

        override fun putInt(key: String?, value: Int): SharedPreferences.Editor = applyPending(key, value)

        override fun putLong(key: String?, value: Long): SharedPreferences.Editor = applyPending(key, value)

        override fun putFloat(key: String?, value: Float): SharedPreferences.Editor = applyPending(key, value)

        override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor = applyPending(key, value)

        override fun remove(key: String?): SharedPreferences.Editor = applyPending(key, REMOVAL_TOKEN)

        override fun clear(): SharedPreferences.Editor {
            clearRequested = true
            return this
        }

        override fun commit(): Boolean {
            runBlocking { applyChanges() }
            return true
        }

        override fun apply() {
            scope.launch { applyChanges() }
        }

        private fun applyPending(key: String?, value: Any?): SharedPreferences.Editor {
            key ?: return this
            pending[key] = value
            return this
        }

        private suspend fun applyChanges() {
            if (pending.isEmpty() && !clearRequested) return

            dataStore.edit { prefs ->
                if (clearRequested) {
                    prefs.clear()
                }
                pending.forEach { (key, value) ->
                    when (value) {
                        REMOVAL_TOKEN -> removeKey(prefs, key)
                        is Boolean -> prefs[booleanPreferencesKey(key)] = value
                        is Int -> prefs[intPreferencesKey(key)] = value
                        is Long -> prefs[longPreferencesKey(key)] = value
                        is Float -> prefs[floatPreferencesKey(key)] = value
                        is Double -> prefs[floatPreferencesKey(key)] = value.toFloat()
                        is String -> prefs[stringPreferencesKey(key)] = value
                        is Set<*> -> prefs[stringSetPreferencesKey(key)] = value.filterIsInstance<String>().toSet()
                        null -> removeKey(prefs, key)
                        else -> {
                            // Fallback for unsupported types - store as string representation.
                            prefs[stringPreferencesKey(key)] = value.toString()
                        }
                    }
                }
            }

            pending.clear()
            clearRequested = false
        }

        private fun removeKey(prefs: MutablePreferences, key: String) {
            prefs.remove(booleanPreferencesKey(key))
            prefs.remove(intPreferencesKey(key))
            prefs.remove(longPreferencesKey(key))
            prefs.remove(floatPreferencesKey(key))
            prefs.remove(stringPreferencesKey(key))
            prefs.remove(stringSetPreferencesKey(key))
        }
    }

    companion object {
        private val REMOVAL_TOKEN = Any()
    }
}
