package com.conney.keeptriple.local.net;

import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.channel.epoll.*;
import io.netty.channel.kqueue.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.DefaultThreadFactory;

/**
 * netty启动配置的上下文
 * 根据操作系统的网络模型支持选择socket文件事件
 * linux2.6以上使用epoll
 * 其他操作系统使用selector
 */
public class NettyContext {

    private static final NettyContext INSTANCE = new NettyContext();

    private final Class<? extends ServerChannel> serverChannelClass;
    private final Class<? extends Channel> channelClass;
    private final Class<? extends DatagramChannel> datagramChannelClass;

    private NettyContext() {
        super();

        if (Epoll.isAvailable()) {
            serverChannelClass = EpollServerSocketChannel.class;
            channelClass = EpollSocketChannel.class;
            datagramChannelClass = EpollDatagramChannel.class;
        } else if (KQueue.isAvailable()) {
            serverChannelClass = KQueueServerSocketChannel.class;
            channelClass = KQueueSocketChannel.class;
            datagramChannelClass = KQueueDatagramChannel.class;
        } else {
            serverChannelClass = NioServerSocketChannel.class;
            channelClass = NioSocketChannel.class;
            datagramChannelClass = NioDatagramChannel.class;
        }
    }

    public static final NettyContext get() {
        return INSTANCE;
    }

    public Class<? extends ServerChannel> getServerChannelClass() {
        return serverChannelClass;
    }

    public Class<? extends Channel> getChannelClass() {
        return channelClass;
    }

    public Class<? extends DatagramChannel> getDatagramChannelClass() {
        return datagramChannelClass;
    }

    public EventLoopGroup getEventLoopGroup() {
        if (Epoll.isAvailable()) {
            return new EpollEventLoopGroup();
        } else if (KQueue.isAvailable()) {
            return new KQueueEventLoopGroup();
        } else {
            return new NioEventLoopGroup();
        }
    }

    public EventLoopGroup getEventLoopGroup(int nThreads) {
        if (Epoll.isAvailable()) {
            return new EpollEventLoopGroup(nThreads);
        } else if (KQueue.isAvailable()) {
            return new KQueueEventLoopGroup(nThreads);
        } else {
            return new NioEventLoopGroup(nThreads);
        }
    }

    public EventLoopGroup getEventLoopGroup(String groupName) {
        boolean hasName = groupName == null;
        if (Epoll.isAvailable()) {
            return hasName ? new EpollEventLoopGroup() : new EpollEventLoopGroup(new DefaultThreadFactory(groupName));
        } else if (KQueue.isAvailable()) {
            return hasName ? new KQueueEventLoopGroup() : new KQueueEventLoopGroup(new DefaultThreadFactory(groupName));
        } else {
            return hasName ? new NioEventLoopGroup() : new NioEventLoopGroup(new DefaultThreadFactory(groupName));
        }
    }

    public EventLoopGroup getEventLoopGroup(int nThreads, String groupName) {
        boolean hasName = groupName == null;
        if (Epoll.isAvailable()) {
            return hasName ? new EpollEventLoopGroup(nThreads) : new EpollEventLoopGroup(nThreads, new DefaultThreadFactory(groupName));
        } else if (KQueue.isAvailable()) {
            return hasName ? new KQueueEventLoopGroup(nThreads) : new KQueueEventLoopGroup(nThreads, new DefaultThreadFactory(groupName));
        } else {
            return hasName ? new NioEventLoopGroup(nThreads) : new NioEventLoopGroup(nThreads, new DefaultThreadFactory(groupName));
        }
    }
}
