package com.happen.it.make.whatisit;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import org.dmlc.mxnet.Predictor;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * Created by leliana on 11/6/15.
 */
public class MxNetUtils {
    private static boolean libLoaded = false;
    private MxNetUtils() {}

    public static String identifyImage(MxNetGauge gauge, final Bitmap bitmap) {
        float[] colors = gauge.loader.toColor(gauge.loader.preprocess(bitmap));

        Predictor predictor = gauge.getPredictor();
        predictor.forward("data", colors);

        final float[] result = predictor.getOutput(0);
        int index = 0;
        for (int i = 0; i < result.length; ++i) {
            if (result[index] < result[i]) index = i;
        }

        String tag = gauge.getName(index);
        return tag;
    }
}
