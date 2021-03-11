package com.example.nearby_test;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.os.Bundle;

import java.nio.charset.Charset;
import java.util.Random;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    NBInterface nearby;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        requestPermissions(new String[] {
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.CHANGE_WIFI_STATE,
                Manifest.permission.ACCESS_FINE_LOCATION,
        }, 1);

        // init driver and bridge
        nearby = new NBInterface(getApplicationContext());
        String localPeerID = UUID.randomUUID().toString();
        bertybridge.ProximityTransport.setDriver(nearby, localPeerID);
        nearby.start(localPeerID);
    }
}