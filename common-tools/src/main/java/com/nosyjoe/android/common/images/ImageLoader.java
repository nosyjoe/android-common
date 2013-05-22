package com.nosyjoe.android.common.images;


import android.graphics.Bitmap;
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
import com.nosyjoe.android.common.cache.*;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Provides utility functionality to aynchrounously load images and pictures.
 *
 * @author Philipp Engel <philipp@filzip.com>
 */
public class ImageLoader implements IImageLoader, IImageReceiver {

    private static final Boolean DEBUG = false;

    private ICache<BitmapEntry> bitmapCache;
    private final ICache<ByteArrayEntry> byteCache;
    private final Drawable placeholder;
    private final Handler mainHandler;
    private Handler loaderHandler;
    private Map<String, List<ImageView>> duplicateRequests;

    public ImageLoader(ICache bitmapCache) {
        this(null, bitmapCache, null);
    }

    /**
     * Creates a new loader with a bitmapCache of a certain size
     * @param cacheSize size of the bitmapCache in bytes
     * @param placeholder
     */
    public ImageLoader(int cacheSize, String cacheDir, Drawable placeholder) throws IOException {
        this(new LruMemoryCache<BitmapEntry>(3*cacheSize/4),
                new CacheChain(
                        new LruMemoryCache<ByteArrayEntry>(cacheSize/4),
                        new LruFileCache<ByteArrayEntry>(cacheDir, new ByteArrayEntry.ByteArrayEntryFactory())),
                placeholder);
    }

    protected ImageLoader(ICache bitmapCache, ICache byteCache, Drawable placeholder) {
        this.bitmapCache = bitmapCache;
        this.byteCache = byteCache;
        this.placeholder = placeholder;
        mainHandler = new Handler(Looper.getMainLooper());
        HandlerThread imageLoaderThread = new HandlerThread("ImageLoaderThread");
        imageLoaderThread.start();
        loaderHandler = new Handler(imageLoaderThread.getLooper());
        duplicateRequests = new HashMap<String, List<ImageView>>();
    }

    @Override
    public void load(ImageView imageView, String imageUrl) {
        this.load(imageView, imageUrl, null);
    }

    @Override
    public void load(final ImageView imageView, final String imageUrl, final IImageModifier modifier) {
        // make sure this runs on the main thread
//        if (Looper.myLooper() != loaderHandler.getLooper()) {
            this.loaderHandler.post(new Runnable() {
                @Override
                public void run() {
                    doLoad(imageView, imageUrl, modifier);
                }
            });
//        } else {
//            this.doLoad(imageView, imageUrl, modifier);
//        }
    }

