package com.example.BertyBridge;

//

import android.content.Context;

import androidx.annotation.NonNull;

import java.io.File;

import tech.berty.bertysdk.lifecycle.UserConnectionCallback;
import tech.berty.bertysdk.lifecycle.UserMessageCallback;
import tech.berty.bertysdk.lifecycle.UserSearchCallback;

/**
 * This interface to be implemented by your BLE driver.
 * The proximity transport uses these methods to control the driver.
 * flow way: golang -> java
 */
public interface BertyNativeDriver {
    /**
     * Start.
     *
     * @param localPID the local pid
     */
    public void start(String localPID);

    /**
     * Stop.
     */
    public void stop();

    /**
     * Dial peer boolean.
     *
     * @param remotePID the remote pid
     * @return the boolean
     */
    public boolean dialPeer(String remotePID);

    /**
     * Send to peer boolean.
     *
     * @param remotePID the remote pid
     * @param payload   the payload
     * @return the boolean
     */
    public boolean sendToPeer(String remotePID, byte[] payload);

    /**
     * Close conn with peer.
     *
     * @param remotePID the remote pid
     */
    public void closeConnWithPeer(String remotePID);

    /**
     * Protocol code long.
     *
     * @return the long
     */
    public long protocolCode();

    /**
     * Protocol name string.
     *
     * @return the string
     */
    public String protocolName();

    /**
     * Default addr string.
     *
     * @return the string
     */
    public String defaultAddr();
}
