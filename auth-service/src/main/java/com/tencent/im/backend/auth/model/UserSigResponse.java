package com.tencent.im.backend.auth.model;

/**
 * UserSig响应模型
 */
public class UserSigResponse {

    private String userId;
    private String userSig;
    private Long expireTime;
    private String sdkAppId;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserSig() {
        return userSig;
    }

    public void setUserSig(String userSig) {
        this.userSig = userSig;
    }

    public Long getExpireTime() {
        return expireTime;
    }

    public void setExpireTime(Long expireTime) {
        this.expireTime = expireTime;
    }

    public String getSdkAppId() {
        return sdkAppId;
    }

    public void setSdkAppId(String sdkAppId) {
        this.sdkAppId = sdkAppId;
    }
}
