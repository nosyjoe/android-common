package com.nosyjoe.android.common.cache;

import com.nosyjoe.android.common.NjLog;

import java.util.*;

/**
 * @author Philipp Engel <philipp@filzip.com>
 *
 * @deprecated Use the LruMemoryCache if possible
 */
public class NaiveMemoryCache<K extends ICacheEntry> implements ICache<K> {
    
    private static final int MAX_ITEMS = 40;

    private Map<String, K> cacheMap = Collections.synchronizedMap(
            new LinkedHashMap<String, K>(40, 1.1f, true));
    
    @Override
    public void put(String url, K image) {
        if (cacheMap.containsKey(url)) {
            this.cacheMap.put(url, image);
        } else {
            if (cacheMap.size() >= MAX_ITEMS) {
                this.makeRoom();
            }
            this.cacheMap.put(url, image);
        }
    }
    
    @Override
    public K get(String url) {
        return this.cacheMap.get(url);
    }

    @Override
    public boolean containsKey(String key) {
        return cacheMap.containsKey(key);
    }

    private void makeRoom() {
        Set<String> toDelete = new HashSet<String>();
        //remove a quarter of the entries
        int rmCount = MAX_ITEMS / 4;
        int sizeBefore = this.cacheMap.size();

        Set<String> allKeys = this.cacheMap.keySet();
        int i = 0;
        for(String aKey : allKeys) {
            if (i >= rmCount) break;
            toDelete.add(aKey);

            i++;
        }
        
        for(String aKey : toDelete) {
            this.cacheMap.remove(aKey);
        }

        NjLog.d(this, "Made room, size before " + sizeBefore + ", size now: " + this.cacheMap.size());
                
    }

}
