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
package io.netty.channel;

import io.netty.channel.socket.DuplexSocketChannel;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.EventExecutorGroup;
import io.netty.util.internal.ObjectUtil;

import java.net.SocketAddress;

import static io.netty.channel.ChannelHandlerMask.MASK_CHANNEL_WRITABILITY_CHANGED;
import static io.netty.channel.ChannelHandlerMask.MASK_FLUSH;
import static io.netty.channel.ChannelHandlerMask.MASK_USER_EVENT_TRIGGERED;
import static io.netty.channel.ChannelHandlerMask.MASK_WRITE;

public class DuplexChannelPipeline extends DefaultChannelPipeline {

    public DuplexChannelPipeline(Channel channel) {
        this.channel = ObjectUtil.checkNotNull(channel, "channel");
        this.succeededFuture = new SucceededChannelFuture(channel, null);
        this.voidPromise = new VoidChannelPromise(channel, true);

        this.tail = new DuplexTailContext(this);
        this.head = new DuplexHeadContext(this);

        head.next = tail;
        tail.prev = head;
    }

    @Override
    public AbstractChannelHandlerContext newContext(EventExecutorGroup group, String name, ChannelHandler handler) {
        return new DuplexChannelHandlerContext(this, childExecutor(group), name, handler);
    }

