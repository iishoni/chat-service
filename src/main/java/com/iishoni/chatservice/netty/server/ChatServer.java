package com.iishoni.chatservice.netty.server;

import com.iishoni.chatservice.netty.handler.MessageHandler;
import com.iishoni.chatservice.netty.handler.UserAuthHandler;
import com.iishoni.chatservice.netty.handler.UserInfoManager;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.IdleStateHandler;

@Component
public class ChatServer extends BaseServer {

    @Value("${socket.port}")
    private int port;
    @Value("${socket.backlog}")
    private int backlog;
    @Value("${socket.max_content_length}")
    private int maxContentLength;

    private ScheduledExecutorService executorService;

    public ChatServer() {
        executorService = Executors.newScheduledThreadPool(2);
    }

    @Override
    public void start() {
        serverBootstrap.group(bossGroup, workGroup)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.SO_BACKLOG, backlog)
                .localAddress(new InetSocketAddress(port))
                .childHandler(new ChannelInitializer<SocketChannel>() {

                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline().addLast(defLoopGroup,
                                new HttpServerCodec(),   //请求解码器
                                new HttpObjectAggregator(maxContentLength),//将多个消息转换成单一的消息对象
                                new ChunkedWriteHandler(),  //支持异步发送大的码流，一般用于发送文件流
                                new IdleStateHandler(60, 0, 0), //检测链路是否读空闲
                                new UserAuthHandler(),  //处理握手和认证
                                new MessageHandler()    //处理消息的发送
                        );
                    }
                });

        try {
            channelFuture = serverBootstrap.bind().sync();
            InetSocketAddress addr = (InetSocketAddress) channelFuture.channel().localAddress();
            logger.info("websocket服务启动成功！端口: {}", addr.getPort());

            // 定时扫描所有的Channel，关闭失效的Channel
            executorService.scheduleAtFixedRate(
                    UserInfoManager::scanNotActiveChannel, 3, 60, TimeUnit.SECONDS);

            // 定时向所有客户端发送Ping消息
            executorService.scheduleAtFixedRate(
                    UserInfoManager::broadcastPing, 3, 50, TimeUnit.SECONDS);

        } catch (InterruptedException e) {
            logger.error("websocket服务启动失败,", e);
        }
    }

    @Override
    public void shutdown() {
        if (executorService != null) {
            executorService.shutdown();
        }
        super.shutdown();
    }
}
