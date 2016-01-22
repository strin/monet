package com.happen.it.make.whatisit;

import android.app.Application;
import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import org.dmlc.mxnet.ModelLoader;
import org.dmlc.mxnet.Predictor;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import android.os.BatteryManager;

/**
 * Created by Tianlins on Jan 17. 2015.
 */


public class MxNetGauge extends Application {
    private static boolean libLoaded = false;

    public Context context;

    public double statsForwardPass = 0;

    // the time the task is started, and ended in nano seconds.
    public long testStartTime, testEndTime;

    public final String[] filenames = new String[] {
            "cat.jpg",
            "keyboard.jpg",
            "night.jpg",
            "red-car.jpg",
            "sea.jpg",
            "sky.jpg"
    };

    BatteryManager mBatteryManager;

    public MxNetGauge(Context context) {
        this.context = context;
        this.mBatteryManager = (BatteryManager)context.getSystemService(Context.BATTERY_SERVICE);
        this.loadModel("inception");
    }

    public MxNetGauge(Context context, String modelName) {
        this.context = context;
        this.mBatteryManager = (BatteryManager)context.getSystemService(Context.BATTERY_SERVICE);
        this.loadModel(modelName);
    }

    public void runTest(int num_iter) {
        AssetManager assetManager = this.context.getAssets();


        final Bitmap[] bitmaps = new Bitmap[filenames.length];
        final ArrayList<float[]> colors = new ArrayList<>();

        for(int i = 0; i < filenames.length; i++) {
            InputStream stream = null;
            BufferedInputStream bufferedInputStream = null;
            try {
                stream = assetManager.open(filenames[i]);
                bufferedInputStream = new BufferedInputStream(stream);
                bitmaps[i] = BitmapFactory.decodeStream(bufferedInputStream);
                bitmaps[i] = loader.preprocess(bitmaps[i]);
                colors.add(loader.toColor(bitmaps[i]));
            }catch(IOException e) {
                System.err.print(e.toString());
            }
        }

        Predictor predictor = this.getPredictor();
        testStartTime = System.nanoTime();

        long percentage = 0;
        for(int it = 0; it < num_iter; it++) {
            if((double)it/num_iter > percentage + 1) {
                percentage++;
                System.out.println(it + '%');
            }
            for(int i = 0; i < bitmaps.length; i++) {
                predictor.forward("data", colors.get(i));
            }
            final float[] result = predictor.getOutput(0);
        }

        testEndTime = System.nanoTime();

        this.statsForwardPass = (testEndTime - testStartTime) / 10e6 / (double)num_iter / (double)bitmaps.length;
    }

    public String toString() {
        StringBuffer buffer = new StringBuffer();
        buffer.append("forward pass average time = " + this.statsForwardPass + " ms");
        return buffer.toString();
    }

    /* models */
    private List<String> dict;
    private Predictor predictor;
    public ModelLoader loader;
    public Predictor getPredictor() {return predictor;}


    public String getName(int i) {
        if (i >= dict.size()) {
            return "Shit";
        }
        return dict.get(i);
    }

    public int getDatasetSize() {
        return filenames.length;
    }

