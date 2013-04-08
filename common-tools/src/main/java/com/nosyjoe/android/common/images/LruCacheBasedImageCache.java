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

        // see https://developer.android.com/training/displaying-bitmaps/manage-memory.html

        // Notify the removed entry that is no longer being cached.
//        @Override
//        protected void entryRemoved(boolean evicted, String key,
//                                    BitmapDrawable oldValue, BitmapDrawable newValue) {
//            if (RecyclingBitmapDrawable.class.isInstance(oldValue)) {
//                // The removed entry is a recycling drawable, so notify it
//                // that it has been removed from the memory cache.
//                ((RecyclingBitmapDrawable) oldValue).setIsCached(false);
//            } else {
//                // The removed entry is a standard BitmapDrawable.
//                if (Utils.hasHoneycomb()) {
//                    // We're running on Honeycomb or later, so add the bitmap
//                    // to a SoftReference set for possible use with inBitmap later.
//                    mReusableBitmaps.add
//                            (new SoftReference<Bitmap>(oldValue.getBitmap()));
//                }
//            }
//        }
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
