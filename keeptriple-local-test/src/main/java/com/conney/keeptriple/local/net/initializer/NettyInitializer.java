package com.conney.keeptriple.local.net.initializer;

import com.conney.keeptriple.local.bean.Preset;
import com.conney.keeptriple.local.net.NettyContext;
import com.conney.keeptriple.local.net.codec.ProtoDecoder;
import com.conney.keeptriple.local.net.codec.ProtoEncoder;
import com.conney.keeptriple.local.net.handler.Dispatcher;
import com.conney.keeptriple.local.net.handler.NettyHandler;
import com.conney.keeptriple.local.net.handler.SocketEventHandler;
import com.conney.keeptriple.local.net.proto.Proto;
import com.conney.keeptriple.local.net.session.ClientSessionManager;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.timeout.IdleStateHandler;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;

public abstract class NettyInitializer extends ChannelInitializer<Channel> implements SocketEventHandler, Preset, InitializingBean {

    protected ApplicationContext springContent;

    protected ClientSessionManager sessionManager;
    protected NettyContext nettyContext;
    protected Dispatcher dispatcher;
    protected NettyHandler handler;

    protected ThreadLocal<ProtoDecoder> decoders = ThreadLocal.withInitial(() -> createProtoDecode());
    protected ProtoEncoder encoder;

    public NettyInitializer() {
    }

    public NettyInitializer(ApplicationContext springContent) {
        this.springContent = springContent;
    }

    @Override
    public void afterPropertiesSet() {
        if (springContent == null) {
            throw new IllegalArgumentException("Spring content not be null");
        }

        this.sessionManager = createSessionManager();
        this.dispatcher = new Dispatcher(this);
        this.encoder = getEncoder();
        this.handler = getNettyHandler();

        this.dispatcher.afterPropertiesSet();
    }

    @Override
    protected void initChannel(Channel ch) throws Exception {
        initPipeline(ch);
    }

    public void initPipeline(Channel ch) {
        addPipeline(getIdleStateHandler(), ch);
        addPipeline(getLengthFieldBasedFrameDecoder(), ch);
        addPipeline(decoders.get(), ch);
        addPipeline(encoder, ch);
        addPipeline(handler, ch);
    }

    public void addPipeline(ChannelHandler handler, Channel... chs) {
        if (handler != null) {
            for (Channel ch : chs) {
                ch.pipeline().addLast(handler);
            }
        }
    }

    public void addFirstPipeline(ChannelHandler handler, Channel... chs) {
        if (handler != null) {
            for (Channel ch : chs) {
                ch.pipeline().addFirst(handler);
            }
        }
    }

    public ProtoDecoder createProtoDecode() {
        ProtoDecoder decoder = getDecoder();
        List<Proto> protocols = new ArrayList<>();

        dispatcher.getProtocols().stream().forEach((e) -> {
            if (isProtoType(e.getValue())) {
                protocols.add(e.getValue());
            }
        });

        decoder.setProtocols(protocols);
        return decoder;
    }

    public synchronized ClientSessionManager createSessionManager() {
        return sessionManager == null ? new ClientSessionManager() : sessionManager;
    }

    public abstract String getName();

    public boolean isProtoType(Proto proto) {
        return true;
    }

    public abstract IdleStateHandler getIdleStateHandler();

    public abstract ByteToMessageDecoder getLengthFieldBasedFrameDecoder();

    public abstract ProtoEncoder getEncoder();

    public abstract ProtoDecoder getDecoder();

    public abstract void shutdownGracefully();

    public Dispatcher getDispatcher() {
        return dispatcher;
    }

    public void setSpringContent(ApplicationContext springContent) {
        this.springContent = springContent;
    }

    public ApplicationContext getSpringContent() {
        return springContent;
    }

    public NettyHandler getNettyHandler() {
        return new NettyHandler(dispatcher, this);
    }

    public synchronized NettyContext getNettyContext() {
        if (nettyContext == null) {
            nettyContext = NettyContext.get();
        }
        return nettyContext;
    }

    public ClientSessionManager getSessionManager() {
        return sessionManager;
    }

    public synchronized void setSessionManager(ClientSessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    public ExecutorService getHandleExecutor() {
        return dispatcher != null ? dispatcher.getHandleExecutor() : null;
    }

    public LinkedBlockingQueue<Runnable> getHandleQueue() {
        return dispatcher.getHandleQueue();
    }
}