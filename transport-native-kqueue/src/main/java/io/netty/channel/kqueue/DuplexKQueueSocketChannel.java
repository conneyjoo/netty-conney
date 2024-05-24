/*
 * Copyright 2016 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package io.netty.channel.kqueue;

import io.netty.channel.ChannelPromise;
import io.netty.channel.DefaultChannelPipeline;
import io.netty.channel.DefaultSelectStrategyFactory;
import io.netty.channel.DuplexChannelOutboundBuffer;
import io.netty.channel.DuplexChannelPipeline;
import io.netty.channel.EventLoop;
import io.netty.channel.SingleThreadEventLoop;
import io.netty.channel.socket.DuplexSocketChannel;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.concurrent.GlobalEventExecutor;
import io.netty.util.concurrent.RejectedExecutionHandlers;
import io.netty.util.concurrent.ThreadPerTaskExecutor;

import java.io.IOException;
import java.util.concurrent.Executor;

public class DuplexKQueueSocketChannel extends KQueueSocketChannel implements DuplexSocketChannel {

    protected boolean writeWFilterEnabled;

    private KQueueEventLoop writeEventLoop;

    public DefaultChannelPipeline newChannelPipeline() {
        return new DuplexChannelPipeline(this);
    }

    public KQueueEventLoop peelWriteEventLoop() {
        KQueueEventLoop eventLoop = (KQueueEventLoop) eventLoop();
        eventLoop.evSet(this, Native.EVFILT_WRITE, Native.EV_DELETE_DISABLE, 0);
        return createWriteEventLoop(eventLoop);
    }

    public KQueueEventLoop createWriteEventLoop(SingleThreadEventLoop eventLoop) {
        try {
            String poolName = eventLoop.thread.getName() + "-write";
            ThreadPerTaskExecutor executor = new ThreadPerTaskExecutor(new DefaultThreadFactory(poolName));
            return new KQueueEventLoop(eventLoop.parent(), executor, 0,
                    DefaultSelectStrategyFactory.INSTANCE.newSelectStrategy(),
                    RejectedExecutionHandlers.reject(), null);
        } catch (Exception e) {
            throw new IllegalStateException("failed to create a child event loop", e);
        }
    }

    public void doRegisterWriteEventLoop(KQueueEventLoop writeEventLoop) throws Exception {
        writeEventLoop.add(this);
        this.writeEventLoop = writeEventLoop;
    }

    public void writeFilter(boolean writeFilterEnabled) throws IOException {
        if (writeEventLoop == null || eventLoop.inEventLoop()) {
            super.writeFilter(writeFilterEnabled);
        } else if (writeEventLoop.inEventLoop()) {
            writeWFilter(writeFilterEnabled);
        }
    }

    public void writeWFilter(boolean writeFilterEnabled) throws IOException {
        if (this.writeWFilterEnabled != writeFilterEnabled) {
            this.writeWFilterEnabled = writeFilterEnabled;
            wevSet(Native.EVFILT_WRITE, writeFilterEnabled ? Native.EV_ADD_CLEAR_ENABLE : Native.EV_DELETE_DISABLE);
        }
    }

    private void wevSet(short filter, short flags) {
        if (isRegistered()) {
            wevSet0(filter, flags, 0);
        }
    }

    private void wevSet0(short filter, short flags, int fflags) {
        if (isOpen()) {
            ((KQueueEventLoop) writeEventLoop()).evSet(this, filter, flags, fflags);
        }
    }

    @Override
    protected AbstractKQueueUnsafe newUnsafe() {
        return new DuplexKQueueSocketChannelUnsafe();
    }

    private final class DuplexKQueueSocketChannelUnsafe extends KQueueStreamUnsafe {

        DuplexKQueueSocketChannelUnsafe() {
            this.outboundBuffer = new DuplexChannelOutboundBuffer(DuplexKQueueSocketChannel.this);
        }

        @Override
        protected Executor prepareToClose() {
            try {
                // Check isOpen() first as otherwise it will throw a RuntimeException
                // when call getSoLinger() as the fd is not valid anymore.
                if (isOpen() && config().getSoLinger() > 0) {
                    // We need to cancel this key of the channel so we may not end up in a eventloop spin
                    // because we try to read or write until the actual close happens which may be later due
                    // SO_LINGER handling.
                    // See https://github.com/netty/netty/issues/4449
                    ((KQueueEventLoop) eventLoop()).remove(DuplexKQueueSocketChannel.this);
                    ((KQueueEventLoop) writeEventLoop()).remove(DuplexKQueueSocketChannel.this);
                    return GlobalEventExecutor.INSTANCE;
                }
            } catch (Throwable ignore) {
                // Ignore the error as the underlying channel may be closed in the meantime and so
                // getSoLinger() may produce an exception. In this case we just return null.
                // See https://github.com/netty/netty/issues/4449
            }
            return null;
        }

        @Override
        public void fulfillConnectPromise(ChannelPromise promise, boolean wasActive) {
            if (promise == null) {
                // Closed via cancellation and the promise has been notified already.
                return;
            }
            active = true;

            // Get the state as trySuccess() may trigger an ChannelFutureListener that will close the Channel.
            // We still need to ensure we call fireChannelActive() in this case.
            boolean active = isActive();

            // trySuccess() will return false if a user cancelled the connection attempt.
            boolean promiseSet = promise.trySuccess();

            // Regardless if the connection attempt was cancelled, channelActive() event should be triggered,
            // because what happened is what happened.
            if (!wasActive && active) {
                activeWriteEventLoop(promise);
                pipeline().fireChannelActive();
            }

            // If a user cancelled the connection attempt, close the channel, which is followed by channelInactive().
            if (!promiseSet) {
                close(voidPromise());
            }
        }

        @Override
        public void finishConnect() {
            if (writeEventLoop != null && writeEventLoop.inEventLoop()) {
                return;
            }
            super.finishConnect();
        }

        public void activeWriteEventLoop(ChannelPromise promise) {
            try {
                KQueueEventLoop writeEventLoop = peelWriteEventLoop();
                doRegisterWriteEventLoop(writeEventLoop);
            } catch (Throwable t) {
                closeForcibly();
                closeFuture.setClosed();
                safeSetFailure(promise, t);
            }
        }
    }

    public EventLoop writeEventLoop() {
        return writeEventLoop;
    }
}
