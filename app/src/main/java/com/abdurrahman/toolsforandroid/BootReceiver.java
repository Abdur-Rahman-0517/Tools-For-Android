package com.abdurrahman.toolsforandroid.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.abdurrahman.toolsforandroid.services.SensorService;
import com.abdurrahman.toolsforandroid.utils.SharedPrefs;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            SharedPrefs prefs = new SharedPrefs(context);
            
            // Start service if any background feature is enabled
            if (prefs.isShakeFlashlightEnabled() || prefs.isAutoSilentEnabled()) {
                Intent serviceIntent = new Intent(context, SensorService.class);
                context.startService(serviceIntent);
                Log.d("BootReceiver", "SensorService started on boot");
            }
        }
    }
}
