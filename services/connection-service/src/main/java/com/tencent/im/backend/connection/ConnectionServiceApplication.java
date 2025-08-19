package com.tencent.im.backend.connection;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 连接服务启动类
 */
@SpringBootApplication(scanBasePackages = "com.tencent.im.backend")
@EnableDiscoveryClient
@EnableScheduling
public class ConnectionServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConnectionServiceApplication.class, args);
    }
}
