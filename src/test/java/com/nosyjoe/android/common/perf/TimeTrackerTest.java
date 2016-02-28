package com.nosyjoe.android.common.perf;

import com.xtremelabs.robolectric.RobolectricTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * @author Philipp Engel <philipp@filzip.com>
 */
@RunWith(RobolectricTestRunner.class)
public class TimeTrackerTest {

    public static final String TEST_MARKER = "test_marker";
    private TimeTracker timeTracker;


    @Test
    public void testMarkStartOf() throws Exception {
        assertThat(TimeTracker.INSTANCE.getStartOf(TEST_MARKER), equalTo(-1L));
        long l = TimeTracker.INSTANCE.markStartOf(TEST_MARKER);
        assertThat(TimeTracker.INSTANCE.getStartOf(TEST_MARKER), equalTo(l));
    }

    @Test
    public void testMarkEndOf() throws Exception {

    }

    @Test
    public void testGetTimeDeltaOf() throws Exception {

    }
}
