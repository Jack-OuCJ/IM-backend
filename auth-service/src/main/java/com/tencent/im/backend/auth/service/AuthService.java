package com.tencent.im.backend.auth.service;

import com.tencent.im.backend.auth.config.IMProperties;
import com.tencentyun.TLSSigAPIv2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

@Service
public class AuthService {

    @Autowired
    private IMProperties imProperties;

    @SuppressWarnings("unused")
    private volatile String privateKey;
    private volatile TLSSigAPIv2 tlsSigApi;

    @PostConstruct
    public void init() {
        reloadKeyAndApi();
    }

    public synchronized void reloadKeyAndApi() {
        String key = loadKeyByRef(imProperties.getPrivateKey().getRef());
        if (key == null || key.isBlank()) {
            throw new IllegalStateException("Private key is empty: " + imProperties.getPrivateKey().getRef());
        }
        this.privateKey = key;

        this.tlsSigApi = new TLSSigAPIv2(imProperties.getSdkAppId(), key);
    }

    public String generateUserSig(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId cannot be null or empty");
        }
        TLSSigAPIv2 api = this.tlsSigApi;
        return api.genUserSig(userId, imProperties.getUserSig().getExpireSeconds());
    }

    public String generateJwtToken(String userId, String username) {
        // 示例实现：生成简单的JWT Token
        return "jwt-token-for-" + userId;
    }

    public String getUserIdFromToken(String token) {
        // 示例实现：从Token中提取用户ID
        if (token.startsWith("jwt-token-for-")) {
            return token.substring("jwt-token-for-".length());
        }
        return null;
    }

    private String loadKeyByRef(String ref) {
        if (ref == null || ref.trim().isEmpty()) {
            throw new IllegalArgumentException("Private key ref is null or empty");
        }
        
        if (ref.startsWith("file://")) {
            try {
                return Files.readString(Paths.get(URI.create(ref)), StandardCharsets.UTF_8);
            } catch (Exception e) {
                throw new RuntimeException("Read private key failed: " + ref, e);
            }
        } else if (ref.startsWith("inline:")) {
            // 直接返回内联的私钥内容，去掉 "inline:" 前缀
            return ref.substring(7);
        } else {
            // 如果没有前缀，直接作为内联内容处理
            return ref;
        }
    }
}