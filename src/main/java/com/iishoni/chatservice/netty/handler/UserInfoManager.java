package com.iishoni.chatservice.netty.handler;

import com.iishoni.chatservice.netty.model.UserInfo;
import com.iishoni.chatservice.netty.proto.ChatProto;
import com.iishoni.chatservice.netty.util.NettyUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;

public class UserInfoManager {

    private static final Logger logger = LoggerFactory.getLogger(UserInfoManager.class);

    private static ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock(true);
    private static ConcurrentMap<Channel, UserInfo> userInfos = new ConcurrentHashMap<>();
    private static AtomicInteger userCount = new AtomicInteger(0);

    /**
     * 保存用户信息
     */
    public static boolean saveUser(Channel channel, Long uid, String nick) {
        UserInfo userInfo = userInfos.get(channel);
        if (userInfo == null) {
            return false;
        }
        if (!channel.isActive()) {
            logger.error("通道已断开: 地址: [{}]", userInfo.getAddr());
            return false;
        }
        // 增加一个认证用户
        userCount.incrementAndGet();
        userInfo.setNick(nick);
        userInfo.setIsAuth(true);
        userInfo.setUid(uid);
        userInfo.setLoginTime(System.currentTimeMillis());
        return true;
    }

    /**
     * 新增channel
     */
    public static void addChannel(Channel channel) {
        String remoteAddr = NettyUtil.parseChannelRemoteAddr(channel);
        if (!channel.isActive()) {
            logger.error("通道已断开: 地址: [{}]", remoteAddr);
        }
        UserInfo userInfo = new UserInfo();
        userInfo.setLoginTime(System.currentTimeMillis());
        userInfo.setAddr(remoteAddr);
        userInfo.setChannel(channel);
        userInfos.put(channel, userInfo);
    }

    /**
     * 从缓存中移除Channel，并且关闭Channel
     */
    public static void removeChannel(Channel channel) {
        try {
            rwLock.writeLock().lock();
            logger.warn("通道将被关闭，地址: [{}]", NettyUtil.parseChannelRemoteAddr(channel));
            channel.close();
            UserInfo userInfo = userInfos.get(channel);
            if (userInfo != null) {
                UserInfo tmp = userInfos.remove(channel);
                if (tmp != null && tmp.getIsAuth()) {
                    // 减去一个认证用户
                    userCount.decrementAndGet();
                }
            }
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    /**
     * 广播ping消息
     */
    public static void broadcastPing() {
        try {
            rwLock.readLock().lock();
            logger.info("正在发送心跳，用户数量: {}", userCount.intValue());
            Set<Channel> keySet = userInfos.keySet();
            for (Channel ch : keySet) {
                UserInfo userInfo = userInfos.get(ch);
                if (userInfo == null || !userInfo.getIsAuth()) continue;
                ch.writeAndFlush(new TextWebSocketFrame(ChatProto.buildPingProto()));
            }
        } finally {
            rwLock.readLock().unlock();
        }
    }

    /**
     * 广播普通消息
     */
    public static void broadcastMess(Long uid, String nick, String message) {
        try {
            rwLock.readLock().lock();
            Set<Channel> keySet = userInfos.keySet();
            for (Channel ch : keySet) {
                UserInfo userInfo = userInfos.get(ch);
                if (userInfo == null || !userInfo.getIsAuth()) continue;
                ch.writeAndFlush(new TextWebSocketFrame(ChatProto.buildMessProto(uid, nick, message)));
            }
        } finally {
            rwLock.readLock().unlock();
        }
    }

    /**
     * 广播系统消息
     */
    public static void broadcastSyst(int code, Object msg) {
        try {
            rwLock.readLock().lock();
            Set<Channel> keySet = userInfos.keySet();
            for (Channel ch : keySet) {
                UserInfo userInfo = userInfos.get(ch);
                if (userInfo == null || !userInfo.getIsAuth()) continue;
                ch.writeAndFlush(new TextWebSocketFrame(ChatProto.buildSystProto(code, msg)));
            }
        } finally {
            rwLock.readLock().unlock();
        }
    }

    /**
     * 单条通道发送系统消息
     */
    public static void sendSyst(Channel channel, int code, Object msg) {
        channel.writeAndFlush(new TextWebSocketFrame(ChatProto.buildSystProto(code, msg)));
    }

    /**
     * 扫描并关闭失效的Channel
     * 关闭条件：
     * 1.通道已关闭
     * 2.通道已断开
     * 3.客户端心跳超时
     */
    public static void scanNotActiveChannel() {
        Set<Channel> keySet = userInfos.keySet();
        for (Channel ch : keySet) {
            UserInfo userInfo = userInfos.get(ch);
            if (userInfo == null) continue;
            if (!ch.isOpen() || !ch.isActive() ||
                    (!userInfo.getIsAuth() && (System.currentTimeMillis() - userInfo.getLoginTime()) > 10000)) {
                removeChannel(ch);
            }
        }
    }

    /**
     * 获取用户信息
     */
    public static UserInfo getUserInfo(Channel channel) {
        return userInfos.get(channel);
    }

    /**
     * 获取认证用户数量
     */
    public static int getAuthUserCount() {
        return userCount.get();
    }

    /**
     * 更新用户登录时间
     */
    public static void updateUserLoginTime(Channel channel) {
        UserInfo userInfo = getUserInfo(channel);
        if (userInfo != null) {
            userInfo.setLoginTime(System.currentTimeMillis());
        }
    }
}
