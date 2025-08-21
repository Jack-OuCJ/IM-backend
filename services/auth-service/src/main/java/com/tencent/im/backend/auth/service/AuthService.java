package com.tencent.im.backend.auth.service;

import com.tencentyun.TLSSigAPIv2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

@Service
public class AuthService {

    @Value("${im.sdkAppId}")
    private long sdkAppId;
        
    @Value("${im.usersig.expireSeconds}")
    private long usersigExpireSeconds;
    
    @Value("${im.privateKey.ref}")
    private String privateKeyRef;

    @SuppressWarnings("unused")
    private volatile String privateKey;
    private volatile TLSSigAPIv2 tlsSigApi;

    @PostConstruct
    public void init() {
        reloadKeyAndApi();
    }

    public synchronized void reloadKeyAndApi() {
        String key = loadKeyByRef(privateKeyRef);
        if (key == null || key.isBlank()) {
            throw new IllegalStateException("Private key is empty: " + privateKeyRef);
        }
        this.privateKey = key;

        this.tlsSigApi = new TLSSigAPIv2(sdkAppId, key);
    }

    public String generateUserSig(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId cannot be null or empty");
        }
        TLSSigAPIv2 api = this.tlsSigApi;
        return api.genUserSig(userId, usersigExpireSeconds);
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
        if (ref != null && ref.startsWith("file://")) {
            try {
                return Files.readString(Paths.get(URI.create(ref)), StandardCharsets.UTF_8);
            } catch (Exception e) {
                throw new RuntimeException("Read private key failed: " + ref, e);
            }
        }
        throw new IllegalArgumentException("Unsupported ref: " + ref);
    }
}