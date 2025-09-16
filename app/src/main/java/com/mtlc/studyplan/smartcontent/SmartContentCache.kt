package com.mtlc.studyplan.smartcontent

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Lightweight in-memory caches for smart content to avoid recomputation and improve UX.
 * All caches are process-lifetime only, safe for offline mode.
 */
object SmartContentCache {
    private data class Entry<T>(val value: T, val expiry: Long)

    private val recsCache = ConcurrentHashMap<String, Entry<List<ContentRecommendation>>>()
    private val dailyPackCache = ConcurrentHashMap<String, Entry<DailyContentPack>>()

    private fun now() = System.currentTimeMillis()

    fun putRecommendations(userKey: String, list: List<ContentRecommendation>, ttlMinutes: Long = 30) {
        val expiry = now() + TimeUnit.MINUTES.toMillis(ttlMinutes)
        recsCache[userKey] = Entry(list, expiry)
    }

    fun getRecommendations(userKey: String): List<ContentRecommendation>? {
        val e = recsCache[userKey] ?: return null
        return if (e.expiry > now()) e.value else null
    }

    fun putDailyPack(dayKey: String, pack: DailyContentPack, ttlMinutes: Long = 120) {
        val expiry = now() + TimeUnit.MINUTES.toMillis(ttlMinutes)
        dailyPackCache[dayKey] = Entry(pack, expiry)
    }

    fun getDailyPack(dayKey: String): DailyContentPack? {
        val e = dailyPackCache[dayKey] ?: return null
        return if (e.expiry > now()) e.value else null
    }

    fun clear() {
        recsCache.clear()
        dailyPackCache.clear()
    }
}

