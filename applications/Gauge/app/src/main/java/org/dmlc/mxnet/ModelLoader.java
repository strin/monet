package org.dmlc.mxnet;

import android.content.Context;
import android.graphics.Bitmap;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UnknownFormatConversionException;

/**
 * Created by tianlins on 1/21/16.
 */
public class ModelLoader {
    protected Predictor predictor;
    protected List<String> dict;

    public ModelLoader() {
    }

    public void load() throws UnsupportedOperationException {
        throw new UnsupportedOperationException("model load function not implemented.");
    }

    public Bitmap preprocess(final Bitmap origin) {
        return origin; // no preprocess.
    }

    public float[] toColor(final Bitmap bitmap) {
        throw new UnsupportedOperationException("model to color function not implemented.");
    }

    public Predictor getPredictor() {
        return predictor;
    }

    public List<String> getSynsetDict() {
        return dict;
    }

}
