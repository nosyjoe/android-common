package com.nosyjoe.android.common.images;

import android.graphics.Bitmap;

/**
 * 
 */
public interface IImageModifier {

    /**
     * Takes the original image, applies modifications to a copy of the bitmap. Never modifies the original Bitmap
     * @param original the original image material, never altered.
     * @return a modified copy of the souce bitmap
     */
    Bitmap apply(Bitmap original);
    
}
