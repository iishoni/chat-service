package com.iishoni.chatservice;

import com.iishoni.chatservice.netty.server.ChatServer;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ImportResource;

import javax.annotation.Resource;

@SpringBootApplication
@ImportResource("classpath*:spring-context.xml")
public class Application implements CommandLineRunner {

    @Resource
    private ChatServer server;

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Override
    public void run(String... strings) {
        server.init();
        server.start();
        // 注册进程钩子，在JVM进程关闭前释放资源
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            server.shutdown();
            System.exit(0);
        }));
    }
}
