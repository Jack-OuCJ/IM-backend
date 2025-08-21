package com.tencent.im.backend.auth.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 配置验证控制器
 * 用于验证 Nacos 配置是否正确加载
 */
@RestController
@RequestMapping("/config")
public class ConfigController {

    @Value("${im.sdkAppId}")
    private long sdkAppId;
    
    @Value("${im.usersig.expireSeconds}")
    private long usersigExpireSeconds;
    
    @Value("${im.privateKey.ref}")
    private String privateKeyRef;

    /**
     * 获取当前 IM 配置信息
     * 用于验证 Nacos 配置是否正确加载
     */
    @GetMapping("/im-properties")
    public Map<String, Object> getImProperties() {
        Map<String, Object> config = new HashMap<>();
        
        // 基本信息
        config.put("sdkAppId", sdkAppId);
        config.put("configLoaded", sdkAppId > 0);
        
        // Usersig 配置
        Map<String, Object> usersigConfig = new HashMap<>();
        usersigConfig.put("expireSeconds", usersigExpireSeconds);
        config.put("usersig", usersigConfig);
        
        // PrivateKey 配置
        Map<String, Object> privateKeyConfig = new HashMap<>();
        privateKeyConfig.put("ref", privateKeyRef);
        privateKeyConfig.put("refConfigured", privateKeyRef != null);
        config.put("privateKey", privateKeyConfig);
        
        // 配置状态
        config.put("status", "SUCCESS");
        config.put("message", "IM Properties loaded successfully from Nacos");
        
        return config;
    }

    /**
     * 健康检查端点
     * 验证配置是否完整
     */
    @GetMapping("/health")
    public Map<String, Object> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        
        boolean isHealthy = sdkAppId > 0 
                && privateKeyRef != null;
        
        health.put("status", isHealthy ? "UP" : "DOWN");
        health.put("configurationLoaded", isHealthy);
        health.put("details", Map.of(
            "sdkAppIdConfigured", sdkAppId > 0,
            "privateKeyConfigured", privateKeyRef != null
        ));
        
        return health;
    }
}
