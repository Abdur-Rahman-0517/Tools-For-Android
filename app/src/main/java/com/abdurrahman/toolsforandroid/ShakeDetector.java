package com.abdurrahman.toolsforandroid.utils;

import android.hardware.SensorEvent;

public class ShakeDetector {

    public interface OnShakeListener {
        void onShake(int count);
    }

    private static final float SHAKE_THRESHOLD_GRAVITY = 2.7F;
    private static final int SHAKE_SLOP_TIME_MS = 500;
    private static final int SHAKE_COUNT_RESET_TIME_MS = 3000;

    private OnShakeListener listener;
    private long shakeTimestamp;
    private int shakeCount;

    public ShakeDetector(OnShakeListener listener) {
        this.listener = listener;
    }

    public void onSensorChanged(SensorEvent event) {
        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];

        float gX = x / android.hardware.SensorManager.GRAVITY_EARTH;
        float gY = y / android.hardware.SensorManager.GRAVITY_EARTH;
        float gZ = z / android.hardware.SensorManager.GRAVITY_EARTH;

        // Calculate g-force
        float gForce = (float) Math.sqrt(gX * gX + gY * gY + gZ * gZ);

        if (gForce > SHAKE_THRESHOLD_GRAVITY) {
            final long now = System.currentTimeMillis();

            // Ignore shakes too close to each other
            if (shakeTimestamp + SHAKE_SLOP_TIME_MS > now) {
                return;
            }

            // Reset shake count after 3 seconds of no shakes
            if (shakeTimestamp + SHAKE_COUNT_RESET_TIME_MS < now) {
                shakeCount = 0;
            }

            shakeTimestamp = now;
            shakeCount++;

            if (listener != null) {
                listener.onShake(shakeCount);
            }
        }
    }
}
