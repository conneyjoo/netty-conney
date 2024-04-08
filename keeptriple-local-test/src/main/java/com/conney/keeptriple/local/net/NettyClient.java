package com.conney.keeptriple.local.net;

import com.conney.keeptriple.local.net.initializer.NettyInitializer;
import com.conney.keeptriple.local.util.ThreadPoolUtils;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.pool.ChannelPoolHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import java.util.concurrent.TimeUnit;

public abstract class NettyClient extends NettyInitializer implements ChannelPoolHandler {

    private static final Logger logger = LoggerFactory.getLogger(NettyClient.class);

    public static final int DEFAULT_WRITER_IDLE_TIME = 30;

    public NettyClient() {
    }

    public NettyClient(ApplicationContext springContent) {
        super(springContent);
    }

    @Override
    public IdleStateHandler getIdleStateHandler() {
        return new IdleStateHandler(0, DEFAULT_WRITER_IDLE_TIME, 0, TimeUnit.SECONDS);
    }

    @Override
    public void accept(ChannelHandlerContext ctx) {
    }

    @Override
    public void heartbeat(ChannelHandlerContext ctx) {
    }

    @Override
    public void close(ChannelHandlerContext ctx) {
    }

    @Override
    public void channelReleased(Channel ch) throws Exception {
        logger.trace("channelReleased. Channel ID: {}", ch.id());
    }

    @Override
    public void channelAcquired(Channel ch) throws Exception {
        logger.trace("channelAcquired. Channel ID: {}", ch.id());
    }

    @Override
    public void channelCreated(Channel ch) throws Exception {
        logger.trace("channelCreated. Channel ID: {}", ch.id());

        SocketChannel channel = (SocketChannel) ch;
        initChannel(channel);
    }

    @Override
    public void shutdownGracefully() {
        logger.info("Shutdown netty {} ...", getName());
        ThreadPoolUtils.shutdownGraceful(getHandleExecutor());
        logger.info("Shutdown netty {} ...", getName());
    }
}