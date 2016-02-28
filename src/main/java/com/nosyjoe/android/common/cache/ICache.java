package com.nosyjoe.android.common.cache;

/**
 * A generic cache for any kind of data
 */
public interface ICache<K extends ICacheEntry> {

    /**
     * Put data to the cache identified by a unique key.
     *
     * @param key the unique key to reference the data in the cache
     * @param data the data
     */
    void put(String key, K data);

    /**
     * Retrieve an entry from the cache.
     * @param key the unique key of cached entry
     * @return the cached entry, or null if the key is unknown or an error happened
     */
     K get(String key);

    /**
     * Check if the cache contains an entry for a certain key.
     *
     * @param key the key to check for
     * @return true if cached data exists, false otherwise
     */
    boolean containsKey(String key);

    /**
     * Remove an entry from the cache.
     * @param key the key to remove the entry of
     * @return the removed entry
     */
    K remove(String key);

    /**
     * Removes all entry from a cache
     */
    void evictAll();
}
