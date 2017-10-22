package com.frc8.team8vision.networking;

import com.frc8.team8vision.util.Constants;

import android.app.Activity;
import android.util.Log;

/**
 * Base class for vision threads
 *
 * @author Quintin Dwight
 */
public abstract class AbstractVisionThread implements Runnable {

    public enum ThreadState {
        PRE_INIT, RUNNING, PAUSED, STOPPED
    }

    protected Activity m_activity;
    protected double m_secondsAlive = 0.0d;
    protected long m_updateRate;
    protected final String k_tag;
    protected boolean m_isRunning = false;
    protected ThreadState m_threadState = ThreadState.PRE_INIT;

    public double getTimeAlive() { return m_secondsAlive; }
    public boolean isRunning() { return m_isRunning; }

    protected AbstractVisionThread(final String k_threadName) {
        k_tag = Constants.kTAG + k_threadName;
    }

    protected void setThreadState(ThreadState state) {
        m_threadState = state;
    }

    /**
     * Starts the thread
     */
    public void start(Activity activity, final long k_updateRate) {

        m_activity = activity;

        m_threadState = ThreadState.PRE_INIT;

        m_updateRate = k_updateRate;

        if (m_isRunning) {
            Log.e(k_tag, "Thread is already running! Aborting...");
            return;
        }

        init();

        Log.i(k_tag, "Starting thread...");
        m_isRunning = true;
        new Thread(this).start();
    }

    /**
     * Called by {@link #start} after it has been verified that the thread can run
     */
    protected abstract void init();

    @Override
    public void run() {

        while (m_isRunning) {

            if (m_threadState == ThreadState.PRE_INIT) {
                Log.e(k_tag, "Thread has not been initialized in running state! Aborting...");
                return;
            }

            update();

            try {
                Thread.sleep(m_updateRate);
                m_secondsAlive += m_updateRate / 1000.0;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Called by {@link #run} every time the thread updates
     */
    protected abstract void update();

    /**
     * Pauses the thread
     */
    public void pause() {

        if (!m_isRunning)
            Log.e(k_tag, "Cannot pause a running thread!");

        setThreadState(ThreadState.PAUSED);

        onPause();
    }

    protected abstract void onPause();

    /**
     * Resume the thread
     */
    public void resume() {

        if (m_threadState == ThreadState.STOPPED || !m_isRunning)
            Log.e(k_tag, "Thread cannot be resumed from a stopped state!");

        setThreadState(ThreadState.RUNNING);

        onResume();
    }

    protected abstract void onResume();

    /**
     * Stops the thread completely
     */
    public void stop() {

        Log.i(k_tag, "Stopping thread...");

        m_isRunning = false;
        setThreadState(ThreadState.STOPPED);

        onStop();
    }

    protected abstract void onStop();
}
