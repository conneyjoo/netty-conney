package com.conney.keeptriple.local.net.session;

import com.conney.keeptriple.local.net.channel.ReactChannelPromise;
import com.conney.keeptriple.local.net.channel.SerialChannelPromise;
import com.conney.keeptriple.local.net.proto.Proto;
import com.conney.keeptriple.local.util.LoopChooser;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class ClientSession extends Session {

    private static final Logger logger = LoggerFactory.getLogger(ClientSession.class);

    public static final int DEFAULT_MAX_CONTEXT_SIZE = Short.MAX_VALUE;

    private Set<ChannelHandlerContext> contexts = new LinkedHashSet<>();

    private int maxContextSize = DEFAULT_MAX_CONTEXT_SIZE;

    private LoopChooser<ChannelHandlerContext> loopChooser = new LoopChooser<>();

    private volatile boolean closed;

    private AtomicInteger hbTimes = new AtomicInteger(0);

    public ClientSession(String id, SessionManager manager) {
        super(id, manager);
    }

    public ClientSession(String id, ChannelHandlerContext ctx, SessionManager manager) {
        super(id, manager);
        addContext(ctx);
    }

    public synchronized void close(ChannelHandlerContext ctx) {
        if (!isClosed()) {
            ctx.close();
            contexts.remove(ctx);
            loopChooser.setArray(contexts.toArray(new ChannelHandlerContext[]{}));

            if (contexts.size() == 0) {
                closed = true;
                expire();
            }
        }
    }

    public synchronized void close() {
        if (!isClosed()) {
            ChannelHandlerContext ctx;
            for (Iterator<ChannelHandlerContext> iterator = contexts.iterator(); iterator.hasNext(); ) {
                ctx = iterator.next();
                ctx.close();
            }

            contexts.clear();
            loopChooser.setArray(contexts.toArray(new ChannelHandlerContext[]{}));
            closed = true;
            expire();
        }
    }

    public synchronized ChannelHandlerContext findCtx(String address) {
        return contexts.stream().filter(c -> c.channel().remoteAddress().toString().equals(address)).findFirst().orElse(null);
    }

    public synchronized void addContext(ChannelHandlerContext ctx) {
        if (contexts.contains(ctx)) {
            return;
        }

        contexts.add(ctx);

        if (contexts.size() > maxContextSize) {
            ChannelHandlerContext context = contexts.iterator().next();
            logger.debug("Remove ctx({}) in session({}), current contexts exceed max context size {}", getId(), context.channel().remoteAddress(), maxContextSize);
            close(context);
        }

        loopChooser.setArray(contexts.toArray(new ChannelHandlerContext[]{}));
        closed = false;
    }

    public synchronized void removeContext(ChannelHandlerContext ctx) {
        if (!contexts.contains(ctx)) {
            return;
        }

        contexts.remove(ctx);
        loopChooser.setArray(contexts.toArray(new ChannelHandlerContext[]{}));
    }

    public <T extends Proto> T sendAndRecv(T t) {
        return sendAndRecv(t, SerialChannelPromise.DEFAULT_RECV_TIMEOUT);
    }

    public boolean send(Proto...protos) {
        ChannelHandlerContext ctx = loopChooser.choose();

        if (ctx == null || protos.length == 0) {
            return false;
        }

        if (protos.length == 1) {
            ctx.writeAndFlush(protos[0]);
        } else {
            for (Proto proto : protos) {
                ctx.write(proto);
            }
            ctx.flush();
        }

        return true;
    }

    public ChannelHandlerContext channel() {
        return loopChooser.choose();
    }

    public <T extends Proto> T sendAndRecv(T t, long timeout) {
        ChannelHandlerContext context = loopChooser.choose();
        SerialChannelPromise<T> promise = new SerialChannelPromise<>(context.channel(), context.executor());
        return promise.writeAndFlush(t, timeout);
    }

    public <T extends Proto> ReactChannelPromise<T> send(T t, ReactChannelPromise.Callback<T> callback) {
        ChannelHandlerContext context = loopChooser.choose();
        ReactChannelPromise<T> promise = new ReactChannelPromise<>(context.channel(), context.executor(), callback);
        promise.writeAndFlush(t);
        return promise;
    }

    public void sendAll(Proto proto) {
        Set<ChannelHandlerContext> contexts = getContexts();

        for (ChannelHandlerContext context : contexts) {
            context.writeAndFlush(proto);
        }
    }

    public boolean isClosed() {
        return closed;
    }

    public String getClientIp() {
        return getRemoteAddress().getAddress().getHostAddress();
    }

    public InetSocketAddress getRemoteAddress() {
        if (contexts.size() == 0) {
            throw new IllegalStateException("Session is already closed.");
        }

        return (InetSocketAddress) contexts.iterator().next().channel().remoteAddress();
    }

    public Set<ChannelHandlerContext> getContexts() {
        return contexts;
    }

    public void setMaxContextSize(int maxContextSize) {
        this.maxContextSize = maxContextSize;
    }

    public int heartbeat() {
        return hbTimes.incrementAndGet();
    }

    @Override
    public String toString() {
        return String.format("ClientSession {hbTimes = %d, isClosed = %b, context size = %d}", hbTimes.get(), closed, contexts.size());
    }
}
