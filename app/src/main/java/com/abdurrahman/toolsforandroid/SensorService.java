package com.abdurrahman.toolsforandroid.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
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

import androidx.core.app.NotificationCompat;

import com.abdurrahman.toolsforandroid.R;
import com.abdurrahman.toolsforandroid.utils.ShakeDetector;
import com.abdurrahman.toolsforandroid.utils.SharedPrefs;

public class SensorService extends Service implements SensorEventListener, ShakeDetector.OnShakeListener {

    private static final String TAG = "SensorService";
    private static final String CHANNEL_ID = "SensorServiceChannel";
    private static final int NOTIFICATION_ID = 1;

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
    private boolean isServiceRunning = false;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service onCreate");
        
        prefs = new SharedPrefs(this);
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        shakeDetector = new ShakeDetector(this);
        cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        
        createNotificationChannel();
        initializeCamera();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Sensor Service Channel",
                    NotificationManager.IMPORTANCE_LOW
            );
            serviceChannel.setDescription("Background sensor monitoring for shake and orientation features");
            
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    private Notification createNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Tools For Android")
                .setContentText("Monitoring sensors in background")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .setSilent(true)
                .build();
    }

    private void initializeCamera() {
        try {
            if (cameraManager != null) {
                for (String id : cameraManager.getCameraIdList()) {
                    CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(id);
                    Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                    Boolean flashAvailable = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
                    if (facing != null && facing == CameraCharacteristics.LENS_FACING_BACK && 
                        flashAvailable != null && flashAvailable) {
                        cameraId = id;
                        break;
                    }
                }
            }
        } catch (CameraAccessException e) {
            Log.e(TAG, "Error accessing camera", e);
        } catch (Exception e) {
            Log.e(TAG, "Error initializing camera", e);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Service onStartCommand");
        
        if (!isServiceRunning) {
            startForeground(NOTIFICATION_ID, createNotification());
            startSensorListening();
            isServiceRunning = true;
        }
        
        return START_STICKY;
    }

    private void startSensorListening() {
        if (accelerometer != null && sensorManager != null) {
            try {
                sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
                Log.d(TAG, "Sensor listener registered");
            } catch (Exception e) {
                Log.e(TAG, "Error registering sensor listener", e);
            }
        } else {
            Log.e(TAG, "Accelerometer or SensorManager not available");
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
            Log.d(TAG, "Device face down - Silent mode activated");
        } else if (z > FACE_UP_THRESHOLD && isFaceDown) {
            // Device is face up
            isFaceDown = false;
            setSilentMode(false);
            vibrate();
            Log.d(TAG, "Device face up - Normal mode activated");
        }
    }

    private void setSilentMode(boolean silent) {
        try {
            if (silent) {
                audioManager.setRingerMode(AudioManager.RINGER_MODE_SILENT);
            } else {
                audioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
            }
        } catch (SecurityException e) {
            Log.e(TAG, "Permission denied for changing ringer mode", e);
        } catch (Exception e) {
            Log.e(TAG, "Error setting silent mode", e);
        }
    }

    private void vibrate() {
        if (vibrator != null && vibrator.hasVibrator()) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    vibrator.vibrate(100);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error vibrating", e);
            }
        }
    }

    @Override
    public void onShake(int count) {
        Log.d(TAG, "Shake detected: " + count);
        if (count >= 5) {
            toggleFlashlight();
        }
    }

    private void toggleFlashlight() {
        try {
            if (cameraId != null && cameraManager != null) {
                if (isFlashlightOn) {
                    cameraManager.setTorchMode(cameraId, false);
                    isFlashlightOn = false;
                    Log.d(TAG, "Flashlight turned off");
                } else {
                    cameraManager.setTorchMode(cameraId, true);
                    isFlashlightOn = true;
                    Log.d(TAG, "Flashlight turned on");
                }
                vibrate();
            } else {
                Log.e(TAG, "Camera not available for flashlight");
            }
        } catch (CameraAccessException e) {
            Log.e(TAG, "Error toggling flashlight - Camera Access", e);
        } catch (SecurityException e) {
            Log.e(TAG, "Error toggling flashlight - Permission denied", e);
        } catch (Exception e) {
            Log.e(TAG, "Error toggling flashlight", e);
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
        Log.d(TAG, "Service onDestroy");
        
        // Clean up resources
        if (sensorManager != null) {
            try {
                sensorManager.unregisterListener(this);
            } catch (Exception e) {
                Log.e(TAG, "Error unregistering sensor listener", e);
            }
        }
        
        // Turn off flashlight when service stops
        if (isFlashlightOn && cameraId != null) {
            try {
                cameraManager.setTorchMode(cameraId, false);
                Log.d(TAG, "Flashlight turned off during service destruction");
            } catch (Exception e) {
                Log.e(TAG, "Error turning off flashlight during destruction", e);
            }
        }
        
        isServiceRunning = false;
    }
}
