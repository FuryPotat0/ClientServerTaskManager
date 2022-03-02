package main.client;

public class WorkingThread implements Runnable {
    protected final static int INTERVAL = 100;
    protected boolean running = false;
    private Thread worker;


    public WorkingThread() {
    }

    public void start() {
        worker = new Thread(this);
        worker.start();
    }

    public void interrupt() {
        running = false;
        worker.interrupt();
    }

    public void run() {
    }
}

