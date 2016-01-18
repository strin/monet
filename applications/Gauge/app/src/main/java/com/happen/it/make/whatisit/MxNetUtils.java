package com.happen.it.make.whatisit;

import android.graphics.Bitmap;

import org.dmlc.mxnet.Predictor;

import java.nio.ByteBuffer;

/**
 * Created by leliana on 11/6/15.
 */
public class MxNetUtils {
    private static boolean libLoaded = false;
    private MxNetUtils() {}

    public static String identifyImage(final Bitmap bitmap) {
        return "";
    }
}
