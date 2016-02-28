package com.nosyjoe.android.common.cache;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * A generic cache entry.
 *
 * @author Philipp Engel <philipp@filzip.com>
 */
public interface ICacheEntry<T> {

    /**
     * Returns the size of the cache entry
     * @return
     */
    int getSize();

    /**
     * Returns the data that the cache entry represents
     * @return
     */
    T getData();

    /**
     * Writes the data of the entry to the output stream. The implementation is not required to take care of
     * closing the stream.
     *
     * @param outputStream the output stream to write the data of the cache entry to
     */
    void writeDataToStream(OutputStream outputStream);

    /**
     * Reads the data of the cache entry from an input stream. The implementation is not required to take care of
     * closing the stream.
     *
     * @param inputStream the stream to read the data from
     */
    void readDataFromStream(InputStream inputStream);
}
