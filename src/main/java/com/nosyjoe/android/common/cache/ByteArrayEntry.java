package com.nosyjoe.android.common.cache;

import com.nosyjoe.android.common.images.Util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author Philipp Engel <philipp@filzip.com>
 */
public class ByteArrayEntry implements ICacheEntry<byte[]> {

    private byte[] bytes;

    public ByteArrayEntry(byte[] bytes) {
        this.bytes = bytes;
    }

    @Override
    public int getSize() {
        return bytes.length;
    }

    @Override
    public byte[] getData() {
        return bytes;
    }

    @Override
    public void writeDataToStream(OutputStream outputStream) {
        try {
            outputStream.write(bytes);
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    @Override
    public void readDataFromStream(InputStream inputStream) {
        try {
            bytes = Util.readFully(inputStream);
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    public static class ByteArrayEntryFactory implements ICacheEntryFactory<ByteArrayEntry> {

        @Override
        public ByteArrayEntry createEntry() {
            return new ByteArrayEntry(null);
        }
    }
}
