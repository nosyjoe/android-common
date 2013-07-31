package com.nosyjoe.android.common.images;

import android.graphics.Bitmap;
import android.os.AsyncTask;

/**
 * @author Philipp Engel <philipp@filzip.com>
 */
public class BitmapLoaderTaskWithCallback extends AsyncTask<byte[], Void, Bitmap> {

//    private WeakReference<ImageView> targetView;
    private String imageUrlString;
    private IImageModifier modifier;
    private final IImageReceiver imageReceiver;
    private int sampleSize;
    private boolean success = false;

    public BitmapLoaderTaskWithCallback(IImageModifier modifier, IImageReceiver imageReceiver,
                                        String imageUrl, int sampleSize) {
        this.modifier = modifier;
        this.imageReceiver = imageReceiver;
        this.sampleSize = sampleSize;
//        this.targetView = new WeakReference<ImageView>(imageView);
        this.imageUrlString = imageUrl;
    }

    @Override
    protected Bitmap doInBackground(byte[]... bytes) {
        if (bytes == null || bytes.length <= 0) {
            return null;
        }

        Bitmap result = null;
        byte[] data = bytes[0];

        result = Util.decodeSampledImageByteArray(data, sampleSize);

        if (isCancelled()) return null;

        this.success = true;

        if (this.modifier != null) {
            result = this.modifier.apply(result);
        }

        return result;
    }

    @Override
    protected void onPostExecute(Bitmap bitmap) {
        if (isCancelled()) {
            bitmap = null;
            imageReceiver.onCancelled(imageUrlString);
        } else {
            if (success) {
                if (bitmap != null) {
//                    if (this.targetView != null && this.targetView.get() != null) {
//                        this.targetView.get().setImageBitmap(bitmap);
//                    }
                    imageReceiver.onImageLoaded(imageUrlString, bitmap, sampleSize);
                }
            } else {
                imageReceiver.onLoadError(imageUrlString, 0, "");
            }
        }

//        this.targetView = null;
    }

    public String getImageUrl() {
        return imageUrlString;
    }
}