    @Override
    public ChannelPipeline fireChannelActive() {
        final DuplexHeadContext next = (DuplexHeadContext) head;
        EventExecutor executor = next.readExecutor();
        if (executor.inEventLoop()) {
            next.invokeChannelActive();
        } else {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    next.invokeChannelActive();
                }
            });
        }
        return this;
    }

    @Override
    public ChannelPipeline fireChannelInactive() {
        final DuplexHeadContext next = (DuplexHeadContext) head;
        EventExecutor executor = next.readExecutor();
        if (executor.inEventLoop()) {
            next.invokeChannelInactive();
        } else {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    next.invokeChannelInactive();
                }
            });
        }
        return this;
    }

    @Override
    public ChannelPipeline fireChannelRead(Object msg) {
        final DuplexHeadContext next = (DuplexHeadContext) head;
        final Object m = touch(ObjectUtil.checkNotNull(msg, "msg"), next);
        EventExecutor executor = next.readExecutor();
        if (executor.inEventLoop()) {
            next.invokeChannelRead(m);
        } else {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    next.invokeChannelRead(m);
                }
            });
        }
        return this;
    }

    @Override
    public ChannelPipeline fireChannelReadComplete() {
        final DuplexHeadContext next = (DuplexHeadContext) head;
        EventExecutor executor = next.readExecutor();
        if (executor.inEventLoop()) {
            next.invokeChannelReadComplete();
        } else {
            AbstractChannelHandlerContext.Tasks tasks = next.invokeTasks;
            if (tasks == null) {
                next.invokeTasks = tasks = new AbstractChannelHandlerContext.Tasks(next);
            }
            executor.execute(tasks.invokeChannelReadCompleteTask);
        }
        return this;
    }

    @Override
    public ChannelPipeline fireChannelWritabilityChanged() {
        final DuplexHeadContext next = (DuplexHeadContext) head;
        EventExecutor executor = next.writeExecutor();
        if (executor.inEventLoop()) {
            next.invokeChannelWritabilityChanged();
        } else {
            AbstractChannelHandlerContext.Tasks tasks = next.invokeTasks;
            if (tasks == null) {
                next.invokeTasks = tasks = new AbstractChannelHandlerContext.Tasks(next);
            }
            executor.execute(tasks.invokeChannelWritableStateChangedTask);
        }
        return this;
    }

    class DuplexChannelHandlerContext extends AbstractChannelHandlerContext {

        public static final int MASK_WRITE_TYPE = MASK_WRITE | MASK_FLUSH | MASK_CHANNEL_WRITABILITY_CHANGED;

        private final ChannelHandler handler;

        private int currentMask;

        DuplexChannelHandlerContext(DefaultChannelPipeline pipeline, EventExecutor executor,
                                           String name, Class<? extends ChannelHandler> handlerClass) {
            super(pipeline, executor, name, handlerClass);
            this.handler =  null;
        }

        DuplexChannelHandlerContext(DefaultChannelPipeline pipeline, EventExecutor executor,
                                           String name, ChannelHandler handler) {
            super(pipeline, executor, name, handler.getClass());
            this.handler = handler;
        }

        @Override
        public ChannelHandler handler() {
            return handler;
        }

        public EventExecutor writeExecutor() {
            return ((DuplexSocketChannel) channel()).writeEventLoop();
        }

        public EventExecutor readExecutor() {
            return super.executor();
        }

        @Override
        public AbstractChannelHandlerContext findContextInbound(int mask) {
            AbstractChannelHandlerContext next = super.findContextInbound(mask);
            if (next instanceof DuplexChannelHandlerContext) {
                ((DuplexChannelHandlerContext) next).currentMask = mask;
            }
            return next;
        }

        @Override
        public AbstractChannelHandlerContext findContextOutbound(int mask) {
            AbstractChannelHandlerContext next = super.findContextOutbound(mask);
            if (next instanceof DuplexChannelHandlerContext) {
                ((DuplexChannelHandlerContext) next).currentMask = mask;
            }
            return next;
        }

        @Override
        public EventExecutor executor() {
            EventExecutor eventExecutor = null;
            int value = currentMask == 0 ? executionMask : currentMask;
            if ((value & MASK_WRITE_TYPE) > 0) {
                eventExecutor = writeExecutor();
            }
            if (eventExecutor == null) {
                eventExecutor = super.executor();
            }
            currentMask = 0;
            return eventExecutor;
        }

        @Override
        public ChannelFuture writeAndFlush(Object msg) {
            ChannelPromise promise = new DefaultChannelPromise(channel(), writeExecutor());
            return writeAndFlush(msg, promise);
        }

        @Override
        public ChannelHandlerContext fireUserEventTriggered(final Object event) {
            ObjectUtil.checkNotNull(event, "event");
            AbstractChannelHandlerContext next = findContextInbound(MASK_USER_EVENT_TRIGGERED);
            EventExecutor executor = isWriteEvent(event) ? writeExecutor() : next.executor();
            if (executor.inEventLoop()) {
                next.invokeUserEventTriggered(event);
            } else {
                executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        next.invokeUserEventTriggered(event);
                    }
                });
            }
            return this;
        }

        public boolean isWriteEvent(Object event) {
            return event.toString().contains("WRITER_IDLE");
        }
    }

    class DuplexTailContext extends DuplexChannelHandlerContext implements ChannelInboundHandler {
        DuplexTailContext(DefaultChannelPipeline pipeline) {
            super(pipeline, null, TAIL_NAME, DuplexTailContext.class);
            setAddComplete();
        }

        @Override
        public ChannelHandler handler() {
            return this;
        }

        @Override
        public void channelRegistered(ChannelHandlerContext ctx) { }

        @Override
        public void channelUnregistered(ChannelHandlerContext ctx) { }

        @Override
        public void channelActive(ChannelHandlerContext ctx) {
            onUnhandledInboundChannelActive();
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) {
            onUnhandledInboundChannelInactive();
        }

        @Override
        public void channelWritabilityChanged(ChannelHandlerContext ctx) {
            onUnhandledChannelWritabilityChanged();
        }

        @Override
        public void handlerAdded(ChannelHandlerContext ctx) { }

        @Override
        public void handlerRemoved(ChannelHandlerContext ctx) { }

        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
            onUnhandledInboundUserEventTriggered(evt);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            onUnhandledInboundException(cause);
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) {
            onUnhandledInboundMessage(ctx, msg);
        }

        @Override
        public void channelReadComplete(ChannelHandlerContext ctx) {
            onUnhandledInboundChannelReadComplete();
        }
    }

    class DuplexHeadContext extends DuplexChannelHandlerContext
            implements ChannelOutboundHandler, ChannelInboundHandler {

        private final Channel.Unsafe unsafe;

        DuplexHeadContext(DefaultChannelPipeline pipeline) {
            super(pipeline, null, HEAD_NAME, DuplexHeadContext.class);
            unsafe = pipeline.channel().unsafe();
            setAddComplete();
        }

        @Override
        public ChannelHandler handler() {
            return this;
        }

        @Override
        public void handlerAdded(ChannelHandlerContext ctx) {
            // NOOP
        }

        @Override
        public void handlerRemoved(ChannelHandlerContext ctx) {
            // NOOP
        }

        @Override
        public void bind(ChannelHandlerContext ctx, SocketAddress localAddress, ChannelPromise promise) {
            unsafe.bind(localAddress, promise);
        }

        @Override
        public void connect(ChannelHandlerContext ctx, SocketAddress remoteAddress,
                            SocketAddress localAddress, ChannelPromise promise) {
            unsafe.connect(remoteAddress, localAddress, promise);
        }

        @Override
        public void disconnect(ChannelHandlerContext ctx, ChannelPromise promise) {
            unsafe.disconnect(promise);
        }

        @Override
        public void close(ChannelHandlerContext ctx, ChannelPromise promise) {
            unsafe.close(promise);
        }

        @Override
        public void deregister(ChannelHandlerContext ctx, ChannelPromise promise) {
            unsafe.deregister(promise);
        }

        @Override
        public void read(ChannelHandlerContext ctx) {
            unsafe.beginRead();
        }

        @Override
        public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
            unsafe.write(msg, promise);
        }

        @Override
        public void flush(ChannelHandlerContext ctx) {
            unsafe.flush();
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            ctx.fireExceptionCaught(cause);
        }

        @Override
        public void channelRegistered(ChannelHandlerContext ctx) {
            invokeHandlerAddedIfNeeded();
            ctx.fireChannelRegistered();
        }

        @Override
        public void channelUnregistered(ChannelHandlerContext ctx) {
            ctx.fireChannelUnregistered();

            // Remove all handlers sequentially if channel is closed and unregistered.
            if (!channel.isOpen()) {
                destroy();
            }
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) {
            ctx.fireChannelActive();

            readIfIsAutoRead();
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) {
            ctx.fireChannelInactive();
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) {
            ctx.fireChannelRead(msg);
        }

        @Override
        public void channelReadComplete(ChannelHandlerContext ctx) {
            ctx.fireChannelReadComplete();

            readIfIsAutoRead();
        }

        private void readIfIsAutoRead() {
            if (channel.config().isAutoRead()) {
                channel.read();
            }
        }

        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
            ctx.fireUserEventTriggered(evt);
        }

        @Override
        public void channelWritabilityChanged(ChannelHandlerContext ctx) {
            ctx.fireChannelWritabilityChanged();
        }
    }
}
