package com.iishoni.chatservice.netty.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iishoni.chatservice.netty.model.Message;

public class JsonUtil {

    public static String object2String(Object object) {
        ObjectMapper mapper = new ObjectMapper();
        String json = null;
        try {
            json = mapper.writeValueAsString(object);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return json;
    }

    public static <T> T string2Object(String string, Class<T> t) {
        ObjectMapper mapper = new ObjectMapper();
        T result = null;
        try {
            result = mapper.readValue(string, t);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public static Message parseMsg(String string) {
        ObjectMapper mapper = new ObjectMapper();
        Message message = null;
        try {
            message = mapper.readValue(string, Message.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return message;
    }
}
