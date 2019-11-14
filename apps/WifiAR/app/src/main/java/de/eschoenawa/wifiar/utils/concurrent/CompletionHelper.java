package de.eschoenawa.wifiar.utils.concurrent;

import android.util.Log;

/**
 * This class provides synchronization utility for waiting for a number of running tasks to finish
 * while the tasks themselves can create new tasks or the number of tasks is initially unknown.
 *
 * @author Emil Schoenawa
 */
public class CompletionHelper {
    private static final String TAG = "CompletionHelper";
    private int numberOfTasks = 0;
    private final Object lock = new Object();

    public void beforeSubmit() {
        synchronized (lock) {
            Log.d(TAG, "New task will be submitted. New task-count: " + ++numberOfTasks);
        }
    }

    public void beforeSubmit(int numberOfNewTasks) {
        synchronized (lock) {
            numberOfTasks += numberOfNewTasks;
            Log.d(TAG, numberOfNewTasks + " new tasks will be submitted. New task-count: " + numberOfTasks);
        }
    }

    public void taskCompleted() {
        synchronized (lock) {
            Log.d(TAG, "A task completed. New task-count: " + --numberOfTasks);
            if (numberOfTasks == 0) {
                Log.d(TAG, "Number of tasks is 0, calling notifyAll().");
                lock.notifyAll();
            }
        }
    }

    /**
     * This method is blocking until all tasks are completed
     * @throws InterruptedException If the waiting thread is interrupted
     */
    public void awaitCompletion() throws InterruptedException {
        synchronized (lock) {
            while (numberOfTasks > 0) {
                lock.wait();
            }
            Log.d(TAG, "awaitCompletion() has concluded, all tasks are done.");
        }
    }
}
