package com.nosyjoe.android.common.images;


import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.text.TextUtils;
import android.widget.ImageView;
import com.nosyjoe.android.common.NjLog;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Provides utility functionality to aynchrounously load images and pictures.
 *
 * @author Philipp Engel <philipp@filzip.com>
 */
public class ImageLoader implements IImageLoader {

    private IImageCache cache;
    private final Handler mainHandler;
    private Handler loaderHandler;
    //    private Map<String, List<ImageView>> duplicateRequests

    public ImageLoader() {
        this(null);
    }

    public ImageLoader(IImageCache cache) {
        this.cache = cache;
        mainHandler = new Handler(Looper.getMainLooper());
        HandlerThread imageLoaderThread = new HandlerThread("ImageLoaderThread");
        imageLoaderThread.start();
        loaderHandler = new Handler(imageLoaderThread.getLooper());
    }

    @Override
    public void load(ImageView imageView, String imageUrl) {
        this.load(imageView, imageUrl, null);
    }

    @Override
    public void load(final ImageView imageView, final String imageUrl, final IImageModifier modifier) {
        // make sure this runs on the main thread
        if (Looper.myLooper() != loaderHandler.getLooper()) {
            this.loaderHandler.post(new Runnable() {
                @Override
                public void run() {
                    doLoad(imageView, imageUrl, modifier);
                }
            });
        } else {
            this.doLoad(imageView, imageUrl, modifier);
        }
    }

    private void doLoad(final ImageView imageView, final String imageUrl, IImageModifier modifier) {
        if (TextUtils.isEmpty(imageUrl)) {
            NjLog.d(this, "Not downloading image, url is empty!");
            return;
        }

        if (this.cache != null && this.cache.containsKey(imageUrl)) {
            // TODO think about concurrency
            postSetImageBitmap(imageView, imageUrl);
        } else {
            if (cancelPotentialDownload(imageUrl, imageView)) {
                ImageLoadTask task = new ImageLoadTask(imageView, modifier);
                DownloadedDrawable downloadedDrawable = new DownloadedDrawable(task);
                postSetImageDrawable(imageView, downloadedDrawable);

                NjLog.d(this, "Starting image download: " + imageUrl);

                task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, imageUrl);
            }
        }
    }

    private boolean postSetImageBitmap(final ImageView imageView, final String imageUrl) {
        return mainHandler.post(new Runnable() {
            @Override
            public void run() {
                imageView.setImageBitmap(cache.get(imageUrl));
            }
        });
    }

    private boolean postSetImageDrawable(final ImageView imageView, final Drawable source) {
        return mainHandler.post(new Runnable() {
            @Override
            public void run() {
                imageView.setImageDrawable(source);
            }
        });
    }

    private boolean postAddImageToCache(final String imageUrl, final Bitmap image) {
        return loaderHandler.post(new Runnable() {
            @Override
            public void run() {
                cache.put(imageUrl, image);
            }
        });
    }

    private static boolean cancelPotentialDownload(String url, ImageView imageView) {
        ImageLoadTask downloadTask = getDownloadTask(imageView);

        if (downloadTask != null) {
            String bitmapUrl = downloadTask.imageUrlString;
            if ((bitmapUrl == null) || (!bitmapUrl.equals(url))) {
                downloadTask.cancel(true);
            } else {
                // The same EXTRA_MEDIA_URL is already being downloaded.
                return false;
            }
        }
        return true;
    }

    private static ImageLoadTask getDownloadTask(ImageView imageView) {
        if (imageView != null) {
            Drawable drawable = imageView.getDrawable();
            if (drawable instanceof DownloadedDrawable) {
                DownloadedDrawable downloadedDrawable = (DownloadedDrawable)drawable;
                return downloadedDrawable.getBitmapDownloaderTask();
            }
        }
        return null;
    }
    
    private class ImageLoadTask extends AsyncTask<String, Void, Bitmap> {

        private WeakReference<ImageView> targetView;
        private String imageUrlString;
        private IImageModifier modifier;

        public ImageLoadTask(ImageView imageView, IImageModifier modifier) {
            this.modifier = modifier;
            this.targetView = new WeakReference<ImageView>(imageView);
        }

        @Override
        protected Bitmap doInBackground(String... strings) {
            if (strings == null || strings.length <= 0) {
                return null;
            }

            Bitmap result = null;
            imageUrlString = strings[0];
            FlushedInputStream inStream;

            try {
                URL url = new URL(imageUrlString);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
//                urlConnection.setDoOutput(true);
                urlConnection.connect();

                inStream = new FlushedInputStream(urlConnection.getInputStream());
                result = BitmapFactory.decodeStream(inStream);

                if (inStream != null)
                    inStream.close();

            } catch (IOException e) {
                NjLog.w(ImageLoader.this, "Failed loading image " + imageUrlString+" because: " + e.getMessage());
                // TODO reschedule loading when failed?
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
            }

            if (bitmap != null) {
                postAddImageToCache(this.imageUrlString, bitmap);
                if (this.targetView != null && this.targetView.get() != null)
                    this.targetView.get().setImageBitmap(bitmap);
            }
            
            this.targetView = null;
        }
    }

    static class DownloadedDrawable extends ColorDrawable {
        private final WeakReference<ImageLoadTask> bitmapDownloaderTaskReference;

        public DownloadedDrawable(ImageLoadTask bitmapDownloaderTask) {
            super(Color.BLACK);
            bitmapDownloaderTaskReference =
                    new WeakReference<ImageLoadTask>(bitmapDownloaderTask);
        }

        public ImageLoadTask getBitmapDownloaderTask() {
            return bitmapDownloaderTaskReference.get();
        }
    }

    private static class FlushedInputStream extends FilterInputStream {
        public FlushedInputStream(InputStream inputStream) {
            super(inputStream);
        }

        @Override
        public long skip(long n) throws IOException {
            long totalBytesSkipped = 0L;
            while (totalBytesSkipped < n) {
                long bytesSkipped = in.skip(n - totalBytesSkipped);
                if (bytesSkipped == 0L) {
                    int byteVal = read();
                    if (byteVal < 0) {
                        break;  // we reached EOF
                    } else {
                        bytesSkipped = 1; // we read one byte
                    }
                }
                totalBytesSkipped += bytesSkipped;
            }
            return totalBytesSkipped;
        }
    }
}
