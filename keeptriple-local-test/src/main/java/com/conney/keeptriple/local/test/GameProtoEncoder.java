package com.conney.keeptriple.local.test;

import com.conney.keeptriple.local.net.codec.ProtoEncoder;
import com.conney.keeptriple.local.net.proto.Proto;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;

@ChannelHandler.Sharable
public class GameProtoEncoder extends ProtoEncoder {

    @Override
    protected void encode(ChannelHandlerContext ctx, Proto proto, ByteBuf out) throws Exception {
        super.encode(ctx, proto, out);
    }
}
