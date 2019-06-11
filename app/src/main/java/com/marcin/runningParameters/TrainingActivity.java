package com.marcin.runningParameters;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.JointType;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.Locale;

public class TrainingActivity extends FragmentActivity
        implements OnMapReadyCallback,
        GoogleMap.OnPolylineClickListener, SensorEventListener {

    FusedLocationProviderClient mFusedLocationProvider;
    LocationRequest locationRequest;
    Location currentLocation;
    Location lastLocation;
    GoogleMap map;
    boolean firstMapRefresh;

    TextView distanceTV;
    TextView timeTV;
    TextView paceTV;
    TextView cadenceTV;
    TextView stepLengthTV;
    TextView currentStepLengthTV;
    TextView currentPaceTV;
    TextView currentCadenceTV;

    ProgressBar currentPacePB;
    ProgressBar currentStepLengthPB;
    ProgressBar pacePB;
    ProgressBar stepLengthPB;

    float distance = 0;  //meters
    float distanceFromLastRefresh = 0;

    int steps = 0;
    int stepsFromLastRefresh = 0;

    long timeFromStart = 0L;
    long startTime = 0L;

    SensorManager sensorManager;

    Handler handler = new Handler();

    ArrayList<LatLng> mapPoints = new ArrayList<>();

    protected void onResume() {
        super.onResume();

        Sensor stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        if (stepSensor != null) {
            sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_FASTEST);
        } else {
            Toast.makeText(this, "Step Sensor not found!", Toast.LENGTH_SHORT).show();
        }
    }

    Runnable stoper = new Runnable() {
        @Override
        public void run() {
            timeFromStart = SystemClock.elapsedRealtime() - startTime;
            int seconds = (int) (timeFromStart / 1000);
            int minutes = seconds / 60;
            int hours = minutes / 60;
            seconds %= 60;
            minutes %= 60;

            String time = String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds);
            timeTV.setText(time);

            handler.postDelayed(this, 1000);
        }
    };


    private Runnable refreshParameters = new Runnable() {
        @Override
        public void run() {
            int secs = (int) (timeFromStart / 1000);
            float mins = (float) secs / 60;

            if (mins > 0) {
                //pace
                float p = averagePace(distance, timeFromStart);
                if (distance > 0) {
                    String avgPace = convertTimeToPace(p);
                    paceTV.setText(avgPace + " min/km");
                    pacePB.setProgress(360 - averagePaceInSeconds(distance, timeFromStart));

                    p = averagePace(distance - distanceFromLastRefresh, 15000);
                    avgPace = convertTimeToPace(p);
                    currentPaceTV.setText(avgPace + " min/km");
                    currentPacePB.setProgress(360 - averagePaceInSeconds(distance - distanceFromLastRefresh, 15000));
                }

                //cadence
                if (stepsFromLastRefresh == 0) {
                    stepsFromLastRefresh = 40;
                    steps += 40;
                }

                //average cadence
                float averageCadence = steps / mins;
                String color = checkParameters.CadenceColor((int) (steps / mins), averagePaceInSeconds(distance, timeFromStart));
                cadenceTV.setTextColor(Color.parseColor(color));
                String arrow = checkParameters.CadenceArrow((int) (steps / mins), averagePaceInSeconds(distance, timeFromStart));
                cadenceTV.setText(String.valueOf((int) averageCadence) + arrow);

                //current cadence
                color = checkParameters.CadenceColor(stepsFromLastRefresh * 4, averagePaceInSeconds(distance - distanceFromLastRefresh, 15000));
                currentCadenceTV.setTextColor(Color.parseColor(color));
                arrow = checkParameters.CadenceArrow(stepsFromLastRefresh * 4, averagePaceInSeconds(distance - distanceFromLastRefresh, 15000));
                currentCadenceTV.setText(String.valueOf(stepsFromLastRefresh * 4) + arrow);

                //step length
                float stepLength = (distance - distanceFromLastRefresh) / stepsFromLastRefresh * 100;
                currentStepLengthTV.setText(String.valueOf((int) stepLength) + "cm");
                currentStepLengthPB.setProgress((int) stepLength - 90);
                stepLength = distance / steps * 100;
                stepLengthTV.setText(String.valueOf((int) stepLength) + "cm");
                stepLengthPB.setProgress((int) stepLength - 90);

                //resets
                stepsFromLastRefresh = 0;
                distanceFromLastRefresh = distance;
            }
            handler.postDelayed(this, 15000); //15s
        }
    };


    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mFusedLocationProvider = LocationServices.getFusedLocationProviderClient(this);

        setContentView(R.layout.trainingactivity);

        distanceTV = findViewById(R.id.distanceTV);
        timeTV = findViewById(R.id.timeTV);
        cadenceTV = findViewById(R.id.cadenceTV);
        paceTV = findViewById(R.id.paceTV);
        stepLengthTV = findViewById(R.id.stepLengthTV);

        currentStepLengthTV = findViewById(R.id.currentStepLengthTV);
        currentPaceTV = findViewById(R.id.currentPaceTV);
        currentCadenceTV = findViewById(R.id.currentCadenceTV);

        currentStepLengthPB = findViewById(R.id.currentStepLengthPB);
        stepLengthPB = findViewById(R.id.stepLengthPB);
        currentPacePB = findViewById(R.id.currentPacePB);
        pacePB = findViewById(R.id.pacePB);


        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);


        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(4 * 1000);
        locationRequest.setFastestInterval(2 * 1000);
        mFusedLocationProvider.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());

        startTime = SystemClock.elapsedRealtime();
        refreshParameters.run();
        stoper.run();

        Button stopBtn = findViewById(R.id.stopBtn);
        stopBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(getApplicationContext(), "Hold button to stop the training!", Toast.LENGTH_SHORT).show();
            }
        });

        stopBtn.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                mFusedLocationProvider.removeLocationUpdates(locationCallback);
                handler.removeCallbacks(stoper);
                handler.removeCallbacks(refreshParameters);
                Intent k = new Intent(getApplicationContext(), StartActivity.class);
                startActivity(k);
                return true;
            }
        });
    }

    private LocationCallback locationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            super.onLocationResult(locationResult);
            Location location = locationResult.getLastLocation();
            if (location == null)
                return;
            refreshRoute(location);
            currentLocation = location;
            if (lastLocation == null) lastLocation = location;
            distance += lastLocation.distanceTo(currentLocation);
            lastLocation = currentLocation;

            String distance = String.format("%.2f", TrainingActivity.this.distance / 1000);
            distanceTV.setText(distance + "km");

            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

            if (firstMapRefresh == false) {
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
                firstMapRefresh = true;
            } else
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
        }
    };

    private void refreshRoute(Location currentLocation) {
        mapPoints.add(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()));
        PolylineOptions lineSettings = new PolylineOptions()
                .clickable(false)
                .color(Color.rgb(255, 64, 129)) //purple
                .width(8)
                .jointType(JointType.BEVEL);
        lineSettings.addAll(mapPoints);
        map.addPolyline(lineSettings);
    }

    @Override
    public void onMapReady(GoogleMap map) {
        this.map = map;
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        this.map.setMyLocationEnabled(true);
        this.map.getUiSettings().setMyLocationButtonEnabled(true);
    }

    public void onSensorChanged(SensorEvent event) {
        steps++;
        stepsFromLastRefresh++;
    }

    private float averagePace(float distanceMeters, long timeMS) {
        int seconds = (int) (timeMS / 1000);
        float minutes = (float) seconds / 60;
        float km = distanceMeters / 1000;
        return minutes / km;
    }

    private int averagePaceInSeconds(float distanceMeters, long timeMS) {
        int seconds = (int) (timeMS / 1000);
        float km = distanceMeters / 1000;
        return (int) (seconds / km);
    }

    //time to pace format (5.5 = 5:30min/km)
    private String convertTimeToPace(float time) {
        int minutes = (int) Math.floor(time);
        double dif = time - minutes;
        int seconds = (int) Math.round(dif * 60);
        return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds);
    }

    @Override
    public void onBackPressed() {
        Toast.makeText(this, "Button locked!", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onPolylineClick(Polyline polyline) {
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }


}
