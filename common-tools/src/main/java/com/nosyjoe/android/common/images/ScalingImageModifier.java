package com.nosyjoe.android.common.images;

import android.graphics.Bitmap;

/**
 * @author Philipp Engel <philipp@filzip.com>
 */
public class ScalingImageModifier implements IImageModifier{

    private int width;
    private int height;

    public ScalingImageModifier(int width, int height) {
        this.width = width;
        this.height = height;
    }

    @Override
    public Bitmap apply(Bitmap original) {
        if (original != null)
            return Bitmap.createScaledBitmap(original, this.width, this.height, true);
        else
            return null;
    }
}
