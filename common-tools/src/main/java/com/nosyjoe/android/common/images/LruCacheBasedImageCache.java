package com.nosyjoe.android.common.images;

import android.graphics.Bitmap;
import android.util.LruCache;

/**
 * @author Philipp Engel <philipp@filzip.com>
 */
public class LruCacheBasedImageCache implements IImageCache {

    private int cacheSize = 4 * 1024 * 1024; // 4MiB

    private LruCache<String, Bitmap> cache = new LruCache(cacheSize) {
        protected int sizeOf(String key, Bitmap value) {
            return value.getByteCount();

        }
    };

    @Override
    public void put(String url, Bitmap image) {
        cache.put(url, image);
    }

    @Override
    public Bitmap get(String url) {
        return cache.get(url);
    }

    @Override
    public boolean containsKey(String key) {
        return cache.get(key) != null;
    }
}
