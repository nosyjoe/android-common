package com.nosyjoe.android.common.images;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import com.nosyjoe.android.common.NjLog;

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Stores a static list of images to
 *
 * @author Philipp Engel <philipp@filzip.com>
 */
public class NaiveFileImageCache implements IImageCache{

    // TODO watch availability of SD card and handle

    private Map<String, String> cachedFiles;
    private String baseDir;
    private MessageDigest md5;
    private Context context;

    public NaiveFileImageCache(Context context, String baseDir) {
        // create a linkedhashmap that is sorted by access-order
        this.cachedFiles = new LinkedHashMap<String, String>(50, 1.1f, true);
        this.context = context.getApplicationContext();
        
        this.setBaseDir(baseDir);
        try {
            md5 = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            NjLog.w(this, "Can't create md5 digest: " + e.getMessage());
        }
    }

    @Override
    public void put(String url, Bitmap image) {
        FileOutputStream out = null;
        String destinationPath = this.getDestinationPath(url);
        try {
            out = new FileOutputStream(destinationPath);
            image.compress(Bitmap.CompressFormat.PNG, 90, out);
        } catch (FileNotFoundException e) {
            NjLog.w(this, "Writing to file " + destinationPath + " failed: " + e.getMessage());
            this.cachedFiles.remove(url);
        } finally {
            if (out != null) {
                try { out.close(); } catch (IOException e) { /* nothing to do */ }
            }
        }
    }

    @Override
    public Bitmap get(String url) {
        String destinationPath = this.getDestinationPath(url);
        Bitmap bitmap = BitmapFactory.decodeFile(destinationPath);
        return bitmap;
    }

    @Override
    public boolean containsKey(String key) {

        // TODO optimize: do not query filesystem every time
        if (key instanceof String) {
            File toTest = new File(this.getDestinationPath((String) key));
            return toTest.exists();
        }

        return false;
    }

    private void makeRoomIfRequired() {

    }
    
    protected String getDestinationPath(String url) {
        String filename = null;
        byte[] md5Bytes;

        try {
            md5Bytes = md5.digest(url.getBytes("UTF-8"));
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < md5Bytes.length; ++i) {
                sb.append(Integer.toHexString((md5Bytes[i] & 0xFF) | 0x100).substring(1,3));
            }
            filename = sb.toString();
            md5.reset();
        } catch (UnsupportedEncodingException e) {
        }

        return this.baseDir + filename;
    }

    private void setBaseDir(String baseDir) {
        if (!baseDir.endsWith(File.separator))
            this.baseDir = baseDir + File.separator;
        else
            this.baseDir = baseDir;

        // create all directories
        new File(this.baseDir).mkdirs();
    }
}
