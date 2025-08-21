package com.tencent.im.backend.auth.config;

import com.tencent.im.backend.auth.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 配置刷新监听器
 * 监听Nacos配置变更事件，自动重新加载认证服务配置
 */
@Component
@RefreshScope
public class ConfigRefreshListener {

    private static final Logger logger = LoggerFactory.getLogger(ConfigRefreshListener.class);

    @Autowired
    private AuthService authService;

    @Autowired
    private IMProperties imProperties;

    @EventListener
    public void handleEnvironmentChange(EnvironmentChangeEvent event) {
        try {
            logger.info("Configuration change event received, reloading auth service configuration...");
            logger.info("Changed keys: {}", event.getKeys());
            
            // 检查是否有IM相关的配置变更
            boolean hasImConfigChange = event.getKeys().stream()
                .anyMatch(key -> key.startsWith("tencent.im."));
            
            if (hasImConfigChange) {
                // 重新加载认证服务配置
                authService.reloadKeyAndApi();
                
                logger.info("Auth service configuration reloaded successfully. SDKAppID: {}, ExpireSeconds: {}", 
                           imProperties.getSdkAppId(), 
                           imProperties.getUserSig().getExpireSeconds());
            }
                       
        } catch (Exception e) {
            logger.error("Failed to reload auth service configuration", e);
        }
    }
}
