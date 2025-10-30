package com.abdurrahman.toolsforandroid;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.CompoundButton;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.abdurrahman.toolsforandroid.services.SensorService;
import com.abdurrahman.toolsforandroid.utils.SharedPrefs;
import com.google.android.material.switchmaterial.SwitchMaterial;

public class MainActivity extends AppCompatActivity {

    private SwitchMaterial shakeFlashlightSwitch, autoSilentSwitch;
    private SharedPrefs prefs;
    private SensorManager sensorManager;
    private Sensor accelerometer;

    // Permission request launcher
    private final ActivityResultLauncher<String[]> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), permissions -> {
                Boolean cameraGranted = permissions.get(Manifest.permission.CAMERA);
                Boolean postNotificationsGranted = permissions.getOrDefault(Manifest.permission.POST_NOTIFICATIONS, true);

                if (cameraGranted != null && cameraGranted) {
                    updateSwitchStates();
                    toggleSensorService();
                } else {
                    Toast.makeText(this, "Camera permission is required for flashlight feature", Toast.LENGTH_LONG).show();
                    updateSwitchStates();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = new SharedPrefs(this);
        initializeViews();
        setupClickListeners();
        checkSensors();
    }

    private void initializeViews() {
        shakeFlashlightSwitch = findViewById(R.id.shake_flashlight_switch);
        autoSilentSwitch = findViewById(R.id.auto_silent_switch);

        updateSwitchStates();
    }

    private void updateSwitchStates() {
        shakeFlashlightSwitch.setChecked(prefs.isShakeFlashlightEnabled());
        autoSilentSwitch.setChecked(prefs.isAutoSilentEnabled());
    }

    private void setupClickListeners() {
        shakeFlashlightSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked && !hasRequiredPermissions()) {
                requestPermissions();
                shakeFlashlightSwitch.setChecked(false);
                return;
            }
            prefs.setShakeFlashlightEnabled(isChecked);
            toggleSensorService();
        });

        autoSilentSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.setAutoSilentEnabled(isChecked);
            toggleSensorService();
        });
    }

    private boolean hasRequiredPermissions() {
        boolean hasCameraPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            boolean hasNotificationPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
            return hasCameraPermission && hasNotificationPermission;
        }
        
        return hasCameraPermission;
    }

    private void requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(new String[]{
                    Manifest.permission.CAMERA,
                    Manifest.permission.POST_NOTIFICATIONS
            });
        } else {
            requestPermissionLauncher.launch(new String[]{
                    Manifest.permission.CAMERA
            });
        }
    }

    private void toggleSensorService() {
        Intent serviceIntent = new Intent(this, SensorService.class);
        
        if (prefs.isShakeFlashlightEnabled() || prefs.isAutoSilentEnabled()) {
            if (hasRequiredPermissions()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(serviceIntent);
                } else {
                    startService(serviceIntent);
                }
                Toast.makeText(this, "Background service started", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Permissions required to start service", Toast.LENGTH_SHORT).show();
            }
        } else {
            stopService(serviceIntent);
            Toast.makeText(this, "Background service stopped", Toast.LENGTH_SHORT).show();
        }
    }

    private void checkSensors() {
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        
        if (accelerometer == null) {
            shakeFlashlightSwitch.setEnabled(false);
            autoSilentSwitch.setEnabled(false);
            Toast.makeText(this, "Accelerometer not available on this device", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateSwitchStates();
    }
}
