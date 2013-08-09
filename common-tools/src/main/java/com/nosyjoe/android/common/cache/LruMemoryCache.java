package com.nosyjoe.android.common.cache;

import android.util.LruCache;

/**
 * @author Philipp Engel <philipp@filzip.com>
 */
public class LruMemoryCache<K extends ICacheEntry> implements ICache<K> {

    public static final int DEFAULT_CACHE_SIZE = 4 * 1024 * 1024;

    private final int cacheSize; // 4MiB
    private LruCache<String, K> cache;

    public LruMemoryCache(int maxCacheSize) {
        cacheSize = maxCacheSize;
        cache = new LruCache<String, K>(cacheSize) {
            @Override
            protected int sizeOf(String key, K value) {
                return value.getSize();
            }

            // see https://developer.android.com/training/displaying-bitmaps/manage-memory.html

        };
    }

    @Override
    public void put(String key, K data) {
        cache.put(key, data);
    }

    @Override
    public K get(String key) {
        return cache.get(key);
    }

    @Override
    public boolean containsKey(String key) {
        return cache.get(key) != null;
    }

    @Override
    public K remove(String key) {
        return cache.remove(key);
    }

    @Override
    public void evictAll() {
        cache.evictAll();
    }
}
