package com.nosyjoe.android.common;

import org.junit.Test;

import java.util.ArrayList;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * @author Philipp Engel <philipp@filzip.com>
 */
public class ToolsTest {
    @Test
    public void testJoin() throws Exception {
        ArrayList<Long> list = new ArrayList<Long>();
        list.add(1L);

        String result = Tools.join(list, ",");
        assertThat(result, equalTo("1"));

        list.add(1345L);
        result = Tools.join(list, ",");
        assertThat(result, equalTo("1,1345"));
    }
}
