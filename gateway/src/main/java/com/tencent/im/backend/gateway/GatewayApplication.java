package com.tencent.im.backend.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 网关服务启动类
 *
 * @author IM Backend Team
 */
@SpringBootApplication
@EnableDiscoveryClient
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
        System.out.println("=== IM Backend Gateway Service Started Successfully ===");
        System.out.println("=== Gateway API: http://localhost:8080 ===");
        System.out.println("=== Health Check: http://localhost:8080/actuator/health ===");
    }
}