    private void doLoad(final ImageView imageView, final String imageUrl, IImageModifier modifier) {
        if (TextUtils.isEmpty(imageUrl)) {
            NjLog.d(this, "Not downloading image, url is empty!");
            return;
        }

        // get by image url
        int sampleSize = tryToGetSampleSize(imageView, imageUrl);


        String keyWithSampleSize = getKeyWithSampleSize(imageUrl, sampleSize);
        if (bitmapCache != null && bitmapCache.containsKey(keyWithSampleSize)) {
            if (DEBUG) NjLog.d(this, "Cache HIT, Bitmap for: " +keyWithSampleSize);
            BitmapEntry bitmapEntry = bitmapCache.get(getKeyWithSampleSize(imageUrl, sampleSize));
            postSetImageBitmap(imageView, bitmapEntry.getData());
        } else if (byteCache != null && byteCache.containsKey(imageUrl)) {
            if (DEBUG) NjLog.d(this, "Cache HIT, byte[] for: " +imageUrl);
            BitmapLoaderTask task = new BitmapLoaderTask(imageView, modifier, this, imageUrl, sampleSize);
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, byteCache.get(imageUrl).getData());
        } else {
            if (DEBUG) NjLog.d(this, "Cache MISS for: " +imageUrl);
            if (cancelPotentialDownload(imageUrl, imageView)) {
                ImageLoaderTask task = new ImageLoaderTask(imageView, modifier, this);
                DownloadedDrawable downloadedDrawable = new DownloadedDrawable(task, placeholder);
//                duplicateRequests.put(imageUrl, new ArrayList<ImageView>());
                postSetImageDrawable(imageView, downloadedDrawable);

                NjLog.d(this, "Starting image download: " + imageUrl);

                task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, imageUrl);
            }
        }
    }

    private int tryToGetSampleSize(ImageView imageView, String imageUrl) {
        int sampleSize = 1;
        if (this.byteCache != null && this.byteCache.containsKey(imageUrl)) {
            int targetWidth = 0;
            int targetHeight = 0;
            if (imageView != null) {
                targetHeight = imageView.getHeight();
                targetWidth = imageView.getWidth();
            }

            if (targetWidth > 0 && targetHeight > 0) {
                ByteArrayEntry byteEntry = byteCache.get(imageUrl);
                sampleSize = Util.calculateInSampleSizeFromByteArray(byteEntry.getData(), targetWidth, targetHeight);
            }
        }
        return sampleSize;
    }

    private boolean postSetImageBitmap(final ImageView imageView, final Bitmap bitmap) {
        return mainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (bitmap != null) {
                    imageView.setImageBitmap(bitmap);
                }
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

    private boolean postAddImageToCache(final String imageUrl, final Bitmap image, final int sampleSize) {
        return loaderHandler.post(new Runnable() {
            @Override
            public void run() {
                if (bitmapCache != null) {
                    bitmapCache.put(getKeyWithSampleSize(imageUrl, sampleSize), new BitmapEntry(image));
                }
            }
        });
    }

    private String getKeyWithSampleSize(String imageUrl, int sampleSize) {
        if (sampleSize > 0) {
            return imageUrl + "_" + String.valueOf(sampleSize);
        } else {
            return imageUrl + "_" + String.valueOf(1);
        }
    }

    private static boolean cancelPotentialDownload(String url, ImageView imageView) {
        ImageLoaderTask downloadTask = getDownloadTask(imageView);

        if (downloadTask != null) {
            String bitmapUrl = downloadTask.getImageUrl();
            if ((bitmapUrl == null) || (!bitmapUrl.equals(url))) {
                downloadTask.cancel(true);
            } else {
                // The same EXTRA_MEDIA_URL is already being downloaded.
                return false;
            }
        }
        return true;
    }

    private static ImageLoaderTask getDownloadTask(ImageView imageView) {
        if (imageView != null) {
            Drawable drawable = imageView.getDrawable();
            if (drawable instanceof DownloadedDrawable) {
                DownloadedDrawable downloadedDrawable = (DownloadedDrawable) drawable;
                return downloadedDrawable.getBitmapDownloaderTask();
            }
        }
        return null;
    }

    @Override
    public void onImageLoaded(String imageUrl, Bitmap image, int sampleSize) {
        postAddImageToCache(imageUrl, image, sampleSize);
    }

    @Override
    public void onLoadError(String imageUrl, int code, String message) {
        postRemoveUrl(imageUrl);
    }

    @Override
    public void onCancelled(String imageUrl) {
        postRemoveUrl(imageUrl);
    }

    @Override
    public void onBytesLoaded(final String imageUrlString, final byte[] bytes) {
        if (bytes != null) {
            loaderHandler.post(new Runnable() {
                @Override
                public void run() {
                    byteCache.put(imageUrlString, new ByteArrayEntry(bytes));
                }
            });
        }
    }

    private boolean postRemoveUrl(final String imageUrl) {
        return loaderHandler.post(new Runnable() {
            @Override
            public void run() {
                duplicateRequests.remove(imageUrl);
            }
        });
    }

    static class DownloadedDrawable extends ColorDrawable {
        private final WeakReference<ImageLoaderTask> bitmapDownloaderTaskReference;

        public DownloadedDrawable(ImageLoaderTask bitmapDownloaderTask, Drawable placeholder) {
            super(Color.BLACK);
            bitmapDownloaderTaskReference =
                    new WeakReference<ImageLoaderTask>(bitmapDownloaderTask);
        }

        public ImageLoaderTask getBitmapDownloaderTask() {
            return bitmapDownloaderTaskReference.get();
        }
    }


}
