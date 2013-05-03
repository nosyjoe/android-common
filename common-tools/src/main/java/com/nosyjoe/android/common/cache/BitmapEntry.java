package com.nosyjoe.android.common.cache;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author Philipp Engel <philipp@filzip.com>
 */
public class BitmapEntry implements ICacheEntry<Bitmap> {

    private Bitmap bitmap;

    public BitmapEntry(Bitmap bitmap) {
        this.bitmap = bitmap;
    }

    @Override
    public int getSize() {
        return bitmap.getByteCount();
    }

    @Override
    public Bitmap getData() {
        return bitmap;
    }

    @Override
    public void writeDataToStream(OutputStream outputStream) {
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
    }

    @Override
    public void readDataFromStream(InputStream inputStream) {
        this.bitmap = BitmapFactory.decodeStream(inputStream);
    }

    public class BitmapEntryFactory implements ICacheEntryFactory<BitmapEntry> {

        @Override
        public BitmapEntry createEntry() {
            return new BitmapEntry(null);
        }
    }
}
