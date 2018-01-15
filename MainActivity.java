package com.sensors;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.LocationSource;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnCompleteListener;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private static final String TAG = "MainActivity";
    //Sensor
    private SensorManager mSensorManager;
    private TextView stepCountWindow, direction, acceleration;
    private boolean activityIsRunning;

    //Map
    private boolean mLocationPermissionGranted = false;
    private GoogleMap gMap;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1210;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activityIsRunning = true;
        setContentView(R.layout.activity_main);
        initFront();
        initSensorManager();
        initAllSensor();
        if (isGoogleServicesOk()) {
            getLocationPermission();
            checkIfCanInitMap();
        }
    }

    private void initMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {
                gMap = googleMap;
                getDeviceLocation();
                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }
                gMap.setMyLocationEnabled(true);
                gMap.setLocationSource(new CurrentLocationProvider(MainActivity.this));
            }
        });
    }

    private void checkIfCanInitMap() {
        if (mLocationPermissionGranted) {
            initMap();
        }
    }

    private void getDeviceLocation() {
        FusedLocationProviderClient fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        try {
            if (mLocationPermissionGranted) {
                final com.google.android.gms.tasks.Task location = fusedLocationProviderClient.getLastLocation();
                location.addOnCompleteListener(new OnCompleteListener() {
                    @Override
                    public void onComplete(@NonNull com.google.android.gms.tasks.Task task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "Found location");
                            Location currLocation = (Location) task.getResult();
                            LatLng latLng = new LatLng(currLocation.getLatitude(), currLocation.getLongitude());
                            gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f));
                        } else {
                            Log.d(TAG, "Not found current location");
                            Toast.makeText(MainActivity.this, "Unable to find current location", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        } catch (SecurityException e) {
            Log.e(TAG, "Security Exception " + e.getMessage());
        }
    }

    private void getLocationPermission() {
        String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            if (ContextCompat.checkSelfPermission(this.getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                mLocationPermissionGranted = true;
            } else {
                ActivityCompat.requestPermissions(this, permissions, LOCATION_PERMISSION_REQUEST_CODE);
            }
        } else {
            ActivityCompat.requestPermissions(this, permissions, LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    private boolean isGoogleServicesOk() {
        int available = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(MainActivity.this);
        if (available == ConnectionResult.SUCCESS) {
            Log.d(TAG, "Google Services are working");
            return true;
        } else if (GoogleApiAvailability.getInstance().isUserResolvableError(available)) {
            Log.d(TAG, "Ther is a problem, but you can resolve it");
            Dialog dialog = GoogleApiAvailability.getInstance().getErrorDialog(MainActivity.this, available, 9001);
            dialog.show();
        } else {
            Toast.makeText(this, "Map requests fail", Toast.LENGTH_SHORT).show();
        }
        return false;
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

    public class CurrentLocationProvider implements LocationSource, LocationListener {
        private OnLocationChangedListener listener;
        private LocationManager locationManager;
        private ArrayList<LatLng> points;
        Polyline line;

        public CurrentLocationProvider(Context context) {
            points = new ArrayList<>();
            locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        }

        @Override
        public void activate(OnLocationChangedListener listener) {
            this.listener = listener;
            LocationProvider gpsProvider = locationManager.getProvider(LocationManager.GPS_PROVIDER);
            if (gpsProvider != null) {
                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }
                locationManager.requestLocationUpdates(gpsProvider.getName(), 0, 10, this);
            }

            LocationProvider networkProvider = locationManager.getProvider(LocationManager.NETWORK_PROVIDER);
            if(networkProvider != null) {
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000 * 6, 10, this);
            }
        }
        private void redrawLine(){

            gMap.clear();

            PolylineOptions options = new PolylineOptions().width(5).color(Color.BLUE).geodesic(true);
            for (int i = 0; i < points.size(); i++) {
                LatLng point = points.get(i);
                options.add(point);
            }
            line = gMap.addPolyline(options);
        }
        @Override
        public void deactivate()
        {
            locationManager.removeUpdates(this);
        }

        @Override
        public void onLocationChanged(Location location)
        {
            double latitude = location.getLatitude();
            double longitude = location.getLongitude();
            LatLng latLng = new LatLng(latitude, longitude);
            points.add(latLng);
            redrawLine();
            if(listener != null)
            {
                listener.onLocationChanged(location);
            }
        }

        @Override
        public void onProviderDisabled(String provider)
        {
            // TODO Auto-generated method stub

        }

        @Override
        public void onProviderEnabled(String provider)
        {
            // TODO Auto-generated method stub

        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras)
        {
            // TODO Auto-generated method stub

        }
    }
}
