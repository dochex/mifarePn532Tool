package mifareTools;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class SerialPortThreadFactory {
    private static final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "SerialPortCommunicationThread");
        thread.setDaemon(true);
        return thread;
    });
    
    public static Future<?> submit(Runnable task) {
        return executor.submit(task);
    }
    
    public static <T> Future<T> submit(Callable<T> task) {
        return executor.submit(task);
    }
    
    public static void shutdown() {
        executor.shutdown();
    }
}
