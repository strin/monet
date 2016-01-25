package monet.experiment;

import android.os.*;
import android.os.Process;
import android.util.Log;

import com.happen.it.make.whatisit.MxNetGauge;
import com.happen.it.make.whatisit.WhatsActivity;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Created by tianlins on 1/22/16.
 */
public class ProcessLevel implements  Experiment {
    public static String EXPERIMENT_ID = "process_level";
    private static int[] priorities = {
            android.os.Process.THREAD_PRIORITY_LOWEST,
            android.os.Process.THREAD_PRIORITY_BACKGROUND,
            android.os.Process.THREAD_PRIORITY_AUDIO,
            android.os.Process.THREAD_PRIORITY_DEFAULT,
            android.os.Process.THREAD_PRIORITY_DISPLAY,
            Process.THREAD_PRIORITY_LESS_FAVORABLE
    };

    private static int num_repeat = 100;

    private WhatsActivity activity;

    private FileWriter log;

    private MxNetGauge mxNetGauge;

    public ProcessLevel(WhatsActivity activity, String modelName) {
        this.activity = activity;
        File file = new File("/sdcard/" + activity.runId + "_" + EXPERIMENT_ID + "_" + modelName + ".txt");
        file.delete();
        try {
            this.log = new FileWriter(file, false);
        }catch(IOException e) {
            e.printStackTrace();
        }

        System.out.println("[model] loading...");
        mxNetGauge = new MxNetGauge(activity.getApplicationContext(), modelName);
        System.out.println("[model] loading complete.");
    }

    public void run() {
        for(int priority : this.priorities) {
            // set prioirty
            android.os.Process.setThreadPriority(priority);

            System.out.println("run with priority" + priority);
            mxNetGauge.runTest(num_repeat);

            double energy = activity.getTotalEnergy(mxNetGauge.testStartTime, mxNetGauge.testEndTime);
            double avgTime = mxNetGauge.statsForwardPass;

            System.out.println("total energy = " + energy);
            System.out.println("average time = " + avgTime);

            try {
                log.write("priority " + priority
                        + " time " + avgTime + " energy " + energy + "\n");
                log.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        System.out.println("experiment ended");
    }

    public MxNetGauge getGauge() {
        return this.mxNetGauge;
    }
}
