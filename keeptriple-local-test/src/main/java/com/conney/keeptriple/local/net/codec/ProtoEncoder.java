package com.conney.keeptriple.local.net.codec;

import com.conney.keeptriple.local.net.proto.Proto;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ProtoEncoder extends MessageToByteEncoder<Proto> {

    private static final Logger logger = LoggerFactory.getLogger(ProtoEncoder.class);

    @Override
    protected void encode(ChannelHandlerContext ctx, Proto proto, ByteBuf out) throws Exception {
        ByteBuf buf = null;

        try {
            buf = ctx.alloc().buffer(proto.available());
            proto.encode(buf);
            //String s = AbstractProto.toByteString(10, buf, proto.getClass());
            //System.out.println(s);
            out.writeBytes(buf);
        } catch (Throwable e) {
            logger.error(e.getMessage(), e);
            throw e;
        } finally {
            if (buf != null) {
                buf.release();
            }
        }
    }
}
