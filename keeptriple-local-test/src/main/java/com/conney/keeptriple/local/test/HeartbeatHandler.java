package com.conney.keeptriple.local.test;

import com.conney.keeptriple.local.net.annotation.ProtoHandler;
import com.conney.keeptriple.local.net.handler.AbstractHandler;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ProtoHandler
public class HeartbeatHandler extends AbstractHandler<Heartbeat> {

    private static final Logger logger = LoggerFactory.getLogger(HeartbeatHandler.class);

    @Override
    public void handle(ChannelHandlerContext ctx, Heartbeat heartbeat) {
        logger.info("Receive heartbeat from client({})", ctx.channel().remoteAddress());
    }
}
