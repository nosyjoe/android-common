package com.nosyjoe.android.common.cache;

import com.nosyjoe.android.common.NjLog;

/**
 * Simple meta cache that provides two cache levels.
 *
 * @author Philipp Engel <philipp@filzip.com>
 */
public class CacheChain<K extends ICacheEntry> implements ICache<K> {

    private static final boolean DEBUG = false;

    private final ICache<K> l1Cache;
    private final ICache<K> l2Cache;

    public CacheChain(ICache l1Cache, ICache l2Cache) {
        this.l1Cache = l1Cache;
        this.l2Cache = l2Cache;
    }

    @Override
    public void put(String url, K image) {
        l1Cache.put(url, image);
        l2Cache.put(url, image);
    }

    @Override
    public K get(String url) {
        if (l1Cache.containsKey(url)) {
            if (DEBUG) NjLog.d(this, "Cache hit L1 cache for: " + url);
            return l1Cache.get(url);
        } else if (l2Cache.containsKey(url)) {
            if (DEBUG) NjLog.d(this, "Cache hit L2 (Miss L1) cache for: " + url);
            K bitmap = l2Cache.get(url);
            l1Cache.put(url, bitmap);
            return bitmap;
        } else {
            if (DEBUG) NjLog.d(this, "Cache miss for: " + url);
            return null;
        }
    }

    @Override
    public boolean containsKey(String key) {
        return l1Cache.containsKey(key) || l2Cache.containsKey(key);
    }

    @Override
    public K remove(String key) {
        K removed = l2Cache.remove(key);
        removed = l1Cache.remove(key);

        return removed;
    }

    @Override
    public void evictAll() {
        l2Cache.evictAll();
        l1Cache.evictAll();
    }
}
