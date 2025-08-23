package com.tencent.im.backend.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * 请求日志全局过滤器
 *
 * @author IM Backend Team
 */
@Slf4j
@Component
public class RequestLoggingFilter implements GlobalFilter, Ordered {

    private static final String REQUEST_ID_HEADER = "X-Request-Id";
    private static final String START_TIME_ATTR = "startTime";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        
        // 生成请求ID
        String requestId = UUID.randomUUID().toString().substring(0, 8);
        
        // 记录开始时间
        long startTime = System.currentTimeMillis();
        exchange.getAttributes().put(START_TIME_ATTR, startTime);
        
        // 添加请求ID到请求头
        ServerHttpRequest modifiedRequest = request.mutate()
                .header(REQUEST_ID_HEADER, requestId)
                .build();
        
        ServerWebExchange modifiedExchange = exchange.mutate()
                .request(modifiedRequest)
                .build();
        
        // 记录请求信息
        log.info("请求开始 - RequestId: {}, Method: {}, URI: {}, RemoteAddr: {}", 
                requestId,
                request.getMethod().name(),
                request.getURI(),
                getClientIp(request));
        
        return chain.filter(modifiedExchange).then(
                Mono.fromRunnable(() -> {
                    // 记录响应信息
                    long endTime = System.currentTimeMillis();
                    long duration = endTime - startTime;
                    
                    log.info("请求结束 - RequestId: {}, Status: {}, Duration: {}ms", 
                            requestId,
                            exchange.getResponse().getStatusCode(),
                            duration);
                })
        );
    }

    /**
     * 获取客户端真实IP
     * 
     * @param request HTTP请求
     * @return 客户端IP
     */
    private String getClientIp(ServerHttpRequest request) {
        String xForwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeaders().getFirst("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }
        
        return request.getRemoteAddress() != null ? 
                request.getRemoteAddress().getAddress().getHostAddress() : "unknown";
    }

    @Override
    public int getOrder() {
        return -200; // 最高优先级，确保第一个执行
    }
}
