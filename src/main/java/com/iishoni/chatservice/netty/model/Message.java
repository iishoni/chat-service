package com.iishoni.chatservice.netty.model;

import lombok.Data;

import java.io.Serializable;

@Data
public class Message implements Serializable {
    private static final long serialVersionUID = -7902275132912091097L;

    private Long uid;
    private String nick;
    private Integer code;
    private String msg;
}
