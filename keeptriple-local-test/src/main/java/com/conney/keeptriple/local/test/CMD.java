package com.conney.keeptriple.local.test;

public class CMD {

    /** 客户端 -> 服务端心跳指令 */
    public static final byte HEART_BEAT = (byte) 0x1;

    /** 认证指令 */
    public static final byte AUTHORIZATION_REQUEST = (byte) 0x2;
    public static final byte AUTHORIZATION_RESPONSE = toResponse(AUTHORIZATION_REQUEST);

    /** 开始对局指令 */
    public static final byte START_REQUEST = (byte) 0x3;
    public static final byte START_RESPONSE = toResponse(START_REQUEST);

    /** 继续游戏指令 */
    public static final byte RESUME_REQUEST = (byte) 0x4;
    public static final byte RESUME_RESPONSE = toResponse(RESUME_REQUEST);

    /** 步骤指令 */
    public static final byte STEP_REQUEST = (byte) 0x5;
    public static final byte STEP_RESPONSE = toResponse(STEP_REQUEST);

    /** 消息指令 */
    public static final byte MESSAGE = (byte) 0x7;

    /** 服务端 -> 客户端返回错误信息 */
    public static final byte ERROR_MESSAGE = (byte) 0x8B;

    /** 客户端上报日志指令 */
    public static final byte REPORT_LOG = (byte) 0x14;

    /** echo指令 */
    public static final byte ECHO = (byte) 0x32;

    /** 测试数据指令 */
    public static final byte TEST = (byte) 0x64;

    /** to指令从127开始 */
    private static final byte RESPONSE_START = 0x7F;

    /** from指令换to指令 */
    private static byte toResponse(byte from) {
        return (byte) (RESPONSE_START + from);
    }
}
