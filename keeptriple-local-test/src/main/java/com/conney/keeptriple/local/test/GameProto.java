package com.conney.keeptriple.local.test;

import com.conney.keeptriple.local.net.proto.AbstractProto;
import io.netty.buffer.ByteBuf;

public abstract class GameProto extends AbstractProto {

    /** 协议固定部分字段长度 */
    public static final int HEADER_FF_LENGTH = 3;

    /** header中的cmd位置 */
    public static final int HEADER_CMD_POS = 2;

    /** header中的length位置 */
    public static final int HEADER_LENGTH_POS = 3;

    /** 魔术,固定4位[1001] */
    public static final byte MAGIC = (1 << 3) + 1;

    /** 数据格式,固定４位[0000] - 最高位表示是否压缩(0.不压缩\1.压缩),低3位表示加密类型(000.不加密\001.SHA-1\010.DES\011.RSA) */
    protected byte format = 0x0;

    /** 协议版本 */
    protected byte version = 0x1;

    /** 协议指令 */
    protected byte cmd;

    /** 数据长度 */
    protected Length length;

    public GameProto() {
    }

    public void writeHeader(ByteBuf buf) {
        buf.writeByte(MAGIC | (format << 4));
        buf.writeByte(version);
        buf.writeByte(getCmd());
        length.write(buf);
    }

    public void readHeader(ByteBuf buf) {
        format = (byte) ((buf.readByte() << 4) & 0xF);
        version = buf.readByte();
        cmd = buf.readByte();
        length = new Length();
        length.read(buf);
    }

    public int available() {
        if (length == null) {
            length = new Length(HEADER_FF_LENGTH + bodySize());
        }
        return length.value;
    }

    public int length() {
        return length.value;
    }

    public Length getLength() {
        return length;
    }

    /**
     * 动态长度类型
     *
     * 二进制数据格式, 长度(30) 标志(2)
     * ----------------------------------------
     * 00: 00 000000
     * 01: 01 000000 00000000
     * 10: 10 000000 00000000 00000000
     * 11: 11 000000 00000000 00000000 00000000
     * ----------------------------------------
     * 高0-1位: 长度标志
     * 低2-31位: 长度
     */
    public static class Length {

        static final int FLAG_MASK = (1 << 2) - 1;

        static final int VALUE_MASK = (1 << 30) - 1;

        /** 长度标志(2bit),总共有4种组合[00 01 10 11]分别对应[6bit 14bit 22bit 30bit] */
        byte lenOfBytes = 0;

        /** 长度值 */
        int value = 0;

        Length() {
        }

        Length(int value) {
            int l = value >> 6;
            while (l > 0) {
                this.lenOfBytes++;
                l = l >> 8;
            }
            this.value = value + 1 + this.lenOfBytes;
        }

        public void write(ByteBuf buf) {
            int length = ((lenOfBytes & FLAG_MASK) << (6 + (lenOfBytes * 8))) | ((value & VALUE_MASK));

            for (int i = lenOfBytes; i >= 0; i--) {
                buf.writeByte((length >> (i * 8)) & 0xFF);
            }
        }

        public void read(ByteBuf buf) {
            lenOfBytes = getLenOfBytes(buf.getByte(HEADER_LENGTH_POS));

            if (lenOfBytes >= 0) {
                int i = lenOfBytes;
                value |= (buf.readByte() & 0x3F) << (i-- * 8);

                for (; i >= 0; i--) {
                    value |= (buf.readByte() & 0xFF) << (i * 8);
                }
            }
        }

        public static byte getLenOfBytes(int i) {
            return (byte) ((i >> 6) & 0x3);
        }

        public static int getLengthOfValue(int bytes, int i) {
            return i & ((1 << (bytes * 8) - 2) - 1);
        }

        public byte lenOfBytes() {
            return lenOfBytes;
        }
    }
}
