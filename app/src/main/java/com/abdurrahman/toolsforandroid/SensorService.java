package com.abdurrahman.toolsforandroid.services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.media.AudioManager;
import android.os.Build;
import android.os.IBinder;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;

import com.abdurrahman.toolsforandroid.utils.ShakeDetector;
import com.abdurrahman.toolsforandroid.utils.SharedPrefs;

public class SensorService extends Service implements SensorEventListener, ShakeDetector.OnShakeListener {

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private ShakeDetector shakeDetector;
    private SharedPrefs prefs;
    private CameraManager cameraManager;
    private String cameraId;
    private boolean isFlashlightOn = false;
    private AudioManager audioManager;
    private Vibrator vibrator;

    private static final float FACE_DOWN_THRESHOLD = -9.0f;
    private static final float FACE_UP_THRESHOLD = 9.0f;
    private boolean isFaceDown = false;

    @Override
    public void onCreate() {
        super.onCreate();
        prefs = new SharedPrefs(this);
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        shakeDetector = new ShakeDetector(this);
        cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        
        initializeCamera();
    }

    private void initializeCamera() {
        try {
            if (cameraManager != null) {
                for (String id : cameraManager.getCameraIdList()) {
                    CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(id);
                    Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                    if (facing != null && facing == CameraCharacteristics.LENS_FACING_BACK) {
                        cameraId = id;
                        break;
                    }
                }
            }
        } catch (CameraAccessException e) {
            Log.e("SensorService", "Error accessing camera", e);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startSensorListening();
        return START_STICKY;
    }

    private void startSensorListening() {
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            // Shake detection for flashlight
            if (prefs.isShakeFlashlightEnabled()) {
                shakeDetector.onSensorChanged(event);
            }

            // Face down detection for silent mode
            if (prefs.isAutoSilentEnabled()) {
                handleOrientationDetection(z);
            }
        }
    }

    private void handleOrientationDetection(float z) {
        if (z < FACE_DOWN_THRESHOLD && !isFaceDown) {
            // Device is face down
            isFaceDown = true;
            setSilentMode(true);
            vibrate();
        } else if (z > FACE_UP_THRESHOLD && isFaceDown) {
            // Device is face up
            isFaceDown = false;
            setSilentMode(false);
            vibrate();
        }
    }

    private void setSilentMode(boolean silent) {
        if (silent) {
            audioManager.setRingerMode(AudioManager.RINGER_MODE_SILENT);
        } else {
            audioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
        }
    }

    private void vibrate() {
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(100);
            }
        }
    }

    @Override
    public void onShake(int count) {
        if (count >= 5) {
            toggleFlashlight();
        }
    }

    private void toggleFlashlight() {
        try {
            if (cameraId != null) {
                if (isFlashlightOn) {
                    cameraManager.setTorchMode(cameraId, false);
                    isFlashlightOn = false;
                } else {
                    cameraManager.setTorchMode(cameraId, true);
                    isFlashlightOn = true;
                }
                vibrate();
            }
        } catch (CameraAccessException e) {
            Log.e("SensorService", "Error toggling flashlight", e);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Not used
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
        // Turn off flashlight when service stops
        if (isFlashlightOn && cameraId != null) {
            try {
                cameraManager.setTorchMode(cameraId, false);
            } catch (CameraAccessException e) {
                Log.e("SensorService", "Error turning off flashlight", e);
            }
        }
    }
}
