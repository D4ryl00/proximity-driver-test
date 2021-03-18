package tech.berty.bertybridgedemo;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import tech.berty.bertybridge.BertyBridge;
import tech.berty.bertybridge.BertyNativeDriver;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import tech.berty.bertysdk.base.BertyDriver;
import tech.berty.bertysdk.lifecycle.UserConnectionCallback;
import tech.berty.bertysdk.lifecycle.UserMessageCallback;
import tech.berty.bertysdk.lifecycle.UserSearchCallback;

public class NearbyBertyNativeDriver implements BertyNativeDriver {
    private static final String TAG = "bty.demo.NativeDriver";

    private static final String SERVICE_ID = "tech.berty.bty.nearby";

    public static final String ACTION_FOUND_ENDPOINT = "ACTION_FOUND_ENDPOINT";
    public static final String ACTION_LOST_ENDPOINT = "ACTION_LOST_ENDPOINT";
    public static final String ACTION_CONNECTED_ENDPOINT = "ACTION_CONNECTED_ENDPOINT";
    public static final String ACTION_DISCONNECTED_ENDPOINT = "ACTION_DISCONNECTED_ENDPOINT";
    public static final String ACTION_SENT_MESSAGE = "ACTION_SENT_MESSAGE";
    public static final String ACTION_RECEIVED_MESSAGE = "ACTION_RECEIVED_MESSAGE";

    public static final String DefaultAddr = "/nearby/Qmeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee";
    public static final int ProtocolCode = 0x0044;
    public static final String ProtocolName = "nearby";

    private BertyDriver nearby;;
    private Context mContext;
    private Handler mainHandler;
    private String localPID;

    private Map<String, Endpoint> foundMap = new ConcurrentHashMap<>();
    private Map<String, Endpoint> connectedMap = new ConcurrentHashMap<>();

    UserConnectionCallback userConnectionCallback = new UserConnectionCallback() {
        @Override
        public void onConnectionRequested(String endpointName, String endpointId, boolean isIncomingRequest) {
            Log.i(TAG, String.format("onConnectionRequested called: userName=%s userId=%s", endpointName, endpointId));

            // kept info for connection
            foundMap.put(endpointId, new Endpoint(endpointId, endpointName));

            // update UI
            if (isIncomingRequest) {
                Message message = new Message();
                EndpointDataView endpointDataView = new EndpointDataView(endpointName, endpointId, ACTION_FOUND_ENDPOINT);
                message.obj = endpointDataView;
                mainHandler.sendMessage(message);
            }
            nearby.acceptConnection(endpointId, userMessageCallback);
        }

        @Override
        public void onConnectionResult(final String endpointId, boolean isConnected) {
            Log.i(TAG, String.format("onConnectionResult called: userId=%s", endpointId));

            String action = ACTION_DISCONNECTED_ENDPOINT;

            Endpoint endpoint = foundMap.get(endpointId);
            if (endpoint == null) {
                Log.e(TAG, String.format("onConnectionResult error: endpointId=%s unknown", endpointId));
                return ;
            }

            foundMap.remove(endpointId);

            if (isConnected) {
                connectedMap.put(endpointId, endpoint);
                action = ACTION_CONNECTED_ENDPOINT;

                // inform BertyBridge that there is a new connection
                BertyBridge.ProximityTransport.handleFoundPeer(endpoint.getName());
            }

            // update UI
            Message message = new Message();
            EndpointDataView endpointDataView = new EndpointDataView(endpointId, action);
            message.obj = endpointDataView;
            mainHandler.sendMessage(message);
        }

        @Override
        public void onDisconnected(String endpointId) {
            Log.i(TAG, String.format("onDisconnected called: userId=%s", endpointId));

            Endpoint endpoint = connectedMap.get(endpointId);
            if (endpoint != null) {
                connectedMap.remove(endpointId);

                // inform BertyBridge that there is a new connection
                BertyBridge.ProximityTransport.handleLostPeer(endpoint.getName());
            } else {
                Log.e(TAG, String.format("onDisconnected error: endpointId=%s not found", endpointId));
            }

            // update UI
            Message message = new Message();
            EndpointDataView endpointDataView = new EndpointDataView(endpointId, ACTION_DISCONNECTED_ENDPOINT);
            message.obj = endpointDataView;
            mainHandler.sendMessage(message);
        }
    };

