package com.nosyjoe.android.common;

import android.content.res.Resources;

/**
 * @author Philipp Engel <philipp@filzip.com>
 */
public class Tools {
    
    private static float scale = -1;

    public static int toDDP(int dip) {
        if (scale < 0) {
            scale = Resources.getSystem().getDisplayMetrics().density;
        }

        return (int) (dip * scale + 0.5f);
    }

}
