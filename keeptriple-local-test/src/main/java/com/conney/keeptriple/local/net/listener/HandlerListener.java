package com.conney.keeptriple.local.net.listener;

import io.netty.channel.ChannelHandlerContext;

import java.io.Serializable;

public interface HandlerListener<T extends Serializable> {

    void beforeHandle(ChannelHandlerContext ctx, T t);

    void handle(ChannelHandlerContext ctx, T t);
}
