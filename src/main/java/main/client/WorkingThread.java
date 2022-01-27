package main.client;

import java.util.concurrent.atomic.AtomicBoolean;

public class WorkingThread implements Runnable {
    protected final static int INTERVAL = 100;
    protected final AtomicBoolean running = new AtomicBoolean(false);
    private Thread worker;


    public WorkingThread() {
    }

    public void start() {
        worker = new Thread(this);
        worker.start();
    }

    public void interrupt() {
        running.set(false);
        worker.interrupt();
    }

    public void run() {
    }
}

