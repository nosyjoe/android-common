package com.nosyjoe.android.common.images;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import com.nosyjoe.android.common.NjLog;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * @author Philipp Engel <philipp@filzip.com>
 */
public class ImageLoaderTaskWithCallback extends AsyncTask<String, Void, Bitmap> {

//    private WeakReference<ImageView> targetView;
    private String imageUrlString;
    private final int targetWidth;
    private final int targetHeight;
    private IImageModifier modifier;
    private final IImageReceiver imageReceiver;
    private int sampleSize;
    private boolean success = false;
    private int responseCode;
    private String responseMessage;

    public ImageLoaderTaskWithCallback(int targetWidth, int targetHeight, IImageModifier modifier, IImageReceiver imageReceiver) {
        this.targetWidth = targetWidth;
        this.targetHeight = targetHeight;
        this.modifier = modifier;
        this.imageReceiver = imageReceiver;
//        this.targetView = new WeakReference<ImageView>(imageView);
    }

    @Override
    protected Bitmap doInBackground(String... strings) {
        if (strings == null || strings.length <= 0) {
            return null;
        }

        Bitmap result = null;
        imageUrlString = strings[0];
        FlushedInputStream inStream = null;

        try {
            URL url = new URL(imageUrlString);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.setUseCaches(true);
            urlConnection.connect();

            if (isCancelled()) return null;

            responseCode = urlConnection.getResponseCode();

            if (responseCode >= HttpURLConnection.HTTP_BAD_REQUEST) {
                responseMessage = urlConnection.getResponseMessage();
                NjLog.d(this, "Error loading image: " + responseCode + ", " + responseMessage);
                urlConnection.disconnect();
                return null;
            }

//            NjLog.d(this, "Response code: " + responseCode + ", " + responseMessage);

            if (isCancelled()) return null;

            inStream = new FlushedInputStream(urlConnection.getInputStream());
            byte[] bytes = Util.readFully(inStream);

            urlConnection.disconnect();

            imageReceiver.onBytesLoaded(imageUrlString, bytes);

            if (isCancelled()) return null;

            if (targetHeight > 0 && targetWidth > 0) {
//                sampleSize = Util.calculateInSampleSizeFromByteArray(bytes, targetWidth, targetHeight);
                BitmapFactory.Options options = Util.getOptions(bytes);
                sampleSize = Util.calculateInSampleSize(options, targetWidth, targetHeight);
                result = Util.decodeSampledImageByteArray(bytes, sampleSize);
            } else {
                sampleSize = 1;
                result = Util.decodeSampledImageByteArray(bytes, sampleSize);
            }

            if (isCancelled()) return null;

            this.success = true;

        } catch (IOException e) {
            NjLog.w(this, "Failed loading image " + imageUrlString + " because: " + e.getMessage());
            this.success = false;
        } finally {
            if (inStream != null) try {
                inStream.close();
            } catch (IOException e) {
            }
        }

        if (this.modifier != null) {
            result = this.modifier.apply(result);
        }

        return result;
    }

    @Override
    protected void onPostExecute(Bitmap bitmap) {
        if (isCancelled()) {
            imageReceiver.onCancelled(imageUrlString);
        } else {
            if (success) {
                if (bitmap != null) {
                    imageReceiver.onImageLoaded(imageUrlString, bitmap, sampleSize);
                }
            } else {
                imageReceiver.onLoadError(imageUrlString, responseCode, responseMessage);
            }
        }
    }

    public String getImageUrl() {
        return imageUrlString;
    }
}
