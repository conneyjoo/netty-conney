package com.conney.keeptriple.local.net.proto;

import io.netty.buffer.ByteBuf;

import java.io.Serializable;

public interface IProto extends Serializable {

    boolean support(byte cmd);

    byte getCmd();

    ByteBuf encode();

    IProto decode(ByteBuf buf);
}
