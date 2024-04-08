package com.conney.keeptriple.local.net.channel;

import com.conney.keeptriple.local.net.proto.Proto;
import io.netty.channel.Channel;
import io.netty.channel.DefaultChannelPromise;
import io.netty.handler.timeout.ReadTimeoutException;
import io.netty.util.Attribute;
import io.netty.util.concurrent.EventExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class SerialChannelPromise<T extends Proto> extends DefaultChannelPromise {

    private static final Logger logger = LoggerFactory.getLogger(SerialChannelPromise.class);

    public static final long DEFAULT_RECV_TIMEOUT = 10000;

    private volatile short waiters;

    private volatile T value;

    private volatile boolean invalid = false;

    public SerialChannelPromise(Channel channel, EventExecutor executor) {
        super(channel, executor);
    }

    public T writeAndFlush(T t) {
        return writeAndFlush(t, DEFAULT_RECV_TIMEOUT);
    }

    public T writeAndFlush(T t, long timeout) {
        try {
            Channel channel = channel();
            Attribute<InterlocutionPipeline<T>> attribute = InterlocutionPipeline.getInterlocutionPipeline(channel);
            InterlocutionPipeline pipeline = attribute.get();

            if (pipeline == null) {
                pipeline = new InterlocutionPipeline();
                InterlocutionPipeline old = attribute.setIfAbsent(pipeline);
                pipeline = old != null ? old : pipeline;
            }

            pipeline.ask(t, this);
            channel.writeAndFlush(t, this);

            if (!awaiting(timeout)) {
                invalid = pipeline.remove(t, this);
                throw ReadTimeoutException.INSTANCE;
            }

            return getValue();
        } catch (InterruptedException e) {
            logger.error(e.getMessage(), e);
            return null;
        }
    }

    public boolean wakeUp(T t) {
        setValue(t);
        checkNotifyWaiters();
        return true;
    }

    public boolean awaiting(final long timeoutMillis) throws InterruptedException {
        return awaiting(TimeUnit.MILLISECONDS.toNanos(timeoutMillis), true);
    }

    public boolean awaiting(final long timeoutNanos, boolean interruptable) throws InterruptedException {
        if (timeoutNanos <= 0) {
            return isDone();
        }

        if (interruptable && Thread.interrupted()) {
            throw new InterruptedException(toString());
        }

        checkDeadLock();

        long startTime = System.nanoTime();
        boolean interrupted = false;

        try {
            for (long waitTime = timeoutNanos; waitTime > 0; waitTime = timeoutNanos - (System.nanoTime() - startTime)) {
                synchronized (this) {
                    if (isDocked()) {
                        return true;
                    }

                    incWaiters();

                    try {
                        wait(waitTime / 1000000, (int) (waitTime % 1000000));
                    } catch (InterruptedException e) {
                        if (interruptable) {
                            throw e;
                        } else {
                            interrupted = true;
                        }
                    } finally {
                        decWaiters();
                    }
                }
            }

            return isDocked();
        } finally {
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void incWaiters() {
        if (waiters == Short.MAX_VALUE) {
            throw new IllegalStateException("too many waiters: " + this);
        }
        ++waiters;
    }

    private void decWaiters() {
        --waiters;
    }

    public synchronized void checkNotifyWaiters() {
        if (waiters > 0) {
            notifyAll();
        }
    }

    public boolean isDocked() {
        return value != null;
    }

    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        this.value = value;
    }

    public boolean invalid() {
        return invalid;
    }
}