    public void loadModel(String name) {
        if(name.equals("default")) {
            loader = new ModelLoader() {
                final int SHORTER_SIDE = 256;
                final int DESIRED_SIDE = 224;

                @Override
                public void load() {
                    final byte[] symbol = readRawFile(context, R.raw.symbol);
                    final byte[] params = readRawFile(context, R.raw.params);
                    final Predictor.Device device = new Predictor.Device(Predictor.Device.Type.CPU, 0);
                    final int[] shape = {1, 3, 224, 224};
                    final String key = "data";
                    final Predictor.InputNode node = new Predictor.InputNode(key, shape);

                    predictor = new Predictor(symbol, params, device, new Predictor.InputNode[]{node});
                    dict = readRawTextFile(context, R.raw.synset);


                }

                @Override
                public Bitmap preprocess(final Bitmap origin) {
                    //TODO: error handling
                    final int originWidth = origin.getWidth();
                    final int originHeight = origin.getHeight();
                    int height = SHORTER_SIDE;
                    int width = SHORTER_SIDE;
                    if (originWidth < originHeight) {
                        height = (int)((float)originHeight / originWidth * width);
                    } else {
                        width = (int)((float)originWidth / originHeight * height);
                    }
                    final Bitmap scaled = Bitmap.createScaledBitmap(origin, width, height, false);
                    int y = (height - DESIRED_SIDE) / 2;
                    int x = (width - DESIRED_SIDE) / 2;
                    return Bitmap.createBitmap(scaled, x, y, DESIRED_SIDE, DESIRED_SIDE);
                }

                @Override
                public float[] toColor(final Bitmap bitmap) {
                    ByteBuffer byteBuffer = ByteBuffer.allocate(bitmap.getByteCount());
                    bitmap.copyPixelsToBuffer(byteBuffer);
                    byte[] bytes = byteBuffer.array();
                    float[] colors = new float[bytes.length / 4 * 3];

                    for (int i = 0; i < bytes.length; i += 4) {
                        int j = i / 4;
                        colors[0 * 224 * 224 + j] = ((float) (((int) (bytes[i + 0])) & 0xFF) - 117.0f);
                        colors[1 * 224 * 224 + j] = ((float) (((int) (bytes[i + 1])) & 0xFF) - 117.0f);
                        colors[2 * 224 * 224 + j] = ((float) (((int) (bytes[i + 2])) & 0xFF) - 117.0f);
                    }

                    return colors;
                }
            };

        }else if(name.equals("inception")) {
            loader = new ModelLoader() {
                final int SHAPE = 299;

                @Override
                public void load() {
                    final byte[] symbol = readRawFile(context, R.raw.inception_symbol);
                    final byte[] params = readRawFile(context, R.raw.inception_params);

                    final Predictor.Device device = new Predictor.Device(Predictor.Device.Type.CPU, 0);
                    final int[] shape = {1, 3, SHAPE, SHAPE};
                    final String key = "data";
                    final Predictor.InputNode node = new Predictor.InputNode(key, shape);

                    predictor = new Predictor(symbol, params, device, new Predictor.InputNode[]{node});
                    dict = readRawTextFile(context, R.raw.inception_synset);

                }

                @Override
                public Bitmap preprocess(final Bitmap origin) {
                    // first crop the image from center.
                    final int originWidth = origin.getWidth(),
                            originHeight = origin.getHeight();
                    int shortEdge = Math.min(originWidth, originHeight);
                    int x = (originWidth - shortEdge) / 2;
                    int y = (originHeight - shortEdge) / 2;
                    final Bitmap cropped = Bitmap.createBitmap(origin, x, y, shortEdge, shortEdge);
                    return Bitmap.createScaledBitmap(cropped, SHAPE, SHAPE, false);
                }

                @Override
                public float[] toColor(final Bitmap bitmap) {
                    ByteBuffer byteBuffer = ByteBuffer.allocate(bitmap.getByteCount());
                    bitmap.copyPixelsToBuffer(byteBuffer);
                    byte[] bytes = byteBuffer.array();
                    float[] colors = new float[bytes.length / 4 * 3];

                    for (int i = 0; i < bytes.length; i += 4) {
                        int j = i / 4;
                        colors[0 * SHAPE * SHAPE + j] = ((float) (((int) (bytes[i + 0])) & 0xFF) - 128.0f) / 128.0f;
                        colors[1 * SHAPE * SHAPE + j] = ((float) (((int) (bytes[i + 1])) & 0xFF) - 128.0f) / 128.0f;
                        colors[2 * SHAPE * SHAPE + j] = ((float) (((int) (bytes[i + 2])) & 0xFF) - 128.0f) / 128.0f;
                    }

                    return colors;
                }
            };
        }else if(name.equals("inception-bn")) {
            loader = new ModelLoader() {
                @Override
                public void load() {
                    final byte[] symbol = readRawFile(context, R.raw.inception_bn_symbol);
                    final byte[] params = readRawFile(context, R.raw.inception_bn_params);

                    final Predictor.Device device = new Predictor.Device(Predictor.Device.Type.CPU, 0);
                    final int[] shape = {1, 3, 224, 224};
                    final String key = "data";
                    final Predictor.InputNode node = new Predictor.InputNode(key, shape);

                    predictor = new Predictor(symbol, params, device, new Predictor.InputNode[]{node});
                    dict = readRawTextFile(context, R.raw.inception_bn_synset);

                }

                @Override
                public Bitmap preprocess(final Bitmap origin) {
                    // first crop the image from center.
                    final int originWidth = origin.getWidth(),
                            originHeight = origin.getHeight();
                    int shortEdge = Math.min(originWidth, originHeight);
                    int x = (originWidth - shortEdge) / 2;
                    int y = (originHeight - shortEdge) / 2;
                    final Bitmap cropped = Bitmap.createBitmap(origin, x, y, shortEdge, shortEdge);
                    return Bitmap.createScaledBitmap(cropped, 224, 224, false);
                }

                @Override
                public float[] toColor(final Bitmap bitmap) {
                    ByteBuffer byteBuffer = ByteBuffer.allocate(bitmap.getByteCount());
                    bitmap.copyPixelsToBuffer(byteBuffer);
                    byte[] bytes = byteBuffer.array();
                    float[] colors = new float[bytes.length / 4 * 3];

                    for (int i = 0; i < bytes.length; i += 4) {
                        int j = i / 4;
                        // TODO: for now use 117f as mean. next step is to load the mean file.
                        colors[0 * 224 * 224 + j] = ((float) (((int) (bytes[i + 0])) & 0xFF) - 117.0f);
                        colors[1 * 224 * 224 + j] = ((float) (((int) (bytes[i + 1])) & 0xFF) - 117.0f);
                        colors[2 * 224 * 224 + j] = ((float) (((int) (bytes[i + 2])) & 0xFF) - 117.0f);
                    }

                    return colors;
                }
            };
        }

        loader.load();
        predictor = loader.getPredictor();
        dict = loader.getSynsetDict();
    }

