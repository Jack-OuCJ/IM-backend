package com.tencent.im.backend.auth.controller;

import com.tencent.im.backend.auth.config.IMProperties;
import com.tencent.im.backend.auth.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
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

    @Autowired
    private IMProperties imProperties;
    
    @Autowired
    private AuthService authService;

    /**
     * 获取当前 IM 配置信息
     * 用于验证 Nacos 配置是否正确加载
     */
    @GetMapping("/im-properties")
    public Map<String, Object> getImProperties() {
        Map<String, Object> config = new HashMap<>();
        
        // 基本信息
        config.put("sdkAppId", imProperties.getSdkAppId());
        config.put("configLoaded", imProperties.getSdkAppId() > 0);
        
        // Usersig 配置
        Map<String, Object> usersigConfig = new HashMap<>();
        usersigConfig.put("expireSeconds", imProperties.getUserSig().getExpireSeconds());
        config.put("usersig", usersigConfig);
        
        // PrivateKey 配置
        Map<String, Object> privateKeyConfig = new HashMap<>();
        privateKeyConfig.put("ref", imProperties.getPrivateKey().getRef());
        privateKeyConfig.put("refConfigured", imProperties.getPrivateKey().getRef() != null);
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
        
        boolean isHealthy = imProperties.getSdkAppId() > 0 
                && imProperties.getPrivateKey().getRef() != null;
        
        health.put("status", isHealthy ? "UP" : "DOWN");
        health.put("configurationLoaded", isHealthy);
        health.put("details", Map.of(
            "sdkAppIdConfigured", imProperties.getSdkAppId() > 0,
            "privateKeyConfigured", imProperties.getPrivateKey().getRef() != null
        ));
        
        return health;
    }

    /**
     * 重新加载配置
     * 支持运行时动态更新
     */
    @PostMapping("/reload")
    public Map<String, Object> reloadConfig() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 重新加载认证服务的密钥和API
            authService.reloadKeyAndApi();
            
            result.put("status", "SUCCESS");
            result.put("message", "Configuration reloaded successfully");
            result.put("timestamp", System.currentTimeMillis());
            result.put("currentConfig", Map.of(
                "sdkAppId", imProperties.getSdkAppId(),
                "expireSeconds", imProperties.getUserSig().getExpireSeconds(),
                "privateKeyRef", imProperties.getPrivateKey().getRef()
            ));
        } catch (Exception e) {
            result.put("status", "FAILED");
            result.put("message", "Failed to reload configuration: " + e.getMessage());
            result.put("error", e.getClass().getSimpleName());
        }
        
        return result;
    }
}
