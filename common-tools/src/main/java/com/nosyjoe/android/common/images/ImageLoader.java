package com.nosyjoe.android.common.images;


import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
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
import java.util.*;

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
    private Set<String> activeRequests = new HashSet<String>();
    private Map<String, List<IBitmapListener>> duplicateRequests;

    public ImageLoader(ICache bitmapCache) {
        this(null, bitmapCache, null);
    }

    /**
     * Creates a new loader with a bitmapCache of a certain size
     *
     * @param cacheSize   size of the bitmapCache in bytes
     * @param placeholder
     */
    public ImageLoader(int cacheSize, String cacheDir, Drawable placeholder) throws IOException {
        this(new LruMemoryCache<BitmapEntry>(3 * cacheSize / 4),
                new CacheChain(
                        new LruMemoryCache<ByteArrayEntry>(cacheSize / 4),
                        new LruFileCache<ByteArrayEntry>(cacheDir, new ByteArrayEntry.ByteArrayEntryFactory())),
                placeholder);
    }

    public ImageLoader(ICache bitmapCache, ICache byteCache, Drawable placeholder) {
        this.bitmapCache = bitmapCache;
        this.byteCache = byteCache;
        this.placeholder = placeholder;
        mainHandler = new Handler(Looper.getMainLooper());
        HandlerThread imageLoaderThread = new HandlerThread("ImageLoaderThread");
        imageLoaderThread.start();
        loaderHandler = new Handler(imageLoaderThread.getLooper());
        duplicateRequests = new HashMap<String, List<IBitmapListener>>();
    }

    @Override
    public void load(ImageView imageView, String imageUrl) {
        this.load(imageView, imageUrl, null);
    }

    @Override
    public void load(final String imageUrl, final int targetWidth, final int targetHeight, final IBitmapListener listener) {
        this.loaderHandler.post(new Runnable() {
            @Override
            public void run() {
                doLoad(targetWidth, targetHeight, imageUrl, null, listener);
            }
        });
    }

    @Override
    public void load(final ImageView imageView, final String imageUrl, final IImageModifier modifier) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                imageView.setImageDrawable(placeholder);
            }
        });
        this.loaderHandler.post(new Runnable() {
            @Override
            public void run() {
                int targetWidth = 0;
                int targetHeight = 0;
                if (imageView != null) {
                    targetHeight = imageView.getHeight();
                    targetWidth = imageView.getWidth();
                }

                if (cancelPotentialDownload(imageUrl, imageView)) {
                    ImageViewListener imageViewListener = new ImageViewListener(imageView, mainHandler);

                    // TODO cancel downloads again
//                    DownloadedDrawable downloadedDrawable = new DownloadedDrawable(task, placeholder);
//                    postSetImageDrawable(imageView, downloadedDrawable);

                    doLoad(targetWidth, targetHeight, imageUrl, modifier, imageViewListener);
                }
            }
        });
    }

    private void doLoad(final int targetWidth, final int targetHeight, final String imageUrl,
                        IImageModifier modifier, final IBitmapListener listener) {

        if (TextUtils.isEmpty(imageUrl)) {
            NjLog.d(this, "Not downloading image, url is empty!");
            return;
        }

        // get by image url
        int sampleSize = tryToGetSampleSize(targetWidth, targetHeight, imageUrl);

        String keyWithSampleSize = getKeyWithSampleSize(imageUrl, sampleSize);

        if (bitmapCache != null && bitmapCache.containsKey(keyWithSampleSize)) {
            if (DEBUG) NjLog.d(this, "Cache HIT, Bitmap for: " + keyWithSampleSize);
            BitmapEntry bitmapEntry = bitmapCache.get(keyWithSampleSize);

            Bitmap data = bitmapEntry.getData();
            if (data != null && !data.isRecycled()) {
                listener.onLoaded(data);
            } else {
                // TODO find out why bitmaps get recycled; hacky solution for recycled images: remove the entry and repeat the call.
                bitmapCache.remove(keyWithSampleSize);
                doLoad(targetWidth, targetHeight, imageUrl, modifier, listener);
                return;
            }
        } else if (byteCache != null && byteCache.containsKey(imageUrl)) {
            addToActiveListeners(imageUrl, listener);
            if (hasActiveRequest(imageUrl)) {
                // TODO fix issue: the sample size might differ and
                if (DEBUG) NjLog.d(this, "Cache HIT, byte[], being loaded, adding to pending: " + imageUrl);
            } else {
                if (DEBUG) NjLog.d(this, "Cache HIT, byte[] for: " + imageUrl);
                addToActiveRequests(imageUrl);
                BitmapLoaderTaskWithCallback task = new BitmapLoaderTaskWithCallback(modifier, this, imageUrl, sampleSize);
                task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, byteCache.get(imageUrl).getData());
            }
        } else {
            if (DEBUG) NjLog.d(this, "Cache MISS for: " + imageUrl);

            addToActiveListeners(imageUrl, listener);

            if (hasActiveRequest(imageUrl)) {
                if (DEBUG) NjLog.d(this, "Network request already active, adding to pending requests: " + imageUrl);
            } else {
                addToActiveRequests(imageUrl);
                ImageLoaderTaskWithCallback task = new ImageLoaderTaskWithCallback(targetWidth, targetHeight, modifier, this);
                if (DEBUG) NjLog.d(this, "Starting image download: " + imageUrl);
                task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, imageUrl);
            }

        }
    }

    private int tryToGetSampleSize(int targetWidth, int targetHeight, String imageUrl) {
        int sampleSize = 1;
        if (this.byteCache != null && this.byteCache.containsKey(imageUrl)) {
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
                if (imageView != null && bitmap != null) {
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

    private boolean postFinishRequest(final String imageUrl, final Bitmap image) {
        return loaderHandler.post(new Runnable() {
            @Override
            public void run() {
                removeFromActiveRequests(imageUrl);
                finishDuplicateRequests(imageUrl, image);
            }
        });
    }

    private void finishDuplicateRequests(String imageUrl, Bitmap bitmap) {
        List<IBitmapListener> ivList = duplicateRequests.remove(imageUrl);
        if (ivList != null) {
            for (IBitmapListener aListener : ivList) {
//                postSetImageBitmap(aListener, bitmap);
                aListener.onLoaded(bitmap);
            }
        }
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
        postFinishRequest(imageUrl, image);
    }

    @Override
    public void onLoadError(String imageUrl, int code, String message) {
        postFinishRequest(imageUrl, null);
    }

    @Override
    public void onCancelled(String imageUrl) {
        postFinishRequest(imageUrl, null);
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

    private boolean hasActiveRequest(String imageUrl) {
        return activeRequests.contains(imageUrl);
    }

    private boolean addToActiveRequests(String imageUrl) {
        return activeRequests.add(imageUrl);
    }

    private boolean removeFromActiveRequests(String imageUrl) {
        return activeRequests.remove(imageUrl);
    }

    private void addToActiveListeners(String imageUrl, IBitmapListener listener) {
        List<IBitmapListener> ivList = duplicateRequests.get(imageUrl);
        if (ivList == null) {
            ivList = new ArrayList<IBitmapListener>();
        }
        if (!ivList.contains(listener)) {
            ivList.add(listener);
        }
        duplicateRequests.put(imageUrl, ivList);
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

    private static class ExactSizeListener implements IBitmapListener {

        private final IBitmapListener originalListener;
        private final int targetWidth;
        private final int targetHeight;

        private ExactSizeListener(IBitmapListener originalListener, int targetWidth, int targetHeight) {
            this.originalListener = originalListener;
            this.targetWidth = targetWidth;
            this.targetHeight = targetHeight;
        }

        @Override
        public void onLoaded(final Bitmap bitmap) {
            new Runnable() {
                @Override
                public void run() {
                    if (originalListener != null) {
                        originalListener.onLoaded(Bitmap.createBitmap(bitmap, 0, 0, targetWidth, targetHeight));
                    }
                }
            };
        }
    }

    private static class ImageViewListener implements IBitmapListener {

        private final Handler handler;
        private WeakReference<ImageView> targetView;

        public ImageViewListener(ImageView imageView, Handler handler) {
            this.handler = handler;
            this.targetView = new WeakReference<ImageView>(imageView);
        }

        @Override
        public void onLoaded(final Bitmap bitmap) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (bitmap != null) {
                        if (targetView != null && targetView.get() != null) {
                            if (bitmap.isRecycled()) NjLog.d("ImageLoader", "IS Recycled! :(");
                            else {
                                TransitionDrawable transitionDrawable = new TransitionDrawable(
                                        new Drawable[]{targetView.get().getDrawable(), new BitmapDrawable(bitmap)});
                                transitionDrawable.setCrossFadeEnabled(true);
                                targetView.get().setImageDrawable(transitionDrawable);
                                transitionDrawable.startTransition(50);
                            }
                        }
                    }

                    targetView = null;
                }
            });
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ImageViewListener that = (ImageViewListener) o;

            if (targetView != null ? !targetView.equals(that.targetView) : that.targetView != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            return targetView != null ? targetView.hashCode() : 0;
        }
    }

}
