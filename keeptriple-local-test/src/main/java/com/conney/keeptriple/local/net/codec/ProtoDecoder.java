package com.conney.keeptriple.local.net.codec;

import com.conney.keeptriple.local.net.exception.ProtoDecodeException;
import com.conney.keeptriple.local.net.proto.Proto;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public abstract class ProtoDecoder extends ByteToMessageDecoder {

    private static final Logger logger = LoggerFactory.getLogger(ProtoDecoder.class);

    protected List<Proto> protocols;

    public abstract boolean check(ByteBuf in) throws ProtoDecodeException;

    public abstract byte getCmd(ByteBuf in);

    public Proto decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
        try {
            if (check(in)) {
                byte cmd = getCmd(in);
                return decode(in, cmd);
            }

            return null;
        } catch (ProtoDecodeException e) {
            if (logger.isDebugEnabled()) {
                logger.debug("{} - {}", e.getMessage(), ctx.channel().remoteAddress().toString());
            }
            throw e;
        } catch (Throwable e) {
            logger.error("{} - {}", e.getMessage(), ctx.channel().remoteAddress().toString(), e);
            throw e;
        } finally {
            in.clear();
        }
    }

    public Proto decode(ByteBuf in, byte cmd) {
        try {
            for (Proto proto : protocols) {
                if (proto.support(cmd)) {
                    proto = proto.getClass().newInstance().decode(in);
                    in.skipBytes(in.readableBytes());
                    return proto;
                }
            }
        } catch (Exception e) {
            logger.error("Proto decode error. [cmd={}]", Integer.toHexString(cmd), e);
            return null;
        }

        logger.warn("Unknown cmd[{}]", cmd);

        return null;
    }

    @Override
    protected void ensureNotSharable() {
    }

    public void setProtocols(List<Proto> protocols) {
        this.protocols = protocols;
    }
}
