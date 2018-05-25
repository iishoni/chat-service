package com.iishoni.chatservice.netty.model;

import io.netty.channel.Channel;
import lombok.Data;

@Data
public class UserInfo {

    private Boolean isAuth = false; // 是否认证
    private Long loginTime;         // 登录时间
    private Long uid;               // 用户id
    private String nick;            // 用户昵称
    private String addr;            // 地址
    private Channel channel;        // 通道
}
