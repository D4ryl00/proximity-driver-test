package tech.berty.bertybridge;

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import tech.berty.bertysdk.util.BertyLogger;

// Simulate the java/golang bridge
public class BertyBridge {
    private final static String TAG = "bty.BertyBridge";

    private static String localPeerId = UUID.randomUUID().toString();

    // return a dummy ProximityTransport
    public static ProximityTransport getProximityTransport(String procotolName) {
        return new ProximityTransport();
    }

    // Your driver will call these methods to give information to the proximity transport.
    // flow way: java -> golang
    public static class ProximityTransport {
        // for testing purpose
        private static BertyNativeDriver mDriver;
        //private static final Queue<String> SentPings = new ConcurrentLinkedQueue<>();
        private static Map<String, Queue<String>> mPings = new ConcurrentHashMap<>();
        private static Map<String, Timer> mTimers = new ConcurrentHashMap<>();

        // end test purpose

        // will be called when the driver has a newly connection with a new endpoint (never seen or after a deco/reco)
        // After this call, libp2p initiates communications between the two peers (local-remote)
        public static boolean handleFoundPeer(String remotePID) {
            BertyLogger.i(TAG, String.format("handleFoundPeer: remotePID=%s", remotePID));

            // Send ping for testing purpose, comment line below to do nothing
            if (localPeerId != null && mDriver != null) {
                Queue<String> pings = new ConcurrentLinkedQueue<>();
                mPings.put(remotePID, pings);

                try {
                    if (UUID.fromString(localPeerId).compareTo(UUID.fromString(remotePID)) < 0) {
                        // Only the lower PID send messages
                        Timer timer = new Timer();
                        TimerTask myTask = new TimerTask() {
                            @Override
                            public void run() {
                                String data = getAlphaNumericString(16);
                                BertyLogger.i(TAG, String.format("handleFoundPeer: send data=%s remotePID=%s", data, remotePID));
                                pings.add(data);
                                mDriver.sendToPeer(remotePID, data.getBytes());
                            }
                        };

                        mTimers.put(remotePID, timer);
                        timer.schedule(myTask, 0, 1000);
                    } else {
                        BertyLogger.i(TAG, "handleFoundPeer: waiting for receiving ping");
                    }
                } catch(Exception e) {
                    BertyLogger.e(TAG, "handleFoundPeer error", e);
                }
            } else {
                BertyLogger.e(TAG, "handleFoundPeer error: driver or localPID not set");
            }
            // end test ping

            return true;
        }

        // will be called when the driver lost an endpoint
        public static void handleLostPeer(String remotePID) {
            BertyLogger.i(TAG, String.format("handleLostPeer: remotePID=%s", remotePID));

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
            BertyLogger.i(TAG, String.format("receiveFromPeer: remotePID=%s", remotePID));

            // for testing purpose
            if (localPeerId != null && mDriver != null) {
                String dataString = new String(payload);
                if (UUID.fromString(localPeerId).compareTo(UUID.fromString(remotePID)) < 0) {
                    // test if it's the good message
                    Queue<String> pings = mPings.get(remotePID);
                    if (pings != null) {
                        String toCompare = pings.remove();
                        if (!dataString.equals(toCompare)) {
                            BertyLogger.e(TAG, String.format("Received ping message is not correct: ping=%s toCompare=%s", dataString, toCompare));
                        } else {
                            BertyLogger.i(TAG, String.format("Ping message is correct: ping=%s", dataString));
                        }
                    } else {
                        BertyLogger.e(TAG, String.format("Received ping message error: queue not found: remotePID=%s",remotePID));
                    }
                } else {
                    // send back the ping to the emitter
                    BertyLogger.i(TAG, String.format("Ping message received and send back: ping=%s", dataString));
                    mDriver.sendToPeer(remotePID, payload);
                }
            }
            // end test purpose
        }

        // Proximity transport needs to have access to the driver, so we give it here.
        // Testing purpose, it's not in the Berty project
        public static void initDriver(BertyNativeDriver driver) {
            mDriver = driver;

            driver.start(localPeerId);
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
