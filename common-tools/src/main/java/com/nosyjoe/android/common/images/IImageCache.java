package com.nosyjoe.android.common.images;

import android.graphics.Bitmap;

/**
 *
 */
public interface IImageCache {

    /**
     *
     * @param url
     * @param image
     */
    void put(String url, Bitmap image);

    /**
     *
     * @param url
     * @return
     */
    Bitmap get(String url);

    /**
     *
     * @param key
     * @return
     */
    boolean containsKey(String key);
}
