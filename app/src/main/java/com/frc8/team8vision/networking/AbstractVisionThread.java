package com.frc8.team8vision.networking;

import com.frc8.team8vision.util.Constants;

import android.util.Log;

/**
 * Base class for vision threads
 *
 * @author Quintin Dwight
 */
public abstract class AbstractVisionThread implements Runnable {

    protected double m_secondsAlive = 0.0d;
    protected long m_updateRate;
    protected final String k_tag;
    protected boolean m_isRunning = false;

    public double getTimeAlive() { return m_secondsAlive; }
    public boolean isRunning() { return m_isRunning; }

    protected AbstractVisionThread(final String k_threadName) {
        k_tag = Constants.kTAG + k_threadName;
    }

    /**
     * Starts the thread
     */
    public void start(final long k_updateRate) {

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
     * Handles destroying the thread
     */
    public void destroy() {

        m_isRunning = false;
        tearDown();
    }

    /**
     * Called by {@link #destroy} whenever the thread should stop running
     */
    protected abstract void tearDown();
}
