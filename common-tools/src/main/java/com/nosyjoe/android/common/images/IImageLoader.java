package com.nosyjoe.android.common.images;

import android.widget.ImageView;

/**
 * Provides functions that load images. Implementations should never do the loading on the UI thread.
 */
public interface IImageLoader {

    /**
     * Loads an image with given url and set it as source of the ImageView once it has finished loading.
     * @param imageView
     * @param imageUrl
     */
    void load(ImageView imageView, String imageUrl);

    void load(ImageView imageView, String imageUrl, IImageModifier modifier);

    void load(final String imageUrl, final int targetWidth, final int targetHeight, final IBitmapListener listener);
}
