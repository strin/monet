package com.happen.it.make.whatisit;

import android.app.Activity;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import org.dmlc.mxnet.Predictor;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import android.os.BatteryManager;
import android.content.Context;
import android.util.Log;

import edu.umich.PowerTutor.util.SystemInfo;

/**
 * Created by Tianlins on Jan 17. 2015.
 */

class BitmapContainer {
    public float[] colors;
    public BitmapContainer(Bitmap bitmap, Map<String, Float> mean) {
        ByteBuffer byteBuffer = ByteBuffer.allocate(bitmap.getByteCount());
        bitmap.copyPixelsToBuffer(byteBuffer);
        byte[] bytes = byteBuffer.array();
        colors = new float[bytes.length / 4 * 3];

        float mean_b = mean.get("b");
        float mean_g = mean.get("g");
        float mean_r = mean.get("r");
        for (int i = 0; i < bytes.length; i += 4) {
            int j = i / 4;
            colors[0 * 224 * 224 + j] = (float) (((int) (bytes[i + 0])) & 0xFF) - mean_r;
            colors[1 * 224 * 224 + j] = (float) (((int) (bytes[i + 1])) & 0xFF) - mean_g;
            colors[2 * 224 * 224 + j] = (float) (((int) (bytes[i + 2])) & 0xFF) - mean_b;
        }
    }
};

public class MxNetGauge extends Application {
    private static boolean libLoaded = false;

    public int num_iter = 1000;
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

    public MxNetGauge(Context context, int num_iter) {
        this.num_iter = num_iter;
        this.context = context;
        this.mBatteryManager = (BatteryManager)context.getSystemService(Context.BATTERY_SERVICE);
        this.loadModel();
    }

    public void runTest() {
        AssetManager assetManager = this.context.getAssets();


        final Bitmap[] bitmaps = new Bitmap[filenames.length];
        final BitmapContainer[] containers = new BitmapContainer[filenames.length];

        for(int i = 0; i < filenames.length; i++) {
            InputStream stream = null;
            BufferedInputStream bufferedInputStream = null;
            try {
                stream = assetManager.open(filenames[i]);
                bufferedInputStream = new BufferedInputStream(stream);
                bitmaps[i] = BitmapFactory.decodeStream(bufferedInputStream);
                bitmaps[i] = processBitmap(bitmaps[i]);
                containers[i] = new BitmapContainer(bitmaps[i], this.mean);
            }catch(IOException e) {
                System.err.print(e.toString());
            }
        }

        Predictor predictor = this.getPredictor();
        testStartTime = System.nanoTime();

        long percentage = 0;
        for(int it = 0; it < this.num_iter; it++) {
            if((double)it/this.num_iter > percentage + 1) {
                percentage++;
                System.out.println(it + '%');
            }
            for(int i = 0; i < bitmaps.length; i++) {
                predictor.forward("data", containers[i].colors);
            }
            final float[] result = predictor.getOutput(0);
        }

        testEndTime = System.nanoTime();

        this.statsForwardPass = (testEndTime - testStartTime) / 10e6 / (double)this.num_iter / (double)bitmaps.length;
    }

    public String toString() {
        StringBuffer buffer = new StringBuffer();
        buffer.append("forward pass average time = " + this.statsForwardPass + " ms");
        return buffer.toString();
    }


    private List<String> dict;
    private Map<String, Float> mean;
    private Predictor predictor;
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

    public Map<String, Float> getMean() {
        return mean;
    }

    public void loadModel() {
        final byte[] symbol = readRawFile(this.context, R.raw.symbol);
        final byte[] params = readRawFile(this.context, R.raw.params);
        final Predictor.Device device = new Predictor.Device(Predictor.Device.Type.CPU, 0);
        final int[] shape = {1, 3, 224, 224};
        final String key = "data";
        final Predictor.InputNode node = new Predictor.InputNode(key, shape);

        predictor = new Predictor(symbol, params, device, new Predictor.InputNode[]{node});
        dict = readRawTextFile(this.context, R.raw.synset);
        try {
            final StringBuilder sb = new StringBuilder();
            final List<String> lines = readRawTextFile(this.context, R.raw.mean);
            for (final String line : lines) {
                sb.append(line);
            }
            final JSONObject meanJson = new JSONObject(sb.toString());
            mean = new HashMap<>();
            mean.put("b", (float) meanJson.optDouble("b"));
            mean.put("g", (float) meanJson.optDouble("g"));
            mean.put("r", (float) meanJson.optDouble("r"));
        } catch (JSONException e) {
            e.printStackTrace();
        }
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

    static final int SHORTER_SIDE = 256;
    static final int DESIRED_SIDE = 224;

    private static Bitmap processBitmap(final Bitmap origin) {
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

    public float getBatteryLevel() {
        Intent batteryIntent = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

        // Error checking that probably isn't needed but I added just in case.
        if(level == -1 || scale == -1) {
            return 50.0f;
        }

        return ((float)level / (float)scale) * 100.0f;
    }


}