    UserMessageCallback userMessageCallback = new UserMessageCallback() {
        @Override
        public void onMessageReceived(String userId, String payload) {
            Endpoint endpoint = connectedMap.get(userId);
            if (endpoint == null) {
                Log.e(TAG, String.format("onMessageReceived error: endpointId=%s not found", userId));
                return ;
            }

            BertyBridge.ProximityTransport.receiveFromPeer(endpoint.getName(), payload.getBytes());

            // update UI
            Message message = new Message();
            EndpointDataView endpointDataView = new EndpointDataView(endpoint.getName(), userId, ACTION_RECEIVED_MESSAGE);
            message.obj = endpointDataView;
            mainHandler.sendMessage(message);
        }

        @Override
        public void onFileReceived(String userId, File file) {

        }
    };

    UserSearchCallback userSearchCallback = new UserSearchCallback() {
        @Override
        public void onUserFound(String userName, String userId) {
            Log.i(TAG, String.format("onUserFound called: userName=%s userId=%s", userName, userId));

            // update UI
            Message message = new Message();
            EndpointDataView endpointDataView = new EndpointDataView(userName, userId, ACTION_FOUND_ENDPOINT);
            message.obj = endpointDataView;
            mainHandler.sendMessage(message);

            nearby.connectTo(localPID, userId, userConnectionCallback);
        }

        @Override
        public void onUserLost(String userId) {
            Log.i(TAG, String.format("onUserLost called: userId=%s", userId));

            // update UI
            Message message = new Message();
            EndpointDataView endpointDataView = new EndpointDataView(userId, ACTION_LOST_ENDPOINT);
            message.obj = endpointDataView;
            mainHandler.sendMessage(message);
        }
    };

    public NearbyBertyNativeDriver(Context context, Handler mainHandler) {
        this.mainHandler = mainHandler;
        this.mContext = context;;

        // init driver and bridge
        nearby = BertyDriver.getInstance(mContext);
    }

    @Override
    public void start(String localPID) {
        this.localPID = localPID;
        nearby.startSharing(localPID, SERVICE_ID, userConnectionCallback);
        nearby.startSearching(SERVICE_ID, userSearchCallback);
    }

    @Override
    public void stop() {
        nearby.stopSharing();
        nearby.stopSearching();
    }

    @Override
    public boolean dialPeer(String remotePID) {
        if (getEndpointFromName(remotePID) != null) {
            return true;
        }
        return false;
    }

    @Override
    public boolean sendToPeer(String remotePID, byte[] payload) {
        Endpoint endpoint;

        if ((endpoint = getEndpointFromName(remotePID)) != null) {
            nearby.sendMessage(endpoint.getId(), new String(payload));

            // update UI
            Message message = new Message();
            EndpointDataView endpointDataView = new EndpointDataView(remotePID, endpoint.getId(), ACTION_SENT_MESSAGE);
            message.obj = endpointDataView;
            mainHandler.sendMessage(message);

            return true;
        }

        Log.e(TAG, String.format("sendToPeer error: remotePID=%s not found", remotePID));
            return false;
    }

    @Override
    public void closeConnWithPeer(String remotePID) {
        Endpoint endpoint;

        if ((endpoint = getEndpointFromName(remotePID)) != null) {
            nearby.disconnectFrom(endpoint.getId());
        }
    }

    @Override
    public long protocolCode() {
        return ProtocolCode;
    }

    @Override
    public String protocolName() {
        return ProtocolName;
    }

    @Override
    public String defaultAddr() {
        return DefaultAddr;
    }

    private Endpoint getEndpointFromName(String name) {
        String id;

        for (Endpoint endpoint: connectedMap.values()) {
            if (endpoint.getName().equals(name)) {
                return endpoint;
            }
        }
        return null;
    }

    private class Endpoint {
        String id;
        String name;

        public Endpoint(String id, String name) {
            this.id = id;
            this.name = name;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }
    }
}
