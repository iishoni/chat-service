package com.iishoni.chatservice.netty.handler;

import com.iishoni.chatservice.netty.model.Message;
import com.iishoni.chatservice.netty.proto.ChatCode;
import com.iishoni.chatservice.netty.util.JsonUtil;
import com.iishoni.chatservice.netty.util.NettyUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;

import java.util.Objects;

public class UserAuthHandler extends SimpleChannelInboundHandler<Object> {

    private static final Logger logger = LoggerFactory.getLogger(UserAuthHandler.class);

    @Value("${websocket.url}")
    private String websocketUrl;

    private WebSocketServerHandshaker handshaker;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof FullHttpRequest) {
            handleHttpRequest(ctx, (FullHttpRequest) msg);
        } else if (msg instanceof WebSocketFrame) {
            handleWebSocket(ctx, (WebSocketFrame) msg);
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent) evt;
            // 判断Channel是否读空闲
            if (event.state().equals(IdleState.READER_IDLE)) {
                String remoteAddress = NettyUtil.parseChannelRemoteAddr(ctx.channel());
                logger.warn("[{}] 读空闲超时！", remoteAddress);
                // 读空闲时移除Channel
                UserInfoManager.removeChannel(ctx.channel());
                // 发送系统消息，更新在线人数
                UserInfoManager.broadcastSyst(ChatCode.SYS_USER_COUNT.value(), UserInfoManager.getAuthUserCount());
            }
        }
        ctx.fireUserEventTriggered(evt);
    }

    private void handleHttpRequest(ChannelHandlerContext ctx, FullHttpRequest request) {
        if (!request.decoderResult().isSuccess() || !"websocket".equals(request.headers().get("Upgrade"))) {
            logger.warn("客户端暂不支持websocket");
            ctx.channel().close();
            return;
        }
        WebSocketServerHandshakerFactory handshakerFactory = new WebSocketServerHandshakerFactory(
                websocketUrl, null, true);
        handshaker = handshakerFactory.newHandshaker(request);
        if (handshaker == null) {
            WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
        } else {
            // 动态加入websocket的编解码处理
            handshaker.handshake(ctx.channel(), request);
            // 存储已经连接的Channel
            UserInfoManager.addChannel(ctx.channel());
        }
    }

    private void handleWebSocket(ChannelHandlerContext ctx, WebSocketFrame frame) {
        // 判断是否关闭链路命令
        if (frame instanceof CloseWebSocketFrame) {
            handshaker.close(ctx.channel(), (CloseWebSocketFrame) frame.retain());
            UserInfoManager.removeChannel(ctx.channel());
            return;
        }
        // 判断是否Ping消息
        if (frame instanceof PingWebSocketFrame) {
            logger.info("ping消息: {}", frame.content().retain());
            ctx.writeAndFlush(new PongWebSocketFrame(frame.content().retain()));
            return;
        }
        // 判断是否Pong消息
        if (frame instanceof PongWebSocketFrame) {
            logger.info("pong消息: {}", frame.content().retain());
            ctx.writeAndFlush(new PongWebSocketFrame(frame.content().retain()));
            return;
        }
        // 本程序目前只支持文本消息
        if (!(frame instanceof TextWebSocketFrame)) {
            throw new UnsupportedOperationException(frame.getClass().getName() + " frame type not supported");
        }

        Channel channel = ctx.channel();
        Message message = JsonUtil.parseMsg(((TextWebSocketFrame) frame).text());
        int code = message.getCode();
        switch (Objects.requireNonNull(ChatCode.fromValue(code))) {
            case PING_CODE:
            case PONG_CODE: // 心跳消息，则更新用户登录时间
                UserInfoManager.updateUserLoginTime(channel);
                logger.info("接收心跳来自地址: [{}]", NettyUtil.parseChannelRemoteAddr(channel));
                return;
            case AUTH_CODE: // 认证消息，则保存用户信息，发送认证结果，更新在线人数
                boolean isSuccess = UserInfoManager.saveUser(channel, message.getUid(), message.getNick());
                UserInfoManager.sendSyst(channel, ChatCode.SYS_AUTH_STATE.value(), isSuccess);
                if (isSuccess) {
                    UserInfoManager.broadcastSyst(ChatCode.SYS_USER_COUNT.value(), UserInfoManager.getAuthUserCount());
                }
                return;
            case MESS_CODE: // 普通消息，留给 MessageHandler 处理
                break;
            default:
                logger.warn("状态码 [{}] 未授权!", code);
                return;
        }
        //后续消息交给MessageHandler处理
        ctx.fireChannelRead(frame.retain());
    }
}
