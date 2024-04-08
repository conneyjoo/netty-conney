package com.conney.keeptriple.local.net.handler;

import io.netty.channel.ChannelHandlerContext;

public interface SocketEventHandler {

    void accept(ChannelHandlerContext ctx);

    void heartbeat(ChannelHandlerContext ctx);

    void close(ChannelHandlerContext ctx);
}
