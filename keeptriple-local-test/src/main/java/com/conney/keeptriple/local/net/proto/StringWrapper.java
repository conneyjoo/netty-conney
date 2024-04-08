package com.conney.keeptriple.local.net.proto;

import io.netty.util.CharsetUtil;

import java.nio.charset.Charset;

public class StringWrapper {

    public static final StringWrapper EMPTY = new StringWrapper("");

    private static final Charset DEFAULT_CHARSET = CharsetUtil.UTF_8;

    private String value;
    private byte[] bytes;

    public StringWrapper(byte[] bytes) {
        this.bytes = bytes;
        this.value = new String(bytes);
    }

    public StringWrapper(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public byte[] getBytes() {
        if (bytes == null) {
            bytes = value.getBytes(DEFAULT_CHARSET);
        }
        return bytes;
    }

    public void setBytes(byte[] bytes) {
        this.bytes = bytes;
    }

    public int length() {
        return getBytes().length + 1;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        StringWrapper s = (StringWrapper) o;
        return value.equals(s.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return value;
    }
}
