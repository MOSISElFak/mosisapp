package com.demo.mosisapp;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class MyGPSReceiver extends BroadcastReceiver
{
    private String TAG = "MyGPSReceiver";
    private Context bContext;

    @Override
    public void onReceive(Context context, Intent intent) {
        if ((intent.getAction().equals("android.location.MODE_CHANGED"))
                || (intent.getAction().equals("android.location.PROVIDERS_CHANGED"))) {
            bContext = context;

            Log.d(TAG,"called");
            boolean service = isMyServiceRunning(MyLocationService.class);
            // If myLocation is not currently running, ignore signal
            if (!service) return;
            // If service is running, notify that there was a change
            // Service will decide on notifying UI
            Intent notifyService = new Intent(context, MyLocationService.class);
            notifyService.putExtra("broadcast",true);
            context.startService(notifyService);
        }
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) bContext.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
               Log.d(TAG, "Service running");
                return true;
            }
        }
        Log.d(TAG,"Service NOT RUNNING");
        return false;
    }
}