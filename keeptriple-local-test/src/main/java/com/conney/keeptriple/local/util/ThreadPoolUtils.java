/**
 * Created by louis on 2020/10/12.
 */
package com.conney.keeptriple.local.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Utility methods for working with thread pools {@link ExecutorService}.
 */
public final class ThreadPoolUtils {

    private static final Logger LOG = LoggerFactory.getLogger(ThreadPoolUtils.class);

    public static final long DEFAULT_SHUTDOWN_AWAIT_TERMINATION = 60 * 1000L;

    /**
     * Shutdown the given executor service only (ie not graceful shutdown).
     *
     * @see ExecutorService#shutdown()
     */
    public static void shutdown(ExecutorService executorService) {
        doShutdown(executorService, 0);
    }

    /**
     * Shutdown now the given executor service aggressively.
     *
     * @param executorService the executor service to shutdown now
     * @return list of tasks that never commenced execution
     * @see ExecutorService#shutdownNow()
     */
    public static List<Runnable> shutdownNow(ExecutorService executorService) {
        List<Runnable> answer = null;
        if (!executorService.isShutdown()) {
            LOG.debug("Forcing shutdown of ExecutorService: {}", executorService);
            answer = executorService.shutdownNow();
            if (LOG.isTraceEnabled()) {
                LOG.trace("Shutdown of ExecutorService: {} is shutdown: {} and terminated: {}.",
                        new Object[]{executorService, executorService.isShutdown(), executorService.isTerminated()});
            }
        }

        return answer;
    }

    /**
     * Shutdown the given executor service graceful at first, and then aggressively
     * if the await termination timeout was hit.
     * <p/>
     * This implementation invokes the {@link #shutdownGraceful(ExecutorService, long)}
     * with a timeout value of {@link #DEFAULT_SHUTDOWN_AWAIT_TERMINATION} millis.
     */
    public static void shutdownGraceful(ExecutorService executorService) {
        doShutdown(executorService, DEFAULT_SHUTDOWN_AWAIT_TERMINATION);
    }

    /**
     * Shutdown the given executor service graceful at first, and then aggressively
     * if the await termination timeout was hit.
     * <p/>
     * Will try to perform an orderly shutdown by giving the running threads
     * time to complete tasks, before going more aggressively by doing a
     * {@link #shutdownNow(ExecutorService)} which
     * forces a shutdown. The parameter <tt>shutdownAwaitTermination</tt>
     * is used as timeout value waiting for orderly shutdown to
     * complete normally, before going aggressively.  If the shutdownAwaitTermination
     * value is negative the shutdown waits indefinitely for the ExecutorService
     * to complete its shutdown.
     *
     * @param executorService the executor service to shutdown
     * @param shutdownAwaitTermination timeout in millis to wait for orderly shutdown
     */
    public static void shutdownGraceful(ExecutorService executorService, long shutdownAwaitTermination) {
        doShutdown(executorService, shutdownAwaitTermination);
    }

    private static void doShutdown(ExecutorService executorService, long shutdownAwaitTermination) {
        // code from Apache Camel - org.apache.camel.impl.DefaultExecutorServiceManager

        if (executorService == null) {
            return;
        }

        // shutting down a thread pool is a 2 step process. First we try graceful, and if that fails, then we go more aggressively
        // and try shutting down again. In both cases we wait at most the given shutdown timeout value given
        // (total wait could then be 2 x shutdownAwaitTermination, but when we shutdown the 2nd time we are aggressive and thus
        // we ought to shutdown much faster)
        if (!executorService.isShutdown()) {
            boolean warned = false;
            StopWatch watch = new StopWatch();

            LOG.info("Shutdown of ExecutorService: {} with await termination: {} millis", executorService, shutdownAwaitTermination);
            executorService.shutdown();

            if (shutdownAwaitTermination > 0) {
                try {
                    if (!awaitTermination(executorService, shutdownAwaitTermination)) {
                        warned = true;
                        LOG.warn("Forcing shutdown of ExecutorService: {} due first await termination elapsed.", executorService);
                        executorService.shutdownNow();
                        // we are now shutting down aggressively, so wait to see if we can completely shutdown or not
                        if (!awaitTermination(executorService, shutdownAwaitTermination)) {
                            LOG.warn("Cannot completely force shutdown of ExecutorService: {} due second await termination elapsed.", executorService);
                        }
                    }
                } catch (InterruptedException e) {
                    warned = true;
                    LOG.warn("Forcing shutdown of ExecutorService: {} due interrupted.", executorService);
                    // we were interrupted during shutdown, so force shutdown
                    try {
                        executorService.shutdownNow();
                    } finally {
                        Thread.currentThread().interrupt();
                    }
                }
            } else  if (shutdownAwaitTermination < 0) {
                try {
                    awaitTermination(executorService);
                } catch (InterruptedException e) {
                    warned = true;
                    LOG.warn("Forcing shutdown of ExecutorService: {} due interrupted.", executorService);
                    // we were interrupted during shutdown, so force shutdown
                    try {
                        executorService.shutdownNow();
                    } finally {
                        Thread.currentThread().interrupt();
                    }
                }
            }

            // if we logged at WARN level, then report at INFO level when we are complete so the end user can see this in the log
            if (warned) {
                LOG.info("Shutdown of ExecutorService: {} is shutdown: {} and terminated: {} took: {}.",
                        new Object[]{executorService, executorService.isShutdown(), executorService.isTerminated(), TimeUtils.printDuration(watch.taken())});
            } else if (LOG.isDebugEnabled()) {
                LOG.debug("Shutdown of ExecutorService: {} is shutdown: {} and terminated: {} took: {}.",
                        new Object[]{executorService, executorService.isShutdown(), executorService.isTerminated(), TimeUtils.printDuration(watch.taken())});
            }
        }
    }

