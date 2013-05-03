package com.nosyjoe.android.common.images;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.widget.ImageView;
import com.nosyjoe.android.common.NjLog;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * @author Philipp Engel <philipp@filzip.com>
 */
public class ImageLoaderTask extends AsyncTask<String, Void, Bitmap> {

    private WeakReference<ImageView> targetView;
    private String imageUrlString;
    private IImageModifier modifier;
    private final IImageReceiver imageReceiver;
    private int sampleSize;
    private boolean success = false;
    private int responseCode;
    private String responseMessage;

    public ImageLoaderTask(ImageView imageView, IImageModifier modifier, IImageReceiver imageReceiver) {
        this.modifier = modifier;
        this.imageReceiver = imageReceiver;
        this.targetView = new WeakReference<ImageView>(imageView);
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

            int targetWidth = 0;
            int targetHeight = 0;
            if (targetView != null && targetView.get() != null) {
                targetHeight = targetView.get().getHeight();
                targetWidth = targetView.get().getWidth();
            }

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
            bitmap = null;
            imageReceiver.onCancelled(imageUrlString);
        } else {
            if (success) {
                if (bitmap != null) {
                    if (this.targetView != null && this.targetView.get() != null) {
                        this.targetView.get().setImageBitmap(bitmap);
                    }
                    imageReceiver.onImageLoaded(imageUrlString, bitmap, sampleSize);
                }
            } else {
                imageReceiver.onLoadError(imageUrlString, responseCode, responseMessage);
            }
        }

        this.targetView = null;
    }

    public String getImageUrl() {
        return imageUrlString;
    }
}
