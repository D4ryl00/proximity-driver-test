package com.example.nearby_test;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.example.BertyBridge.BertyBridge;

import java.util.ArrayList;

import static com.example.nearby_test.NearbyBertyNativeDriver.ACTION_CONNECTED_ENDPOINT;
import static com.example.nearby_test.NearbyBertyNativeDriver.ACTION_DISCONNECTED_ENDPOINT;
import static com.example.nearby_test.NearbyBertyNativeDriver.ACTION_FOUND_ENDPOINT;
import static com.example.nearby_test.NearbyBertyNativeDriver.ACTION_LOST_ENDPOINT;
import static com.example.nearby_test.NearbyBertyNativeDriver.ACTION_RECEIVED_MESSAGE;
import static com.example.nearby_test.NearbyBertyNativeDriver.ACTION_SENT_MESSAGE;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "BertyNativeDriverUI";

    private RecyclerView recyclerView;
    private CustomAdapter customAdapter;
    private ArrayList<EndpointDataView> endpointDataViews = new ArrayList<>();

    private Handler mainHandler;
    private NearbyBertyNativeDriver nearby;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        recyclerView = findViewById(R.id.endPointListView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));

        customAdapter = new CustomAdapter(endpointDataViews);
        recyclerView.setAdapter(customAdapter);

        requestPermissions(new String[] {
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.CHANGE_WIFI_STATE,
                Manifest.permission.ACCESS_FINE_LOCATION,
        }, 1);

        mainHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                super.handleMessage(msg);
                Log.v(TAG, "handleMessage called");

                EndpointDataView endpointDataView = (EndpointDataView) msg.obj;
                switch (endpointDataView.getAction()) {
                    case ACTION_FOUND_ENDPOINT:
                        Log.i(TAG, String.format("ACTION_FOUND_ENDPOINT: userName=%s userId=%s", endpointDataView.getName(), endpointDataView.getId()));

                        handleFoundEndpoint(endpointDataView);
                        break ;
                    case ACTION_LOST_ENDPOINT:
                        Log.i(TAG, String.format("ACTION_LOST_ENDPOINT: userId=%s", endpointDataView.getId()));

                        handleLostEndpoint(endpointDataView);
                        break ;
                    case ACTION_CONNECTED_ENDPOINT:
                        Log.i(TAG, String.format("ACTION_CONNECTED_ENDPOINT: userId=%s", endpointDataView.getId()));

                        handleConnectedEndpoint(endpointDataView);
                        break ;
                    case ACTION_DISCONNECTED_ENDPOINT:
                        Log.i(TAG, String.format("ACTION_DISCONNECTED_ENDPOINT: userId=%s", endpointDataView.getId()));

                        handleDisconnectedEndpoint(endpointDataView);
                        break ;
                    case ACTION_SENT_MESSAGE:
                        Log.i(TAG, String.format("ACTION_SENT_MESSAGE: userId=%s", endpointDataView.getId()));

                        handleSentMessage(endpointDataView);
                        break ;
                    case ACTION_RECEIVED_MESSAGE:
                        Log.i(TAG, String.format("ACTION_RECEIVED_MESSAGE: userId=%s", endpointDataView.getId()));

                        handleReceivedMessage(endpointDataView);
                        break ;
                    default:
                        Log.e(TAG, "Unknown action message");
                }
            }
        };

        // init driver and bridge
        nearby = new NearbyBertyNativeDriver(getApplicationContext(), mainHandler);
        BertyBridge.ProximityTransport.initDriver(nearby);
    }

    private void handleFoundEndpoint(EndpointDataView endpointDataView) {
        endpointDataViews.add(endpointDataView);
        customAdapter.notifyItemInserted(endpointDataViews.indexOf(endpointDataView));
    }

    private void handleLostEndpoint(EndpointDataView endpointDataView) {
        EndpointDataView addedEndpoint;
        if ((addedEndpoint = getItemById(endpointDataView.getId())) != null) {
            addedEndpoint.setAction(ACTION_LOST_ENDPOINT);

            customAdapter.notifyItemChanged(endpointDataViews.indexOf(addedEndpoint));
        }
    }

    private void handleConnectedEndpoint(EndpointDataView endpointDataView) {
        EndpointDataView addedEndpoint;
        if ((addedEndpoint = getItemById(endpointDataView.getId())) != null) {
            addedEndpoint.setAction(ACTION_CONNECTED_ENDPOINT);

            customAdapter.notifyItemChanged(endpointDataViews.indexOf(addedEndpoint));
        }
    }

    private void handleDisconnectedEndpoint(EndpointDataView endpointDataView) {
        EndpointDataView addedEndpoint;
        if ((addedEndpoint = getItemById(endpointDataView.getId())) != null) {
            addedEndpoint.setAction(ACTION_DISCONNECTED_ENDPOINT);

            customAdapter.notifyItemChanged(endpointDataViews.indexOf(addedEndpoint));
        }
    }

    private void handleSentMessage(EndpointDataView endpointDataView) {
        EndpointDataView addedEndpoint;
        if ((addedEndpoint = getItemById(endpointDataView.getId())) != null) {
            addedEndpoint.incrementSentMessages();

            customAdapter.notifyItemChanged(endpointDataViews.indexOf(addedEndpoint));
        }
    }

    private void handleReceivedMessage(EndpointDataView endpointDataView) {
        EndpointDataView addedEndpoint;
        if ((addedEndpoint = getItemById(endpointDataView.getId())) != null) {
            addedEndpoint.incrementReceivedMessages();

            customAdapter.notifyItemChanged(endpointDataViews.indexOf(addedEndpoint));
        }
    }

    private EndpointDataView getItemById(String id) {
        for (EndpointDataView endpoint: endpointDataViews) {
            if (endpoint.getId().equals(id)) {
                return endpoint;
            }
        }
        return null;
    }

    private void removeItem(EndpointDataView endpoint) {
        int index = endpointDataViews.indexOf(endpoint);
        endpointDataViews.remove(index);
        customAdapter.notifyItemRemoved(index);
    }
}