package com.tencent.im.backend.gateway.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 服务降级处理控制器
 *
 * @author IM Backend Team
 */
@Slf4j
@RestController
@RequestMapping("/fallback")
public class FallbackController {

    /**
     * 用户服务降级处理
     */
    @RequestMapping("/user")
    public ResponseEntity<Map<String, Object>> userFallback() {
        log.warn("用户服务不可用，触发降级处理");
        return createFallbackResponse("用户服务暂时不可用，请稍后重试");
    }

    /**
     * 认证服务降级处理
     */
    @RequestMapping("/auth")
    public ResponseEntity<Map<String, Object>> authFallback() {
        log.warn("认证服务不可用，触发降级处理");
        return createFallbackResponse("认证服务暂时不可用，请稍后重试");
    }

    /**
     * 连接服务降级处理
     */
    @RequestMapping("/connection")
    public ResponseEntity<Map<String, Object>> connectionFallback() {
        log.warn("连接服务不可用，触发降级处理");
        return createFallbackResponse("连接服务暂时不可用，请稍后重试");
    }

    /**
     * 消息服务降级处理
     */
    @RequestMapping("/message")
    public ResponseEntity<Map<String, Object>> messageFallback() {
        log.warn("消息服务不可用，触发降级处理");
        return createFallbackResponse("消息服务暂时不可用，请稍后重试");
    }

    /**
     * 创建统一的降级响应
     */
    private ResponseEntity<Map<String, Object>> createFallbackResponse(String message) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 503);
        result.put("message", message);
        result.put("data", null);
        result.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(result);
    }
}
