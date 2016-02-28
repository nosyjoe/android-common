package com.nosyjoe.android.common.cache;

import com.jakewharton.disklrucache.DiskLruCache;

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * @author Philipp Engel <philipp@filzip.com>
 */
public class LruFileCache<K extends ICacheEntry> implements ICache<K> {

    private final DiskLruCache fileCache;
    private final ICacheEntryFactory<K> entryFactory;
    private MessageDigest md5;

    public LruFileCache(String cacheDir, ICacheEntryFactory<K> entryFactory) throws IOException {
        this.entryFactory = entryFactory;
        try {
            md5 = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            md5 = null;
        }
        fileCache = DiskLruCache.open(new File(cacheDir), 1, 1, 3 * 1024 * 1024);
    }

    // Creates a unique subdirectory of the designated app cache directory. Tries to use external
    // but if not mounted, falls back on internal storage.
//    public static File getDiskCacheDir(Context context, String uniqueName) {
//        // Check if media is mounted or storage is built-in, if so, try and use external cache dir
//        // otherwise use internal cache dir
//        final String cachePath =
//                Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()) ||
//                        !isExternalStorageRemovable() ? getExternalCacheDir(context).getPath() :
//                        context.getCacheDir().getPath();
//
//        return new File(cachePath + File.separator + uniqueName);
//    }

    @Override
    public void put(String key, K data) {
        try {
            DiskLruCache.Editor editor = fileCache.edit(getKey(key));

            if (editor != null) {
                OutputStream outputStream = editor.newOutputStream(0);
                data.writeDataToStream(outputStream);
                outputStream.close();
                editor.commit();
            }
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    @Override
    public K get(String key) {
        DiskLruCache.Snapshot snapshot = null;
        try {
            snapshot = fileCache.get(getKey(key));
            if (snapshot != null) {
                InputStream inputStream = snapshot.getInputStream(0);
                K entry = entryFactory.createEntry();
                entry.readDataFromStream(inputStream);
                inputStream.close();
                snapshot.close();
                return entry;
            }
        } catch (IOException e) {
        }
        return null;
    }

    @Override
    public boolean containsKey(String key) {
        try {
            DiskLruCache.Snapshot snapshot = fileCache.get(getKey(key));
            if (snapshot != null) {
                snapshot.close();
            }
            return snapshot != null;
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public K remove(String key) {
        try {
            if (containsKey(key)) {
                K toBeRemoved = get(key);
                fileCache.remove(getKey(key));
                return toBeRemoved;
            }
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        return null;
    }

    @Override
    public void evictAll() {
        // TODO implementation
    }

    private String getKey(String oldKey) {
        byte[] md5Bytes;

        try {
            md5Bytes = md5.digest(oldKey.getBytes("UTF-8"));
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < md5Bytes.length; ++i) {
                sb.append(Integer.toHexString((md5Bytes[i] & 0xFF) | 0x100).substring(1, 3));
            }
            md5.reset();
            return sb.toString();
        } catch (UnsupportedEncodingException e) {
        }
        return oldKey;
    }

}
