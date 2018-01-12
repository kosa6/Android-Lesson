package com.sensors;

import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;


public class MainActivity extends AppCompatActivity implements SensorEventListener {

    //Sensor
    private SensorManager mSensorManager;
    private TextView stepCountWindow, direction, acceleration;
    private boolean activityIsRunning;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activityIsRunning = true;
        setContentView(R.layout.activity_main);
        initFront();
        initSensorManager();
        initAllSensor();
    }

    private void initFront() {
        stepCountWindow = findViewById(R.id.stepCount);
        direction = findViewById(R.id.way);
        acceleration = findViewById(R.id.Acceleration);
    }

    private void initSensorManager() {
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
    }

    private void initAllSensor() {
        initStepCounter();
        initLight();
        initOrientation();
        initAcceleration();
    }

    private void initStepCounter() {
        if (mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) != null) {
            Sensor stepCount = mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
            mSensorManager.registerListener(stepCountSensorListener, stepCount, mSensorManager.SENSOR_DELAY_UI);
        } else {
            stepCountWindow.setText("You don't have step counter on your device");
        }
    }

    private void initOrientation() {
        if (mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION) != null && checkIfFlashLightIsSupported()) {
            Sensor mDirection = mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
            mSensorManager.registerListener(directionShow, mDirection, mSensorManager.SENSOR_DELAY_UI);
        } else {
            Toast tost = Toast.makeText(getApplicationContext(), "You don't have orientation sensor", Toast.LENGTH_SHORT);
            tost.show();
        }
    }


    private void initLight() {
        if (mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT) != null && checkIfFlashLightIsSupported()) {
            Sensor mLight = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
            mSensorManager.registerListener(lightSensorListener, mLight, mSensorManager.SENSOR_DELAY_UI);
        } else {
            Toast tost = Toast.makeText(getApplicationContext(), "You don't have light sensor", Toast.LENGTH_SHORT);
            tost.show();
        }
    }

    private void initAcceleration() {
        if (mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null) {
            Sensor mAcceleration = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            mSensorManager.registerListener(this, mAcceleration, mSensorManager.SENSOR_DELAY_UI);
        } else {
            Toast tost = Toast.makeText(getApplicationContext(), "You don't have acceleration sensor", Toast.LENGTH_SHORT);
            tost.show();
        }
    }

    private boolean checkIfFlashLightIsSupported() {
        if (!getApplicationContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {
            Toast tost = Toast.makeText(getApplicationContext(), "You phone doesn't support flashlight'", Toast.LENGTH_SHORT);
            tost.show();
            return false;
        }
        return true;
    }

    private void flashLightOn() {
        if (activityIsRunning) {
            CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
            try {
                String cameraId = cameraManager.getCameraIdList()[0];
                cameraManager.setTorchMode(cameraId, true);
            } catch (CameraAccessException e) {
            }
        }
    }

    private void flashLightOff() {
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            String cameraId = cameraManager.getCameraIdList()[0];
            cameraManager.setTorchMode(cameraId, false);
        } catch (CameraAccessException e) {
        }
    }

    SensorEventListener lightSensorListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.values[0] == 0) {
                flashLightOn();
            } else {
                flashLightOff();
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };

    SensorEventListener stepCountSensorListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            stepCountWindow.setText("Step count: " + String.valueOf(event.values[0]));
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };

    SensorEventListener directionShow = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            int degree = Math.round(event.values[0]);
            direction.setText(String.valueOf("Degree: " + degree));
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        final double alpha = 0.8;

        double gravity[] = new double[3];
        double linear_acceleration[] = new double[3];

        gravity[0] = alpha * gravity[0] + (1 - alpha) * sensorEvent.values[0];
        gravity[1] = alpha * gravity[1] + (1 - alpha) * sensorEvent.values[1];
        gravity[2] = alpha * gravity[2] + (1 - alpha) * sensorEvent.values[2];

        linear_acceleration[0] = sensorEvent.values[0] - gravity[0];
        linear_acceleration[1] = sensorEvent.values[1] - gravity[1];
        linear_acceleration[2] = sensorEvent.values[2] - gravity[2];

        acceleration.setText("Wait");

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    @Override
    protected void onPause() {
        activityIsRunning = false;
        super.onPause();
    }

    @Override
    protected void onStop() {
        activityIsRunning = false;
        flashLightOff();
        super.onStop();
    }

}
