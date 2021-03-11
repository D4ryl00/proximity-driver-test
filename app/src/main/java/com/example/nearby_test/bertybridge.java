package com.example.nearby_test;

import android.util.Log;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.Queue;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

// Simulate the java/golang bridge
public class bertybridge {
    private final static String TAG = "bertybridge";

    // return a dummy ProximityTransport
    public static ProximityTransport getProximityTransport(String procotolName) {
        return new ProximityTransport();
    }

    // interface to be implemented by your BLE driver.
    // The proximity transport uses these methods to control the driver.
    // flow way: golang -> java
    public interface NativeNBDriver {
        public void start(String localPID);
        public void stop();
        public boolean dialPeer(String remotePID);
        public boolean sendToPeer(String remotePID, byte[] payload);
        public void closeConnWithPeer(String remotePID);
        public long protocolCode();
        public String protocolName();
        public String defaultAddr();
    }

    // Your driver will call these functions to give information to the proximity transport.
    // flow way: java -> golang
    public static class ProximityTransport {
        // for testing purpose
        private static NBInterface mDriver;
        private static String mPeerID;
        //private static final Queue<String> SentPings = new ConcurrentLinkedQueue<>();
        private static HashMap<String, Queue<String>> mPings = new HashMap<>();
        private static HashMap<String, Timer> mTimers = new HashMap<>();

        // end test purpose

        // will be called when the driver has a newly connection with a new endpoint (never seen or after a deco/reco)
        // After this call, libp2p initiates communications between the two peers (local-remote)
        public static boolean handleFoundPeer(String remotePID) {
            Log.i(TAG, String.format("handleFoundPeer: remotePID=%s", remotePID));

            // Send ping for testing purpose, comment line below to do nothing
            if (mPeerID != null && mDriver != null) {
                Queue<String> pings = new ConcurrentLinkedQueue<>();
                mPings.put(remotePID, pings);

                if (UUID.fromString(mPeerID).compareTo(UUID.fromString(remotePID)) < 0) {
                    // Only the lower PID send messages
                    Timer timer = new Timer();
                    TimerTask myTask = new TimerTask() {
                        @Override
                        public void run() {
                            String data = getAlphaNumericString(16);
                            Log.i(TAG, String.format("handleFoundPeer: send data=%s remotePID=%s", data, remotePID));
                            pings.add(data);
                            mDriver.sendToPeer(remotePID, data.getBytes());                        }
                    };

                    mTimers.put(remotePID, timer);
                    timer.schedule(myTask, 0, 1000);
                }
            }
            // end test ping

            return true;
        }

        // will be called when the driver lost an endpoint
        public static void handleLostPeer(String remotePID) {
            Log.i(TAG, String.format("handleLostPeer: remotePID=%s", remotePID));

            // for testing purpose
            Timer timer;
            if ((timer = mTimers.remove(remotePID)) != null) {
                timer.cancel();
                timer.purge();
            }
            mPings.remove(remotePID);
            // end test purpose
        }

        // will be called when the driver received payload from an endpoint
        public static void receiveFromPeer(String remotePID, byte[] payload) {
            Log.i(TAG, String.format("receiveFromPeer: remotePID=%s", remotePID));

            // for testing purpose
            if (mPeerID != null && mDriver != null) {
                String dataString = new String(payload);
                if (UUID.fromString(mPeerID).compareTo(UUID.fromString(remotePID)) < 0) {
                    // test if it's the good message
                    Queue<String> pings = mPings.get(remotePID);
                    if (pings != null) {
                        String toCompare = pings.remove();
                        if (!dataString.equals(toCompare)) {
                            Log.e(TAG, String.format("Received ping message is not correct: ping=%s toCompare=%s", dataString, toCompare));
                        } else {
                            Log.i(TAG, String.format("Ping message is correct: ping=%s", dataString));
                        }
                    } else {
                        Log.e(TAG, String.format("Received ping message error: queue not found: remotePID=%s",remotePID));
                    }
                } else {
                    // send back the ping to the emitter
                    Log.i(TAG, String.format("Ping message received and send back: ping=%s", dataString));
                    mDriver.sendToPeer(remotePID, payload);
                }
            }
            // end test purpose
        }

        // Bellow methods are only used for testing purpose, it's not in the Berty project

        // Proximity transport needs to have access at the driver, so we give it here.
        public static void setDriver(NBInterface driver, String peerID) {
            mDriver = driver;
            mPeerID = peerID;
        }
    }

    // Bellow method are only used for testing purpose, it's not in the Berty project
    // https://www.geeksforgeeks.org/generate-random-string-of-given-size-in-java/
    public static String getAlphaNumericString(int n)
    {

        // chose a Character random from this String
        String AlphaNumericString = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
                + "0123456789"
                + "abcdefghijklmnopqrstuvxyz";

        // create StringBuffer size of AlphaNumericString
        StringBuilder sb = new StringBuilder(n);

        for (int i = 0; i < n; i++) {

            // generate a random number between
            // 0 to AlphaNumericString variable length
            int index
                    = (int)(AlphaNumericString.length()
                    * Math.random());

            // add Character one by one in end of sb
            sb.append(AlphaNumericString
                    .charAt(index));
        }

        return sb.toString();
    }
}
