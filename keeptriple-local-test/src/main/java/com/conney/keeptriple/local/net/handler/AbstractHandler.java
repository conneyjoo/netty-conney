package com.conney.keeptriple.local.net.handler;

import com.conney.keeptriple.local.net.initializer.NettyInitializer;
import com.conney.keeptriple.local.net.listener.HandlerListener;
import com.conney.keeptriple.local.net.session.ClientSessionManager;
import io.netty.channel.ChannelHandlerContext;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;

public abstract class AbstractHandler<T extends Serializable> implements Handler<T> {

    private boolean blocking = false;

    private List<HandlerListener<T>> listeners = new LinkedList<>();

    private ClientSessionManager sessionManager;

    private NettyInitializer nettyInitializer;

    private ExecutorService handleExecutor;

    @Override
    public void afterPropertiesSet() {
    }

    @Override
    public void process(ChannelHandlerContext ctx, T t) {
        if (isBlocking()) {
            doHandle(ctx, t);
        } else {
            handleExecutor.submit(() -> doHandle(ctx, t));
        }
    }

    private void doHandle(ChannelHandlerContext ctx, T t) {
        fireBeforeHandlerEvent(ctx, t);
        handle(ctx, t);
        fireHandlerEvent(ctx, t);
    }

    @Override
    public void addListener(HandlerListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeListener(HandlerListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void fireBeforeHandlerEvent(ChannelHandlerContext ctx, T t) {
        for (HandlerListener listener : listeners) {
            listener.beforeHandle(ctx, t);
        }
    }

    @Override
    public void fireHandlerEvent(ChannelHandlerContext ctx, T t) {
        for (HandlerListener listener : listeners) {
            listener.handle(ctx, t);
        }
    }

    @Override
    public boolean isBlocking() {
        return blocking;
    }

    @Override
    public void setBlocking(boolean blocking) {
        this.blocking = blocking;
    }

    @Override
    public ClientSessionManager getSessionManager() {
        return sessionManager;
    }

    @Override
    public void setSessionManager(ClientSessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    @Override
    public NettyInitializer getNettyInitializer() {
        return nettyInitializer;
    }

    @Override
    public void setNettyInitializer(NettyInitializer nettyInitializer) {
        this.nettyInitializer = nettyInitializer;
    }

    @Override
    public void setHandleExecutor(ExecutorService handleExecutor) {
        this.handleExecutor = handleExecutor;
    }
}