    /**
     * Awaits the termination of the thread pool indefinitely (Use with Caution).
     * <p/>
     * This implementation will log every 2nd second at INFO level that we are waiting, so the end user
     * can see we are not hanging in case it takes longer time to terminate the pool.
     *
     * @param executorService            the thread pool
     *
     * @throws InterruptedException is thrown if we are interrupted during the waiting
     */
    public static void awaitTermination(ExecutorService executorService) throws InterruptedException {
        // log progress every 5th second so end user is aware of we are shutting down
        StopWatch watch = new StopWatch();
        final long interval = 2000;
        while (true) {
            if (executorService.awaitTermination(interval, TimeUnit.MILLISECONDS)) {
                return;
            } else {
                LOG.info("Waited {} for ExecutorService: {} to terminate...", TimeUtils.printDuration(watch.taken()), executorService);
            }
        }
    }

    /**
     * Awaits the termination of the thread pool.
     * <p/>
     * This implementation will log every 2nd second at INFO level that we are waiting, so the end user
     * can see we are not hanging in case it takes longer time to terminate the pool.
     *
     * @param executorService            the thread pool
     * @param shutdownAwaitTermination   time in millis to use as timeout
     * @return <tt>true</tt> if the pool is terminated, or <tt>false</tt> if we timed out
     * @throws InterruptedException is thrown if we are interrupted during the waiting
     */
    public static boolean awaitTermination(ExecutorService executorService, long shutdownAwaitTermination) throws InterruptedException {
        // log progress every 5th second so end user is aware of we are shutting down
        StopWatch watch = new StopWatch();
        long interval = Math.min(2000, shutdownAwaitTermination);
        boolean done = false;
        while (!done && interval > 0) {
            if (executorService.awaitTermination(interval, TimeUnit.MILLISECONDS)) {
                done = true;
            } else {
                LOG.info("Waited {} for ExecutorService: {} to terminate...", TimeUtils.printDuration(watch.taken()), executorService);
                // recalculate interval
                interval = Math.min(2000, shutdownAwaitTermination - watch.taken());
            }
        }

        return done;
    }

    /**
     * shutdown ExecutorService list gracefully
     * @param executorServiceList
     * @return
     */
    public static CompletableFuture<Void> shutdownGraceful(List<ExecutorService> executorServiceList) {
        List<ExecutorService> allExecutorServiceList = executorServiceList.stream().filter((service) -> service != null).collect(Collectors.toList());
        if (allExecutorServiceList == null || allExecutorServiceList.size() == 0) {
            return null;
        }

        Executor shutdownExecutor = Executors.newFixedThreadPool(allExecutorServiceList.size());
        CompletableFuture<Void>[] cfArrays = new CompletableFuture[allExecutorServiceList.size()];
        int index = 0;
        for (ExecutorService executorService : allExecutorServiceList) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() ->
                            ThreadPoolUtils.shutdownGraceful(executorService)
                    , shutdownExecutor);
            cfArrays[index] = future;
            index++;
        }

        return CompletableFuture.allOf(cfArrays);
    }
}

