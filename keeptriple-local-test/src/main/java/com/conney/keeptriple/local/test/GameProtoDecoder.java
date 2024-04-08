package com.conney.keeptriple.local.test;

import com.conney.keeptriple.local.net.codec.ProtoDecoder;
import com.conney.keeptriple.local.net.exception.ProtoDecodeException;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@ChannelHandler.Sharable
public class GameProtoDecoder extends ProtoDecoder {

    private static final Logger logger = LoggerFactory.getLogger(GameProtoDecoder.class);

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        Object decoded = decode(ctx, in);

        if (decoded != null) {
            out.add(decoded);
        }
    }

    @Override
    public boolean check(ByteBuf in) throws ProtoDecodeException {
        if (in.readableBytes() < GameProto.HEADER_FF_LENGTH + 1) {
            throw new ProtoDecodeException("Readable bytes cannot be below minimum(" + (GameProto.HEADER_FF_LENGTH + 1) + ")");
        }

        if (getMagic(in) != GameProto.MAGIC) {
            throw new ProtoDecodeException("Unknown magic");
        }

        return true;
    }

    private int getMagic(ByteBuf in) {
        return in.getByte(0) & 0xF;
    }

    @Override
    public byte getCmd(ByteBuf in) {
        return in.getByte(GameProto.HEADER_CMD_POS);
    }
}
