package com.nosyjoe.android.common.images;

import android.graphics.Bitmap;
import com.nosyjoe.android.common.NjLog;

/**
 * Simple meta cache that provides two cache levels.
 *
 * @author Philipp Engel <philipp@filzip.com>
 */
public class ImageCacheChain implements IImageCache {

    private final IImageCache l1Cache;
    private final IImageCache l2Cache;

    public ImageCacheChain(IImageCache l1Cache, IImageCache l2Cache) {
        this.l1Cache = l1Cache;
        this.l2Cache = l2Cache;
    }

    @Override
    public void put(String url, Bitmap image) {
        l1Cache.put(url, image);
        l2Cache.put(url, image);
    }

    @Override
    public Bitmap get(String url) {
        if (l1Cache.containsKey(url)) {
            NjLog.d(this, "Cache hit L1 cache for: " + url);
            return l1Cache.get(url);
        } else if (l2Cache.containsKey(url)) {
            NjLog.d(this, "Cache hit L2 (Miss L1) cache for: " + url);
            Bitmap bitmap = l2Cache.get(url);
            l1Cache.put(url, bitmap);
            return bitmap;
        } else {
            NjLog.d(this, "Cache miss for: " + url);
            return null;
        }
    }

    @Override
    public boolean containsKey(String key) {
        return l1Cache.containsKey(key) || l2Cache.containsKey(key);
    }

}
