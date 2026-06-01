package com.example.localisationsmartphone;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements LocationListener {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;

    /*
     * IMPORTANT :
     * - Si tu testes avec l'émulateur Android Studio + XAMPP sur ton PC :
     *   utilise http://10.0.2.2/localisation/createPosition.php
     *
     * - Si tu testes avec un vrai téléphone :
     *   remplace 10.0.2.2 par l'adresse IP de ton PC.
     *   Exemple : http://192.168.1.10/localisation/createPosition.php
     */
    private static final String SERVER_URL = "http://127.0.0.1:8080/localisation/createPosition.php";

    private TextView tvLatitude;
    private TextView tvLongitude;
    private TextView tvDate;
    private TextView tvImei;
    private TextView tvStatus;
    private Button btnStartLocation;
    private Button btnSend;

    private LocationManager locationManager;
    private RequestQueue requestQueue;

    private double currentLatitude = 0.0;
    private double currentLongitude = 0.0;
    private String currentDate = "";
    private String imei = "";

    private boolean hasPosition = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvLatitude = findViewById(R.id.tvLatitude);
        tvLongitude = findViewById(R.id.tvLongitude);
        tvDate = findViewById(R.id.tvDate);
        tvImei = findViewById(R.id.tvImei);
        tvStatus = findViewById(R.id.tvStatus);
        btnStartLocation = findViewById(R.id.btnStartLocation);
        btnSend = findViewById(R.id.btnSend);

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        requestQueue = Volley.newRequestQueue(this);

        imei = getPhoneIdentifier();
        tvImei.setText(imei);

        btnStartLocation.setOnClickListener(v -> checkPermissionAndStartLocation());

        btnSend.setOnClickListener(v -> {
            if (hasPosition) {
                sendPositionToServer();
            } else {
                Toast.makeText(this, getString(R.string.status_no_position), Toast.LENGTH_SHORT).show();
                tvStatus.setText(getString(R.string.status_no_position));
            }
        });
    }

    private void checkPermissionAndStartLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                    this,
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    },
                    LOCATION_PERMISSION_REQUEST_CODE
            );

        } else {
            startLocationUpdates();
        }
    }

    private void startLocationUpdates() {
        try {
            tvStatus.setText(getString(R.string.status_location_started));

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                return;
            }

            boolean gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            boolean networkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

            if (gpsEnabled) {
                locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        5000,
                        5,
                        this
                );
            }

            if (networkEnabled) {
                locationManager.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER,
                        5000,
                        5,
                        this
                );
            }

            Location lastKnownLocation = null;

            if (gpsEnabled) {
                lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            }

            if (lastKnownLocation == null && networkEnabled) {
                lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            }

            if (lastKnownLocation != null) {
                updateLocationData(lastKnownLocation);
            } else {
                tvStatus.setText("Localisation démarrée. Déplacez-vous ou attendez quelques secondes.");
            }

        } catch (Exception e) {
            tvStatus.setText("Erreur localisation : " + e.getMessage());
        }
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        updateLocationData(location);
    }

    private void updateLocationData(Location location) {
        currentLatitude = location.getLatitude();
        currentLongitude = location.getLongitude();
        currentDate = getCurrentDateTime();
        hasPosition = true;

        tvLatitude.setText(String.valueOf(currentLatitude));
        tvLongitude.setText(String.valueOf(currentLongitude));
        tvDate.setText(currentDate);
        tvImei.setText(imei);
        tvStatus.setText(getString(R.string.status_location_updated));
    }

    private String getCurrentDateTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date());
    }

    private String getPhoneIdentifier() {
        /*
         * On garde le nom "imei" pour respecter le TP et la table MySQL.
         * Mais sur Android récent, l'accès au vrai IMEI est limité.
         * Donc on utilise ANDROID_ID comme identifiant du téléphone.
         */
        return Settings.Secure.getString(
                getContentResolver(),
                Settings.Secure.ANDROID_ID
        );
    }

    private void sendPositionToServer() {
        StringRequest request = new StringRequest(
                Request.Method.POST,
                SERVER_URL,
                response -> {
                    tvStatus.setText(getString(R.string.status_send_success) + "\n" + response);
                    Toast.makeText(MainActivity.this, response, Toast.LENGTH_LONG).show();
                },
                error -> {
                    String message = getString(R.string.status_send_error);

                    if (error.networkResponse != null) {
                        int statusCode = error.networkResponse.statusCode;
                        String responseBody = "";

                        if (error.networkResponse.data != null) {
                            responseBody = new String(error.networkResponse.data);
                        }

                        message += "\nCode HTTP : " + statusCode;
                        message += "\nRéponse serveur : " + responseBody;

                    } else if (error.getCause() != null) {
                        message += "\nCause : " + error.getCause().toString();

                    } else if (error.getMessage() != null) {
                        message += "\nMessage : " + error.getMessage();

                    } else {
                        message += "\nAucune réponse du serveur.";
                    }

                    tvStatus.setText(message);
                    Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
                }
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();

                params.put("latitude", String.valueOf(currentLatitude));
                params.put("longitude", String.valueOf(currentLongitude));
                params.put("date_position", currentDate);
                params.put("imei", imei);

                return params;
            }
        };

        requestQueue.add(request);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (locationManager != null) {
            locationManager.removeUpdates(this);
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationUpdates();
            } else {
                tvStatus.setText(getString(R.string.status_permission_denied));
                Toast.makeText(this, getString(R.string.status_permission_denied), Toast.LENGTH_SHORT).show();
            }
        }
    }
}