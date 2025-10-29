package com.abdurrahman.toolsforandroid.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class SharedPrefs {

    private static final String PREFS_NAME = "ToolsForAndroidPrefs";
    private SharedPreferences prefs;

    // Preference keys
    private static final String KEY_SHAKE_FLASHLIGHT = "shake_flashlight";
    private static final String KEY_AUTO_SILENT = "auto_silent";

    public SharedPrefs(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    // Shake Flashlight
    public boolean isShakeFlashlightEnabled() {
        return prefs.getBoolean(KEY_SHAKE_FLASHLIGHT, false);
    }

    public void setShakeFlashlightEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_SHAKE_FLASHLIGHT, enabled).apply();
    }

    // Auto Silent Mode
    public boolean isAutoSilentEnabled() {
        return prefs.getBoolean(KEY_AUTO_SILENT, false);
    }

    public void setAutoSilentEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_AUTO_SILENT, enabled).apply();
    }
}
