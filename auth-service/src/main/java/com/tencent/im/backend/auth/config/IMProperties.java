package com.tencent.im.backend.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * IM配置属性类
 */
@Component
@ConfigurationProperties(prefix = "im")
public class IMProperties {

    private long sdkAppId;
    
    private UserSig userSig = new UserSig();
    
    private PrivateKey privateKey = new PrivateKey();

    public static class UserSig {
        private long expireSeconds = 86400; // 默认24小时

        public long getExpireSeconds() {
            return expireSeconds;
        }

        public void setExpireSeconds(long expireSeconds) {
            this.expireSeconds = expireSeconds;
        }
    }

    public static class PrivateKey {
        private String ref;

        public String getRef() {
            return ref;
        }

        public void setRef(String ref) {
            this.ref = ref;
        }
    }

    // Getters and Setters
    public long getSdkAppId() {
        return sdkAppId;
    }

    public void setSdkAppId(long sdkAppId) {
        this.sdkAppId = sdkAppId;
    }

    public UserSig getUserSig() {
        return userSig;
    }

    public void setUserSig(UserSig userSig) {
        this.userSig = userSig;
    }

    public PrivateKey getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(PrivateKey privateKey) {
        this.privateKey = privateKey;
    }
}
