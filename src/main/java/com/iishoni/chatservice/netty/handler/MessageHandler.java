package com.iishoni.chatservice.netty.handler;

import com.iishoni.chatservice.netty.model.Message;
import com.iishoni.chatservice.netty.model.UserInfo;
import com.iishoni.chatservice.netty.proto.ChatCode;

import com.iishoni.chatservice.netty.util.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;

public class MessageHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {

    private static final Logger logger = LoggerFactory.getLogger(MessageHandler.class);

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame frame) {
        UserInfo userInfo = UserInfoManager.getUserInfo(ctx.channel());
        if (userInfo != null && userInfo.getIsAuth()) {
            Message message = JsonUtil.parseMsg(frame.text());
            // 广播返回用户发送的消息文本
            UserInfoManager.broadcastMess(userInfo.getUid(), userInfo.getNick(), message.getMsg());
        }
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        UserInfoManager.removeChannel(ctx.channel());
        UserInfoManager.broadcastSyst(ChatCode.SYS_USER_COUNT.value(), UserInfoManager.getAuthUserCount());
        super.channelUnregistered(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.error("连接异常，即将关闭该通道", cause);
        UserInfoManager.removeChannel(ctx.channel());
        UserInfoManager.broadcastSyst(ChatCode.SYS_USER_COUNT.value(), UserInfoManager.getAuthUserCount());
    }
}
