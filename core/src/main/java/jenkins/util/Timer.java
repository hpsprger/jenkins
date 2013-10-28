package jenkins.util;


import javax.annotation.Nonnull;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * Holds the {@link ScheduledExecutorService} for running all background tasks in Jenkins.
 * This ExecutorService will create additional threads to execute due (enabled) tasks.
 *
 * Provides a minimal abstraction for locating the ScheduledExecutorService so that we
 * can modify it's behavior going forward. For instance, to add managability/monitoring.
 *
 * This is not an @Extension because it must be available before any extensions are loaded.
 *
 * Plugins should probably use one of the following as they provide higher level abstractions:
 *   {@link hudson.model.AperiodicWork}, {@link hudson.model.PeriodicWork},
 *   {@link hudson.model.AsyncAperiodicWork}, {@link hudson.model.AsyncPeriodicWork}.
 *
 * @author Ryan Campbell
 * @since 1.538
 */
public class Timer {

    /**
     * The scheduled executor thread pool. This is initialized lazily since it may be created/shutdown many times
     * when running the test suite.
     */
    private static ScheduledExecutorService executorService = null;

    /**
     * Returns the scheduled executor service used by all timed tasks in Jenkins.
     *
     * @return the single {@link ScheduledExecutorService}.
     */
    @Nonnull
    public static synchronized ScheduledExecutorService get() {
        if (executorService == null) {
            // corePoolSize is set to 10, but will only be created if needed.
            // ScheduledThreadPoolExecutor "acts as a fixed-sized pool using corePoolSize threads"
            executorService = Executors.newScheduledThreadPool(10, new ThreadFactory() {

                private final AtomicInteger threadNumber = new AtomicInteger(1);

                @Override
                public Thread newThread(Runnable r) {
                    Thread t = new Thread(r);
                    t.setName("Jenkins-cron-thread-" + threadNumber.getAndIncrement());
                    t.setDaemon(true);
                    return t;
                }
            });
        }
        return executorService;
    }

    /**
     * Shutdown the timer and throw it away.
     */
    public static synchronized void shutdown() {
        executorService.shutdownNow();
        executorService = null;
    }

    /**
     * Do not create this.
     */
    private Timer() {};

}
