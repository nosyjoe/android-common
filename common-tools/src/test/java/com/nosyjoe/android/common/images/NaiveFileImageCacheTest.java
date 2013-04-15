package com.nosyjoe.android.common.images;

import com.xtremelabs.robolectric.Robolectric;
import com.xtremelabs.robolectric.RobolectricTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;

import static org.junit.Assert.assertEquals;

/**
 * @author Philipp Engel <philipp@filzip.com>
 */
@RunWith(RobolectricTestRunner.class)
public class NaiveFileImageCacheTest {

    @Test
    //@Ignore("Must be converted to apklib project first")
    public void testMD5Hash() {
        String baseDir = "/bla/test";
        NaiveFileImageCache naiveFileImageCache = new NaiveFileImageCache(Robolectric.application, baseDir);

        String destinationPath = naiveFileImageCache.getDestinationPath("http://example.com/test.rss");

        
        String expected = baseDir + File.separator + "f481d8d9cb97a3073d92c07bf5cd68e6";

        assertEquals(expected, destinationPath);
    }

}
