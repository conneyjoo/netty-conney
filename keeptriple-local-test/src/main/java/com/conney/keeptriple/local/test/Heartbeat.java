package com.conney.keeptriple.local.test;

import com.conney.keeptriple.local.net.annotation.ProtoDefine;
import io.netty.buffer.ByteBuf;

@ProtoDefine
public class Heartbeat extends GameProto {

    public Heartbeat() {
    }

    @Override
    public int bodySize() {
        return 0;
    }

    @Override
    public byte getCmd() {
        return CMD.HEART_BEAT;
    }

    @Override
    public void writeBody(ByteBuf buf) {
    }

    @Override
    public void readBody(ByteBuf buf) {
    }
}
