package com.abdurrahman.toolsforandroid;

import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.widget.CompoundButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import com.abdurrahman.toolsforandroid.services.SensorService;
import com.abdurrahman.toolsforandroid.utils.SharedPrefs;

public class MainActivity extends AppCompatActivity {

    private SwitchCompat shakeFlashlightSwitch, autoSilentSwitch;
    private SharedPrefs prefs;
    private SensorManager sensorManager;
    private Sensor accelerometer;

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
        // Use findViewById instead of view binding
        shakeFlashlightSwitch = findViewById(R.id.shake_flashlight_switch);
        autoSilentSwitch = findViewById(R.id.auto_silent_switch);

        // Set current states
        updateSwitchStates();
    }

    private void updateSwitchStates() {
        shakeFlashlightSwitch.setChecked(prefs.isShakeFlashlightEnabled());
        autoSilentSwitch.setChecked(prefs.isAutoSilentEnabled());
    }

    private void setupClickListeners() {
        shakeFlashlightSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.setShakeFlashlightEnabled(isChecked);
            toggleSensorService();
        });

        autoSilentSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.setAutoSilentEnabled(isChecked);
            toggleSensorService();
        });
    }

    private void toggleSensorService() {
        Intent serviceIntent = new Intent(this, SensorService.class);
        
        if (prefs.isShakeFlashlightEnabled() || prefs.isAutoSilentEnabled()) {
            startService(serviceIntent);
            Toast.makeText(this, "Background service started", Toast.LENGTH_SHORT).show();
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
            Toast.makeText(this, "Accelerometer not available", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateSwitchStates();
    }
}
