package com.nosyjoe.android.common.images;

import android.graphics.Bitmap;

public interface IImageReceiver {

    void onImageLoaded(String imageUrl, Bitmap image, int sampleSize);

    void onLoadError(String imageUrl, int code, String message);

    void onCancelled(String imageUrl);

    void onBytesLoaded(String imageUrlString, byte[] bytes);
}
