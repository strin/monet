package com.happen.it.make.whatisit;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.v4.util.Pair;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

import edu.umich.PowerTutor.service.ICounterService;
import edu.umich.PowerTutor.service.PowerEstimator;
import edu.umich.PowerTutor.service.UMLoggerService;
import edu.umich.PowerTutor.service.UidInfo;
import edu.umich.PowerTutor.util.Counter;
import edu.umich.PowerTutor.util.SystemInfo;
import monet.experiment.DatasetSize;
import monet.experiment.Experiment;

public class WhatsActivity extends AppCompatActivity implements Runnable {

    private TextView resultTextView;
    private ImageView inputImageView;
    private Bitmap bitmap;
    private Bitmap processedBitmap;
    private Button identifyButton;
    private SharedPreferences sharedPreferences;
    private String currentPhotoPath;
    private static final String PREF_USE_CAMERA_KEY = "USE_CAMERA";

    // energy profiler service.
    private static final String TAG = "PowerProfiler";
    private ICounterService counterService;
    private Intent serviceIntent;
    private Handler handler;
    private CounterServiceConnection conn;
    private int noUidMask;
    private String[] componentNames;

    // expreriment variables.
    public long runId = 0;
    private FileWriter energyVsTimeLog;
    private long startTime = 0;
    private ArrayList<Pair<Double, Double>> timeVsEnergy;
    private Experiment experiment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_whats);
        identifyButton = (Button)findViewById(R.id.identify_button);
        inputImageView = (ImageView)findViewById(R.id.tap_to_add_image);
        resultTextView = (TextView)findViewById(R.id.result_text);
        sharedPreferences = getSharedPreferences("Picture Pref", Context.MODE_PRIVATE);

        // get experiment settings.
        String appName = this.getApplicationName();
        this.runId = System.currentTimeMillis();
        System.out.println("runId = " + runId);
        this.startTime = System.nanoTime();

        this.timeVsEnergy = new ArrayList<>();
        this.timeVsEnergy.add(new Pair(new Double(getCurrentRunTime()), new Double(0)));

        File file = new File("/sdcard/" + runId + "_energy_time.txt");
        file.delete();
        try {
            this.energyVsTimeLog = new FileWriter(file, false);
        }catch(IOException e) {
            e.printStackTrace();
        }

        // create PowerTutor service.
        serviceIntent = new Intent(this, UMLoggerService.class);
        conn = new CounterServiceConnection();

        if(conn == null) {
            System.err.println("connection to Profiler service failed to be established.");
        } else {
            startService(serviceIntent);
            System.out.println("service started");
        }

        if(savedInstanceState != null) {
            componentNames = savedInstanceState.getStringArray("componentNames");
            noUidMask = savedInstanceState.getInt("noUidMask");
        }

        getApplicationContext().bindService(serviceIntent, conn, BIND_AUTO_CREATE);

        // run test code.
        // experiments can be
        // 1. DatasetSize. effect of dataset size on energy and performance.
        experiment = new DatasetSize(WhatsActivity.this, "inception-bn");
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                experiment.run();
            }
        });

        // use this to start experiment.
        thread.start();
        handler = new Handler();
        handler.postDelayed(this, 100);

        identifyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (v != identifyButton) {
                    return;
                }
                if (processedBitmap == null) {
                    return;
                }

                new AsyncTask<Bitmap, Void, String>(){
                    private double timeElapsed;
                    private long numRuns = 1000000;

                    @Override
                    protected void onPreExecute() {
                        resultTextView.setText("Calculating...");
                    }

                    @Override
                    protected String doInBackground(Bitmap... bitmaps) {
                        synchronized (identifyButton) {
                            String tag = MxNetUtils.identifyImage(experiment.getGauge(), bitmaps[0]);
                            return tag;
                        }
                    }
                    @Override
                    protected void onPostExecute(String tag) {
                        System.out.println("total time used = " + timeElapsed);
                        System.out.println("throughput = " + numRuns / (float)timeElapsed);
                        System.out.println("numRuns = " + numRuns);
                        resultTextView.setText(tag + "/" + timeElapsed + "/" + numRuns / (float) timeElapsed + "/" + numRuns);
                    }
                }.execute(processedBitmap);
            }


        });

        inputImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (v != inputImageView) {
                    return;
                }
                final boolean useCamera = sharedPreferences.getBoolean(PREF_USE_CAMERA_KEY, false);
                if (useCamera) {
                    dispatchTakePictureIntent();
                } else {
                    final Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
                    photoPickerIntent.setType("image/*");
                    startActivityForResult(photoPickerIntent, Constants.SELECT_PHOTO_CODE);
                }
            }
        });


    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                ex.printStackTrace();
                return;
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,
                        Uri.fromFile(photoFile));
                startActivityForResult(takePictureIntent, Constants.CAPTURE_PHOTO_CODE);
            }
        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (processedBitmap != null) {
            inputImageView.setImageBitmap(processedBitmap);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_whats, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_use_camera) {
            sharedPreferences.edit().putBoolean(PREF_USE_CAMERA_KEY, true).apply();
            return true;
        } else if (id == R.id.action_use_gallery) {
            sharedPreferences.edit().putBoolean(PREF_USE_CAMERA_KEY, false).apply();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) {
        super.onActivityResult(requestCode, resultCode, imageReturnedIntent);

        switch(requestCode) {
            case Constants.SELECT_PHOTO_CODE:
                if(resultCode == RESULT_OK){
                    try {
                        final Uri imageUri = imageReturnedIntent.getData();
                        final InputStream imageStream = getContentResolver().openInputStream(imageUri);
                        bitmap = BitmapFactory.decodeStream(imageStream);
                        processedBitmap = processBitmap(bitmap);
                        inputImageView.setImageBitmap(processedBitmap);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }

                }
                break;
            case Constants.CAPTURE_PHOTO_CODE:
                if (resultCode == RESULT_OK) {
                    bitmap = BitmapFactory.decodeFile(currentPhotoPath);
                    processedBitmap = processBitmap(bitmap);
                    inputImageView.setImageBitmap(bitmap);
                }
                break;
        }
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


    // energy gauge.
    public TextView loadingText = null;
    private void refreshView() {
//        System.out.println("refreshing energy profiles.");
        if (counterService == null) {
            loadingText = new TextView(this);
            loadingText.setText("Waiting for profiler service...");
            loadingText.setGravity(Gravity.CENTER);
            setContentView(loadingText);
            return;
        }else if(loadingText != null) {
            setContentView(R.layout.activity_whats);
            loadingText = null;
        }


        try {
            int ignoreMask = 1;
            byte[] rawUidInfo = counterService.getUidInfo(Counter.WINDOW_TOTAL, noUidMask | ignoreMask);
            if (rawUidInfo != null) {
                UidInfo[] uidInfos = (UidInfo[]) new ObjectInputStream(
                        new ByteArrayInputStream(rawUidInfo)).readObject();
                double total = 0;
                for (UidInfo uidInfo : uidInfos) {
                    if (uidInfo.uid == SystemInfo.AID_ALL) continue;
                    String name = this.getNameByUid(uidInfo.uid);
                    if(name == this.getApplicationName()) { // only show the data for this app.
//                        System.out.println("[" + name + "] power usage");
//                        System.out.println("currentPower: " + uidInfo.currentPower);
//                        System.out.println("total energy: " + uidInfo.totalEnergy);
//                        System.out.println("average power: " + uidInfo.totalEnergy /
//                                (uidInfo.runtime == 0 ? 1 : uidInfo.runtime));

                        double currTime = getCurrentRunTime();
                        this.timeVsEnergy.add(new Pair(new Double(currTime), new Double(uidInfo.totalEnergy)));
                        this.energyVsTimeLog.write("time " + currTime + " energy " + uidInfo.totalEnergy + "\n");
                        this.energyVsTimeLog.flush();
                    }
                }


            }
        } catch (IOException e) {
        } catch (RemoteException e) {
        } catch (ClassNotFoundException e) {
        } catch (ClassCastException e) {
        }
    }

    private String getNameByUid(int uid) {
        SystemInfo sysInfo = SystemInfo.getInstance();
        PackageManager pm = getApplicationContext().getPackageManager();
        return sysInfo.getUidName(uid, pm);
    }

    private String getApplicationName() {
        int stringId = getApplicationContext().getApplicationInfo().labelRes;
        return getApplicationContext().getString(stringId);
    }

    private class CounterServiceConnection implements ServiceConnection {
        public void onServiceConnected(ComponentName className,
                                       IBinder boundService ) {
            System.out.println("service connected");
            counterService = ICounterService.Stub.asInterface((IBinder)boundService);
            try {
                componentNames = counterService.getComponents();
                noUidMask = counterService.getNoUidMask();
            } catch(RemoteException e) {
                counterService = null;
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            counterService = null;
            getApplicationContext().unbindService(conn);
            getApplicationContext().bindService(serviceIntent, conn, 0);
            Log.w(TAG, "Unexpectedly lost connection to service");
        }
    }

    public void run() {
        refreshView();
        if(handler != null) {
            handler.postDelayed(this, 2 * PowerEstimator.ITERATION_INTERVAL);
        }
    }

    /* experiment utils. */

    // return the current run time in ms.
    public double getCurrentRunTime() {
        long nanoTime = System.nanoTime();
        return (double)(nanoTime - this.startTime) / 10e6;
    }

    public double getTotalEnergy(long startTimeNano, long endTimeNano) {
        double startTime = (startTimeNano - this.startTime) / 10e6;
        double endTime = (endTimeNano - this.startTime) / 10e6;

        int i;
        double startEnergy, endEnergy;
        for(i = 0; i < this.timeVsEnergy.size(); i++) {
            if(this.timeVsEnergy.get(i).first < startTime) continue;
            else break;
        }

        if(i == 0) startEnergy = 0;
        else if(i == this.timeVsEnergy.size())
            startEnergy = this.timeVsEnergy.get(this.timeVsEnergy.size()-1).second;
        else{
            startEnergy = this.timeVsEnergy.get(i-1).second
                    + (this.timeVsEnergy.get(i).second - this.timeVsEnergy.get(i-1).second)
                    * (startTime - this.timeVsEnergy.get(i-1).first)
                    / (this.timeVsEnergy.get(i).first - this.timeVsEnergy.get(i-1).first);
        }

        for(i = 0; i < this.timeVsEnergy.size(); i++) {
            if(this.timeVsEnergy.get(i).first < endTime) continue;
            else break;
        }

        if(i == 0) endEnergy = 0;
        else if(i == this.timeVsEnergy.size())
            endEnergy = this.timeVsEnergy.get(this.timeVsEnergy.size()-1).second;
        else{
            endEnergy = this.timeVsEnergy.get(i-1).second
                    + (this.timeVsEnergy.get(i).second - this.timeVsEnergy.get(i-1).second)
                    * (endTime - this.timeVsEnergy.get(i-1).first)
                    / (this.timeVsEnergy.get(i).first - this.timeVsEnergy.get(i-1).first);
        }

        return endEnergy - startEnergy;
    }
}
