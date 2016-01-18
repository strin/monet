package com.happen.it.make.whatisit;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;

import org.dmlc.mxnet.Predictor;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by leliana on 8/5/15.
 */
public class WhatsApplication extends Application{
    @Override
    public void onCreate() {
        super.onCreate();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_BATTERY_CHANGED);
        registerReceiver(mBroadcastReceiver, filter);

        MxNetGauge mxNetGauge = new MxNetGauge(getApplicationContext(), 10);
        mxNetGauge.runTest();
        System.out.println(mxNetGauge.toString());
    }

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            DecimalFormat formatter = new DecimalFormat();

            String action = intent.getAction();

            // store battery information received from BatteryManager
            if (action.equals(Intent.ACTION_BATTERY_CHANGED)) {
                int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, 0);
                int health = intent.getIntExtra(BatteryManager.EXTRA_HEALTH, 0);
                boolean present = intent.getBooleanExtra(
                        BatteryManager.EXTRA_PRESENT, false);
                int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
                int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 0);
                int icon_small = intent.getIntExtra(
                        BatteryManager.EXTRA_ICON_SMALL, 0);
                int plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED,
                        0);
                int voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE,
                        0);
                int temperature = intent.getIntExtra(
                        BatteryManager.EXTRA_TEMPERATURE, 0);
                String technology = intent
                        .getStringExtra(BatteryManager.EXTRA_TECHNOLOGY);

                // display the battery icon that fits the current battery status (charging/discharging)

                // create TextView of the remaining information , to display to screen.
                String statusString = "";

                switch (status) {
                    case BatteryManager.BATTERY_STATUS_UNKNOWN:
                        statusString = "unknown";
                        break;
                    case BatteryManager.BATTERY_STATUS_CHARGING:
                        statusString = "charging";
                        break;
                    case BatteryManager.BATTERY_STATUS_DISCHARGING:
                        statusString = "discharging";
                        break;
                    case BatteryManager.BATTERY_STATUS_NOT_CHARGING:
                        statusString = "not charging";
                        break;
                    case BatteryManager.BATTERY_STATUS_FULL:
                        statusString = "full";
                        break;
                }

                String healthString = "";

                switch (health) {
                    case BatteryManager.BATTERY_HEALTH_UNKNOWN:
                        healthString = "unknown";
                        break;
                    case BatteryManager.BATTERY_HEALTH_GOOD:
                        healthString = "good";
                        break;
                    case BatteryManager.BATTERY_HEALTH_OVERHEAT:
                        healthString = "overheat";
                        break;
                    case BatteryManager.BATTERY_HEALTH_DEAD:
                        healthString = "dead";
                        break;
                    case BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE:
                        healthString = "over voltage";
                        break;
                    case BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE:
                        healthString = "unspecified failure";
                        break;
                }

                String acString = "";

                switch (plugged) {
                    case BatteryManager.BATTERY_PLUGGED_AC:
                        acString = "plugged AC";
                        break;
                    case BatteryManager.BATTERY_PLUGGED_USB:
                        acString = "plugged USB";
                        break;
                    default:
                        acString = "not plugged";
                }


                formatter.applyPattern("#");
                String levelStr = formatter.format((float) level / scale * 100);
                System.out.println("level:" + levelStr + "% (out of 100)\n");


                // voltage is reported in millivolts
                formatter.applyPattern(".##");
                String voltageStr = formatter.format((float) voltage / 1000);
                System.out.println("voltage:" + voltageStr + "V\n");

                // temperature is reported in tenths of a degree Centigrade (from BatteryService.java)
                formatter.applyPattern(".#");
                String temperatureStr = formatter.format((float) temperature / 10);
                System.out.println("temperature:" + temperatureStr
                        + "C" + "\n");


                //finish();
            }
        }
    };
}
