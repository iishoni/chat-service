package com.iishoni.chatservice.netty.proto;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.iishoni.chatservice.netty.util.JsonUtil;
import lombok.Getter;
import lombok.Setter;

/**
 * 协议
 */
@Setter
@Getter
public class ChatProto {

    private static final int PING_PROTO = 1;    // ping消息
    private static final int PONG_PROTO = 2;    // pong消息
    private static final int SYST_PROTO = 3;    // 系统消息
    private static final int EROR_PROTO = 4;    // 错误消息
    private static final int MESS_PROTO = 5;    // 普通消息

    private int version = 1;
    private int uri;
    private String body;
    private Map<String, Object> extend = new HashMap<>();

    private ChatProto(int head, String body) {
        this.uri = head;
        this.body = body;
    }

    public static String buildPingProto() {
        return buildProto(PING_PROTO, null);
    }

    public static String buildPongProto() {
        return buildProto(PONG_PROTO, null);
    }

    public static String buildSystProto(int code, Object msg) {
        ChatProto chatProto = new ChatProto(SYST_PROTO, null);
        chatProto.extend.put("code", code);
        chatProto.extend.put("msg", msg);
        return JsonUtil.object2String(chatProto);
    }

    public static String buildErorProto(int code, String msg) {
        ChatProto chatProto = new ChatProto(EROR_PROTO, null);
        chatProto.extend.put("code", code);
        chatProto.extend.put("msg", msg);
        return JsonUtil.object2String(chatProto);
    }

    public static String buildMessProto(Long uid, String nick, String mess) {
        ChatProto chatProto = new ChatProto(MESS_PROTO, mess);
        chatProto.extend.put("uid", uid);
        chatProto.extend.put("nick", nick);
        chatProto.extend.put("time", new Date());
        return JsonUtil.object2String(chatProto);
    }

    private static String buildProto(int head, String body) {
        ChatProto chatProto = new ChatProto(head, body);
        return JsonUtil.object2String(chatProto);
    }

}
