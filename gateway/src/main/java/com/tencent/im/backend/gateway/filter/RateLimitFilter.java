package com.tencent.im.backend.gateway.filter;

import com.alibaba.fastjson2.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 基于Redis的限流全局过滤器
 *
 * @author IM Backend Team
 */
@Slf4j
@Component
public class RateLimitFilter implements GlobalFilter, Ordered {

    @Autowired
    private ReactiveStringRedisTemplate redisTemplate;

    // 限流配置
    private static final int DEFAULT_RATE_LIMIT = 100; // 每分钟100次请求
    private static final Duration RATE_LIMIT_WINDOW = Duration.ofMinutes(1);
    
    // 限流配置映射 (路径前缀 -> 限制次数)
    private static final Map<String, Integer> RATE_LIMIT_CONFIG = Map.of(
            "/api/auth/", 10,  // 认证接口每分钟10次
            "/api/user/register", 5,  // 注册接口每分钟5次
            "/api/user/send-verification-code", 3,  // 验证码接口每分钟3次
            "/api/", DEFAULT_RATE_LIMIT  // 其他API接口每分钟100次
    );

    // Lua脚本：原子性地检查和增加计数器
    private static final String RATE_LIMIT_SCRIPT = 
        "local key = KEYS[1] " +
        "local limit = tonumber(ARGV[1]) " +
        "local window = tonumber(ARGV[2]) " +
        "local current = redis.call('GET', key) " +
        "if current == false then " +
        "  redis.call('SET', key, 1) " +
        "  redis.call('EXPIRE', key, window) " +
        "  return {1, limit} " +
        "else " +
        "  current = tonumber(current) " +
        "  if current < limit then " +
        "    local newval = redis.call('INCR', key) " +
        "    local ttl = redis.call('TTL', key) " +
        "    if ttl == -1 then " +
        "      redis.call('EXPIRE', key, window) " +
        "    end " +
        "    return {newval, limit} " +
        "  else " +
        "    local ttl = redis.call('TTL', key) " +
        "    return {current, limit, ttl} " +
        "  end " +
        "end";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        
        // 获取客户端IP
        String clientIp = getClientIp(request);
        
        // 获取限流配置
        int rateLimit = getRateLimit(path);
        
        // 构建Redis key
        String key = "rate_limit:" + clientIp + ":" + path;
        
        // 执行限流检查
        return checkRateLimit(key, rateLimit)
                .flatMap(result -> {
                    long current = result[0];
                    long limit = result[1];
                    
                    // 添加限流响应头
                    ServerHttpResponse response = exchange.getResponse();
                    response.getHeaders().add("X-RateLimit-Limit", String.valueOf(limit));
                    response.getHeaders().add("X-RateLimit-Remaining", String.valueOf(Math.max(0, limit - current)));
                    
                    if (current > limit) {
                        // 触发限流
                        long resetTime = result.length > 2 ? result[2] : RATE_LIMIT_WINDOW.getSeconds();
                        response.getHeaders().add("X-RateLimit-Reset", String.valueOf(resetTime));
                        
                        log.warn("限流触发 - IP: {}, Path: {}, Current: {}, Limit: {}", 
                                clientIp, path, current, limit);
                        
                        return handleRateLimitExceeded(exchange);
                    } else {
                        // 继续处理请求
                        log.debug("限流检查通过 - IP: {}, Path: {}, Current: {}, Limit: {}", 
                                clientIp, path, current, limit);
                        
                        return chain.filter(exchange);
                    }
                })
                .onErrorResume(throwable -> {
                    // Redis异常时不阻塞请求
                    log.error("限流检查异常，允许请求通过: {}", throwable.getMessage());
                    return chain.filter(exchange);
                });
    }

    /**
     * 执行限流检查
     * 
     * @param key Redis键
     * @param limit 限制次数
     * @return 检查结果 [当前次数, 限制次数, 重置时间(可选)]
     */
    private Mono<long[]> checkRateLimit(String key, int limit) {
        RedisScript<List> script = RedisScript.of(RATE_LIMIT_SCRIPT, List.class);
        
        return redisTemplate.execute(script, 
                List.of(key), 
                List.of(String.valueOf(limit), String.valueOf(RATE_LIMIT_WINDOW.getSeconds())))
                .collectList()
                .map(results -> {
                    if (results.isEmpty()) {
                        return new long[]{0, limit};
                    }
                    List result = results.get(0);
                    long[] values = new long[result.size()];
                    for (int i = 0; i < result.size(); i++) {
                        values[i] = ((Number) result.get(i)).longValue();
                    }
                    return values;
                });
    }

    /**
     * 获取路径对应的限流配置
     * 
     * @param path 请求路径
     * @return 限制次数
     */
    private int getRateLimit(String path) {
        return RATE_LIMIT_CONFIG.entrySet().stream()
                .filter(entry -> path.startsWith(entry.getKey()))
                .mapToInt(Map.Entry::getValue)
                .findFirst()
                .orElse(DEFAULT_RATE_LIMIT);
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

    /**
     * 处理限流超出情况
     * 
     * @param exchange ServerWebExchange
     * @return Mono<Void>
     */
    private Mono<Void> handleRateLimitExceeded(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> result = new HashMap<>();
        result.put("code", 429);
        result.put("message", "请求过于频繁，请稍后再试");
        result.put("data", null);

        String body = JSON.toJSONString(result);
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -50; // 在认证过滤器之后，业务过滤器之前执行
    }
}
