package com.conney.keeptriple.local.net;

import com.conney.keeptriple.local.net.initializer.NettyInitializer;
import com.conney.keeptriple.local.util.ThreadPoolUtils;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import java.util.concurrent.atomic.AtomicBoolean;

public abstract class NettyServer extends NettyInitializer {

    private static final Logger logger = LoggerFactory.getLogger(NettyServer.class);

    public static final int READER_IDLE_TIME = NettyClient.DEFAULT_WRITER_IDLE_TIME * 3;

    private AtomicBoolean running = new AtomicBoolean(false);
    private ServerBootstrap bootstrap;
    private int bossThreadNum = 0;
    private int workThreadNum = 0;

    public NettyServer(ApplicationContext springContent) {
        super(springContent);
    }

    public NettyServer(ApplicationContext springContent, int bossThreadNum, int workThreadNum) {
        super(springContent);
        this.bossThreadNum = bossThreadNum;
        this.workThreadNum = workThreadNum;
    }

    public void start(int port) {
        if (running.compareAndSet(false, true)) {
            String name = getName();
            EventLoopGroup bossGroup = new NioEventLoopGroup();
            EventLoopGroup workerGroup = new NioEventLoopGroup();

            bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(this);

            initChannel(bootstrap);
            ChannelFuture channelFuture = bootstrap.bind(port);

            logger.info("Server({}) listen port: {}", getName(), port);

            if (!channelFuture.awaitUninterruptibly().isSuccess()) {
                if (channelFuture.channel().isOpen())
                    channelFuture.channel().close();

                Throwable failedChannelCause = channelFuture.cause();
                throw new RuntimeException(failedChannelCause);
            }

            logger.info("Server({}) start success {}", getName(), channelFuture.channel().localAddress().toString());
        } else {
            logger.info("Server({}) already started", getName());
        }
    }

    public void initChannel(ServerBootstrap b) {
    }

    @Override
    public void shutdownGracefully() {
        logger.info("Shutdown netty {} ...", getName());

        if (!isRunning() || bootstrap == null) {
            logger.warn("Netty server has not been started");
            return;
        }

        ThreadPoolUtils.shutdownGraceful(getHandleExecutor());

        EventLoopGroup bossGroup = bootstrap.config().group();
        EventLoopGroup workerGroup = bootstrap.config().childGroup();

        try {
            bossGroup.shutdownGracefully().sync();
            workerGroup.shutdownGracefully().sync();
        } catch (InterruptedException e) {
            logger.error("Shutdown terminate netty: {}", e.getMessage(), e);
        }

        logger.info("Shutdown netty {} finish.", getName());
    }

    public boolean isRunning() {
        return running.get();
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
}