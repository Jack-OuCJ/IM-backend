package com.tencent.im.backend.sequence;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 消息序列号服务启动类
 */
@SpringBootApplication(scanBasePackages = "com.tencent.im.backend")
@EnableDiscoveryClient
@EnableScheduling
public class MessageSequenceApplication {

    public static void main(String[] args) {
        SpringApplication.run(MessageSequenceApplication.class, args);
    }
}
