// LRU Cache implementation to replace unbounded caches
private class LRUCache<K, V>(private val maxSize: Int = 256) : LinkedHashMap<K, V>(16, 0.75f, true) {
    override fun removeEldestEntry(eldest: Map.Entry<K, V>?): Boolean {
        return size > maxSize
    }

    fun getOrPut(key: K, defaultValue: () -> V): V {
        return get(key) ?: run {
            val value = defaultValue()
            put(key, value)
            value
        }
    }

    fun clear() {
        super.clear()
    }
}
