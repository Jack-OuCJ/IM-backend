package com.tencent.im.backend.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * 网关路由配置
 *
 * @author IM Backend Team
 */
@Configuration
public class GatewayConfig {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                // 用户服务路由
                .route("user-service", r -> r
                        .path("/api/user/**")
                        .filters(f -> f
                                .circuitBreaker(config -> config
                                        .setName("user-service-cb")
                                        .setFallbackUri("forward:/fallback/user"))
                                .retry(config -> config
                                        .setRetries(3)
                                        .setBackoff(Duration.ofMillis(100), Duration.ofSeconds(1), 2, false)))
                        .uri("lb://user-service"))
                
                // 认证服务路由
                .route("auth-service", r -> r
                        .path("/api/auth/**")
                        .filters(f -> f
                                .circuitBreaker(config -> config
                                        .setName("auth-service-cb")
                                        .setFallbackUri("forward:/fallback/auth"))
                                .retry(config -> config
                                        .setRetries(2)
                                        .setBackoff(Duration.ofMillis(100), Duration.ofSeconds(1), 2, false))
                                .stripPrefix(2))
                        .uri("lb://auth-service"))
                
                // 认证服务配置路由（用于配置管理和调试）
                .route("auth-config-service", r -> r
                        .path("/config/**")
                        .filters(f -> f
                                .circuitBreaker(config -> config
                                        .setName("auth-service-cb")
                                        .setFallbackUri("forward:/fallback/auth"))
                                .retry(config -> config
                                        .setRetries(2)
                                        .setBackoff(Duration.ofMillis(100), Duration.ofSeconds(1), 2, false)))
                        .uri("lb://auth-service"))
                
                // 连接服务路由
                .route("connection-service", r -> r
                        .path("/api/connection/**")
                        .filters(f -> f
                                .circuitBreaker(config -> config
                                        .setName("connection-service-cb")
                                        .setFallbackUri("forward:/fallback/connection"))
                                .retry(config -> config
                                        .setRetries(3)
                                        .setBackoff(Duration.ofMillis(100), Duration.ofSeconds(1), 2, false)))
                        .uri("lb://connection-service"))
                
                // 消息序列服务路由
                .route("message-sequence-service", r -> r
                        .path("/api/message/**")
                        .filters(f -> f
                                .circuitBreaker(config -> config
                                        .setName("message-sequence-service-cb")
                                        .setFallbackUri("forward:/fallback/message"))
                                .retry(config -> config
                                        .setRetries(3)
                                        .setBackoff(Duration.ofMillis(100), Duration.ofSeconds(1), 2, false)))
                        .uri("lb://message-sequence-service"))
                
                .build();
    }
}
