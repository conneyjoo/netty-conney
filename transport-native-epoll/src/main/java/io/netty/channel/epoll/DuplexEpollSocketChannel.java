/*
 * Copyright 2014 The Netty Project
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
package io.netty.channel.epoll;

import io.netty.channel.ChannelPromise;
import io.netty.channel.DefaultChannelPipeline;
import io.netty.channel.DefaultSelectStrategyFactory;
import io.netty.channel.DuplexChannelPipeline;
import io.netty.channel.EventLoop;
import io.netty.channel.socket.DuplexSocketChannel;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.concurrent.GlobalEventExecutor;
import io.netty.util.concurrent.RejectedExecutionHandlers;
import io.netty.util.concurrent.ThreadPerTaskExecutor;

import java.io.IOException;
import java.util.concurrent.Executor;

public final class DuplexEpollSocketChannel extends EpollSocketChannel implements DuplexSocketChannel {

    protected int wflags = Native.EPOLLET;

    private EpollEventLoop writeEventLoop;

    public DefaultChannelPipeline newChannelPipeline() {
        return new DuplexChannelPipeline(this);
    }

    public EpollEventLoop peelWriteEventLoop() throws Exception {
        EpollEventLoop eventLoop = (EpollEventLoop) eventLoop();
        clearFlag(Native.EPOLLOUT);
        return createWriteEventEventLoop(eventLoop);
    }

    public EpollEventLoop createWriteEventEventLoop(EpollEventLoop eventLoop) {
        try {
            String poolName = eventLoop.thread.getName() + "-write";
            ThreadPerTaskExecutor executor = new ThreadPerTaskExecutor(new DefaultThreadFactory(poolName));
            return new EpollEventLoop(eventLoop.parent(), executor, 0,
                    DefaultSelectStrategyFactory.INSTANCE.newSelectStrategy(),
                    RejectedExecutionHandlers.reject(), null);
        } catch (Exception e) {
            throw new IllegalStateException("failed to create a child event loop", e);
        }
    }

    public void doRegisterWriteEventLoop(EpollEventLoop writeEventLoop) throws Exception {
        addWriteEventLoop(writeEventLoop);
        setWFlag(writeEventLoop, Native.EPOLLOUT);
        this.writeEventLoop = writeEventLoop;
    }

    public void addWriteEventLoop(EpollEventLoop writeEventLoop) throws IOException {
        assert writeEventLoop.inEventLoop();
        int fd = this.socket.intValue();
        Native.epollCtlAdd(writeEventLoop.epollFd.intValue(), fd, wflags);
        AbstractEpollChannel old = writeEventLoop.channels.put(fd, this);
        assert old == null || !old.isOpen();
    }

    @Override
    void setFlag(int flag) throws IOException {
        if (writeEventLoop == null || eventLoop.inEventLoop()) {
            super.setFlag(flag);
        } else if (writeEventLoop.inEventLoop()) {
            setWFlag(flag);
        }
    }

    @Override
    void clearFlag(int flag) throws IOException {
        if (writeEventLoop == null || eventLoop.inEventLoop()) {
            super.clearFlag(flag);
        } else if (writeEventLoop.inEventLoop()) {
            clearWFlag(writeEventLoop, flag);
        }
    }

    void setWFlag(int flag) throws IOException {
        setWFlag(writeEventLoop, flag);
    }

    void setWFlag(EpollEventLoop writeEventLoop, int flag) throws IOException {
        if (!isWFlagSet(flag)) {
            wflags |= flag;
            modifyWEvents(writeEventLoop);
        }
    }

    void clearWFlag(EpollEventLoop writeEventLoop, int flag) throws IOException {
        if (isWFlagSet(flag)) {
            wflags &= ~flag;
            modifyWEvents(writeEventLoop);
        }
    }

    boolean isWFlagSet(int flag) {
        return (wflags & flag) != 0;
    }

    private void modifyWEvents(EpollEventLoop writeEventLoop) throws IOException {
        if (isOpen() && isRegistered()) {
            assert writeEventLoop.inEventLoop();
            Native.epollCtlMod(writeEventLoop.epollFd.intValue(), this.socket.intValue(), wflags);
        }
    }

    @Override
    protected AbstractEpollUnsafe newUnsafe() {
        return new DuplexEpollSocketChannelUnsafe();
    }

    private final class DuplexEpollSocketChannelUnsafe extends EpollStreamUnsafe {

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
                    ((EpollEventLoop) eventLoop()).remove(DuplexEpollSocketChannel.this);
                    ((EpollEventLoop) writeEventLoop()).remove(DuplexEpollSocketChannel.this);
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
                EpollEventLoop writeEventLoop = peelWriteEventLoop();
                doRegisterWriteEventLoop(writeEventLoop);
            } catch (Throwable t) {
                closeForcibly();
                closeFuture.setClosed();
                safeSetFailure(promise, t);
            }
        }
    }

    @Override
    public EventLoop writeEventLoop() {
        return writeEventLoop;
    }
}
