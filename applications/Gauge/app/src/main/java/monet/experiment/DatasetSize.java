package monet.experiment;

import android.provider.ContactsContract;

import com.happen.it.make.whatisit.MxNetGauge;
import com.happen.it.make.whatisit.WhatsActivity;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Created by tianlins on 1/20/16.
 * In order to analyze the effect of dataset size on performance and energy.
 */
public class DatasetSize implements Experiment {
    public static String EXPERIMENT_ID = "datasize";
    private static int[] repeats = {10, 20, 30, 40, 50, 60};

    private  WhatsActivity activity;
    private double[] energy, time;

    private FileWriter log;

    public DatasetSize(WhatsActivity activity) {
        this.activity = activity;
        this.energy = new double[repeats.length];
        this.time = new double[repeats.length];

        File file = new File("/sdcard/" + activity.runId + "_" + EXPERIMENT_ID + ".txt");
        file.delete();
        try {
            this.log = new FileWriter(file, false);
        }catch(IOException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        for(int ri = 0; ri < repeats.length; ri++) {
            System.out.println("run #" + ri);
            int numRepeat = this.repeats[ri];
            MxNetGauge mxNetGauge = new MxNetGauge(activity.getApplicationContext(), numRepeat);
            mxNetGauge.runTest();
            this.energy[ri] = activity.getTotalEnergy(mxNetGauge.testStartTime, mxNetGauge.testEndTime);
            this.time[ri] = (mxNetGauge.testEndTime - mxNetGauge.testStartTime) / 10e6;
            System.out.println("total energy = " + energy[ri]);
            System.out.println("total time = " + time[ri]);

            try {
                log.write("size " + mxNetGauge.getDatasetSize() * numRepeat
                        + " time " + time[ri] + " energy " + energy[ri] + "\n");
                log.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        System.out.println("experiment ended");
    }
}
