package com.nosyjoe.android.common.cache;

/**
 * Factory for new cache entries.
 * @param <K> The type of the cache entry
 */
public interface ICacheEntryFactory<K extends ICacheEntry> {

    /**
     * Creates one new cache entry, a descendant of ICacheEntry
     * @return the new instance of a cache entry
     */
    K createEntry();

}
