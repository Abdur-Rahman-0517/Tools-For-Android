package com.abdurrahman.toolsforandroid.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.abdurrahman.toolsforandroid.services.SensorService;
import com.abdurrahman.toolsforandroid.utils.SharedPrefs;

public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction()) || 
            "android.intent.action.QUICKBOOT_POWERON".equals(intent.getAction())) {
            
            SharedPrefs prefs = new SharedPrefs(context);
            
            // Start service if any background feature is enabled
            if (prefs.isShakeFlashlightEnabled() || prefs.isAutoSilentEnabled()) {
                Intent serviceIntent = new Intent(context, SensorService.class);
                try {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        context.startForegroundService(serviceIntent);
                    } else {
                        context.startService(serviceIntent);
                    }
                    Log.d(TAG, "SensorService started on boot");
                } catch (SecurityException e) {
                    Log.e(TAG, "Security exception starting service on boot", e);
                } catch (Exception e) {
                    Log.e(TAG, "Error starting service on boot", e);
                }
            }
        }
    }
}
