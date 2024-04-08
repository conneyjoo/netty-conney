package com.conney.keeptriple.local.test;

import com.conney.keeptriple.local.net.annotation.ProtoDefine;
import io.netty.buffer.ByteBuf;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

@ProtoDefine
public class TestData extends GameProto {

    public static final byte[] B128 = new byte[128 * 1];

    public static final byte[] KB1 = new byte[1028 * 1];

    public static final byte[] KB4 = new byte[1028 * 4];

    public static final byte[] KB16 = new byte[1028 * 16];

    public static final byte[] M1 = new byte[1028 * 1024];

    static {
        Arrays.fill(B128, (byte) 127);

        Arrays.fill(KB1, (byte) 127);
        Arrays.fill(KB4, (byte) 127);
        Arrays.fill(KB16, (byte) 127);

        Arrays.fill(M1, (byte) 127);
    }

    private byte[] data;

    @Override
    public void writeBody(ByteBuf buf) {
        buf.writeInt(data.length);
        buf.writeBytes(data);
    }

    @Override
    public void readBody(ByteBuf buf) {
        data = readBytes(buf, buf.readInt());
    }

    @Override
    public int bodySize() {
        return 4 + data.length;
    }

    @Override
    public byte getCmd() {
        return 0x7F;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public static void main(String[] args) throws Exception {
        byte[] data = new byte[1024 * 1024 * 8];
        TestData testData = new TestData();
        testData.data = data;
        ByteBuf buf = testData.encode();
        byte[] bytes = new byte[buf.readableBytes()];
        buf.readBytes(bytes);
        Files.write(Paths.get("/root/tmp/8mb"), bytes);
    }
}
