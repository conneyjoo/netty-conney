package com.conney.keeptriple.local.net.address;

import io.netty.channel.ChannelHandlerContext;

import java.net.InetAddress;
import java.net.InetSocketAddress;

public class ReadWriteAddress implements java.io.Serializable {

    private static final long serialVersionUID = 4215720748342549866L;

    private int type;

    private InetSocketAddress socketAddress;

    private ChannelHandlerContext attachCtx;

    public ReadWriteAddress(String hostname, int port) {
        this.socketAddress = new InetSocketAddress(hostname, port);
        this.type = 0;
    }

    public ReadWriteAddress(String hostname, int port, int type, ChannelHandlerContext attachCtx) {
        this.socketAddress = new InetSocketAddress(hostname, port);
        this.type = type;
        this.attachCtx = attachCtx;
    }

    public final int getPort() {
        return socketAddress.getPort();
    }

    public final InetAddress getAddress() {
        return socketAddress.getAddress();
    }

    public final String getHostName() {
        return socketAddress.getHostName();
    }

    public final String getHostString() {
        return socketAddress.getHostString();
    }

    public final boolean isUnresolved() {
        return socketAddress.isUnresolved();
    }

    public InetSocketAddress getSocketAddress() {
        return socketAddress;
    }

    public ChannelHandlerContext pollAttachCtx() {
        ChannelHandlerContext ctx = attachCtx;
        attachCtx = null;
        return ctx;
    }

    public ReadWriteAddress setType(int type) {
        this.type = type;
        return this;
    }

    public int getType() {
        return type;
    }

    public boolean isRead() {
        return type > 0;
    }

    public boolean isWrite() {
        return type == 0;
    }

    public String getTypeName() {
        return isWrite() ? "write" : "read";
    }

    @Override
    public boolean equals(Object o) {
        return socketAddress.equals(o) && type == ((ReadWriteAddress) o).type;
    }

    @Override
    public int hashCode() {
        int result = socketAddress.hashCode();
        result = 31 * result + type;
        return result;
    }
}
