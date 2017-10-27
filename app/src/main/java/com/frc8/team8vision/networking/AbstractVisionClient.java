package com.frc8.team8vision.networking;

import android.app.Activity;
import android.provider.Settings;
import android.provider.SyncStateContract;
import android.util.AtomicFile;
import android.util.Log;

import com.frc8.team8vision.util.Constants;

import java.io.IOException;
import java.net.Socket;
import java.util.Arrays;

/**
 * Base class for vision servers. Implements {@link AbstractVisionThread}.
 *
 * @author Quintin Dwight
 */
public abstract class AbstractVisionClient extends AbstractVisionThread {

    public enum SocketState {
        PRE_INIT, ATTEMPTING_CONNECTION, OPEN
    }

    protected boolean m_testing = false;
    protected int m_port = 0;
    protected String m_hostName = "";
    protected Socket m_client;
    protected SocketState m_socketState = SocketState.PRE_INIT;

    protected AbstractVisionClient(final String k_threadName) {
        super(k_threadName);
    }

    @Override
    @Deprecated
    public void start(Activity activity, final long k_updateRate) {

        super.start(null, k_updateRate);
    }

    /**
     * Starts the server thread
     *
     * @param activity Current activity
     * @param k_updateRate Update rate of the thread
     * @param k_hostName The host name as a string
     * @param k_port The port to connect to the server
     * @param k_testing Whether or not we are testing
     */
    public void start(Activity activity, final long k_updateRate, final String k_hostName, final int k_port, final boolean k_testing)
    {
        super.start(activity, k_updateRate);

        m_hostName = k_hostName;
        m_testing = k_testing;
        m_port = k_port;
    }

    @Override
    protected void init() {

        if (m_socketState != SocketState.PRE_INIT) {
            Log.e(k_tag, "Thread has already been initialized. Aborting...");
            return;
        }

        m_socketState = SocketState.ATTEMPTING_CONNECTION;
    }

    protected abstract void afterInit();

    /**
     * Sets the state of the server
     *
     * @param state State of the server
     */
    protected void setSocketState(SocketState state) {
        m_socketState = state;
    }

    @Override
    protected void onStop() {

        closeSocket();
    }

    @Override
    protected void onResume() {}

    @Override
    protected void onPause() {}

    /**
     * Attempts to close the current socket.
     */
    protected void closeSocket() {

        try {
            m_client.close();
        } catch (IOException e) {
            Log.e(k_tag, "Error closing socket on stop: ");
            e.printStackTrace();
        }
    }

    /**
     * Attempt to connect to the server.
     *
     * @return Whether or not we connected successfully.
     */
    protected SocketState attemptConnection() {

        try {
            // Attempt to connect to server
            m_client = new Socket(m_hostName, m_port);
            return SocketState.OPEN;
        } catch (IOException e) {
            return SocketState.ATTEMPTING_CONNECTION;
        }
    }

    /**
     * Check to see if we are still connected to the server.
     *
     * @return Whether or not we are connected.
     */
    protected SocketState checkConnection() {

        final boolean connected = m_client.isConnected();

        if (!connected) Log.e(k_tag, "Lost connection to socket.");

        return connected ? SocketState.OPEN : SocketState.ATTEMPTING_CONNECTION;
    }

    @Override
    protected void update()
    {
        switch (m_threadState) {

            case RUNNING: {
                switch (m_socketState) {

                    case PRE_INIT: {
                        Log.e(k_tag, "Thread client state is not initialized while in update.");
                        break;
                    } case OPEN: {
                        setSocketState(checkConnection());
                        break;
                    } case ATTEMPTING_CONNECTION: {
                        setSocketState(attemptConnection());
                        break;
                    }
                }
                break;
            }
        }

        afterUpdate();
    }

    protected abstract void afterUpdate();
}
