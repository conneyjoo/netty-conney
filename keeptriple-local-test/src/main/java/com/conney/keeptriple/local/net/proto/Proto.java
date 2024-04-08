package com.conney.keeptriple.local.net.proto;

import io.netty.buffer.ByteBuf;

import java.io.Serializable;

public interface Proto extends Serializable {

    /**
     * 是否支持该指令
     *
     * @param cmd
     * @return
     */
    boolean support(byte cmd);

    /**
     * 获取指令
     *
     * @return
     */
    byte getCmd();

    /**
     * 设置序列号
     *
     * @param serial
     * @return
     */
    long serial(long serial);

    /**
     * 获取序列号
     *
     * @return
     */
    long serial();

    /**
     * 获得协议长度
     *
     * @return
     */
    int available();

    /**
     * 协议encode
     *
     * @return
     */
    ByteBuf encode();

    /**
     * 协议encode
     *
     * @param buf
     * @return
     */
    ByteBuf encode(ByteBuf buf);

    /**
     * 协议decode
     *
     * @param buf
     * @return
     */
    Proto decode(ByteBuf buf);

    /**
     * 转换简明的字符串
     *
     * @return
     */
    String toSimpleString();

    /**
     * 是否支持序列号的
     *
     * @return
     */
    default boolean serializable() { return this instanceof Serializer; }
}
