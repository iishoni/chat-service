package com.iishoni.chatservice.netty.proto;

public enum ChatCode {

    AUTH_CODE(10000),
    PING_CODE(10001),
    PONG_CODE(10002),
    MESS_CODE(10003),
    SYS_USER_COUNT(20001),  // 在线用户数
    SYS_AUTH_STATE(20002),  // 认证结果
    SYS_SYST_INFO(20003),   // 系统消息
    ;

    private int value;

    ChatCode(int value) {
        this.value = value;
    }

    public int value() {
        return this.value;
    }

    public static ChatCode fromValue(int value) {
        for (ChatCode e : values()) {
            if (value == e.value) {
                return e;
            }
        }
        return null;
    }
}