    public static byte[] readRawFile(Context ctx, int resId)
    {
        ByteArrayOutputStream outputStream=new ByteArrayOutputStream();
        int size = 0;
        byte[] buffer = new byte[1024];
        try (InputStream ins = ctx.getResources().openRawResource(resId)) {
            while((size=ins.read(buffer,0,1024))>=0){
                outputStream.write(buffer,0,size);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return outputStream.toByteArray();
    }

    public static List<String> readRawTextFile(Context ctx, int resId)
    {
        List<String> result = new ArrayList<>();
        InputStream inputStream = ctx.getResources().openRawResource(resId);

        InputStreamReader inputreader = new InputStreamReader(inputStream);
        BufferedReader buffreader = new BufferedReader(inputreader);
        String line;

        try {
            while (( line = buffreader.readLine()) != null) {
                result.add(line);
            }
        } catch (IOException e) {
            return null;
        }
        return result;
    }



//    private static Bitmap processBitmap(final Bitmap origin) {
//        //TODO: error handling
//        final int originWidth = origin.getWidth();
//        final int originHeight = origin.getHeight();
//        int height = SHORTER_SIDE;
//        int width = SHORTER_SIDE;
//        if (originWidth < originHeight) {
//            height = (int)((float)originHeight / originWidth * width);
//        } else {
//            width = (int)((float)originWidth / originHeight * height);
//        }
//        final Bitmap scaled = Bitmap.createScaledBitmap(origin, width, height, false);
//        int y = (height - DESIRED_SIDE) / 2;
//        int x = (width - DESIRED_SIDE) / 2;
//        return Bitmap.createBitmap(scaled, x, y, DESIRED_SIDE, DESIRED_SIDE);
//    }



}
