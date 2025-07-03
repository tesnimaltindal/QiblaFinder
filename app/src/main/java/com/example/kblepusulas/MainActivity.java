package com.example.kblepusulas;


import android.Manifest;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;

import android.os.Bundle;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;

import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.lang.Math;

public class MainActivity extends AppCompatActivity implements SensorEventListener, LocationListener {

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Sensor magnetometer;

    private float[] gravity;
    private float[] geomagnetic;
    private float azimuth = 0f;
    private float currentAzimuth = 0f;

    private ImageView compassArrow;
    private LocationManager locationManager;
    private double userLatitude;
    private double userLongitude;

    private final double KAABA_LATITUDE = 21.4225;
    private final double KAABA_LONGITUDE = 39.8262;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        compassArrow = findViewById(R.id.compass_arrow);

        sensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        locationManager = (LocationManager)getSystemService(LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, this);
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
            gravity = event.values;
        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
            geomagnetic = event.values;

        if (gravity != null && geomagnetic != null) {
            float[] R = new float[9];
            float[] I = new float[9];
            boolean success = SensorManager.getRotationMatrix(R, I, gravity, geomagnetic);
            if (success) {
                float[] orientation = new float[3];
                SensorManager.getOrientation(R, orientation);
                azimuth = (float) Math.toDegrees(orientation[0]);
                azimuth = (azimuth + 360) % 360;

                float bearing = getBearing(userLatitude, userLongitude, KAABA_LATITUDE, KAABA_LONGITUDE);
                float direction = (bearing - azimuth + 360) % 360;

                animateCompass(direction);
            }
        }
    }

    private void animateCompass(float direction) {
        RotateAnimation ra = new RotateAnimation(
                currentAzimuth,
                -direction,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF,
                0.5f);

        ra.setDuration(500);
        ra.setFillAfter(true);

        compassArrow.startAnimation(ra);
        currentAzimuth = -direction;
    }

    private float getBearing(double startLat, double startLng, double endLat, double endLng) {
        double dLon = Math.toRadians(endLng - startLng);
        startLat = Math.toRadians(startLat);
        endLat = Math.toRadians(endLat);

        double y = Math.sin(dLon) * Math.cos(endLat);
        double x = Math.cos(startLat) * Math.sin(endLat) -
                Math.sin(startLat) * Math.cos(endLat) * Math.cos(dLon);
        double brng = Math.toDegrees(Math.atan2(y, x));
        return (float)((brng + 360) % 360);
    }

    @Override
    public void onLocationChanged(Location location) {
        userLatitude = location.getLatitude();
        userLongitude = location.getLongitude();
    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
        sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {}
}
