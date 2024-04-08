package com.conney.keeptriple.local.net.handler;

import com.conney.keeptriple.local.bean.Preset;
import com.conney.keeptriple.local.net.initializer.NettyInitializer;
import com.conney.keeptriple.local.net.listener.HandlerListener;
import com.conney.keeptriple.local.net.session.ClientSessionManager;
import io.netty.channel.ChannelHandlerContext;

import java.io.Serializable;
import java.util.concurrent.ExecutorService;

public interface Handler<T extends Serializable> extends Preset {

    void afterPropertiesSet();

    void process(ChannelHandlerContext ctx, T t);

    void handle(ChannelHandlerContext ctx, T t);

    void addListener(HandlerListener listener);

    void removeListener(HandlerListener listener);

    void fireBeforeHandlerEvent(ChannelHandlerContext ctx, T t);

    void fireHandlerEvent(ChannelHandlerContext ctx, T t);

    boolean isBlocking();

    void setBlocking(boolean blocking);

    ClientSessionManager getSessionManager();

    void setSessionManager(ClientSessionManager sessionManager);

    NettyInitializer getNettyInitializer();

    void setNettyInitializer(NettyInitializer nettyInitializer);

    void setHandleExecutor(ExecutorService handleExecutor);
}
