package com.tencent.im.backend.user.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 邮箱验证实体类
 *
 * @author IM Backend Team
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("email_verifications")
public class EmailVerification {

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 邮箱地址
     */
    @TableField("email")
    private String email;

    /**
     * 验证码
     */
    @TableField("verification_code")
    private String verificationCode;

    /**
     * 验证目的（1-注册，2-找回密码，3-邮箱验证）
     */
    @TableField("purpose")
    private Integer purpose;

    /**
     * 是否已验证（0-未验证，1-已验证）
     */
    @TableField("verified")
    private Integer verified;

    /**
     * 过期时间
     */
    @TableField("expires_at")
    private LocalDateTime expiresAt;

    /**
     * 创建时间
     */
    @TableField("created_at")
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField("updated_at")
    private LocalDateTime updatedAt;

    /**
     * 验证目的枚举
     */
    public enum Purpose {
        REGISTER(1, "注册"),
        RESET_PASSWORD(2, "找回密码"),
        EMAIL_VERIFY(3, "邮箱验证");

        private final Integer code;
        private final String description;

        Purpose(Integer code, String description) {
            this.code = code;
            this.description = description;
        }

        public Integer getCode() {
            return code;
        }

        public String getDescription() {
            return description;
        }

        public static Purpose getByCode(Integer code) {
            for (Purpose purpose : values()) {
                if (purpose.getCode().equals(code)) {
                    return purpose;
                }
            }
            return null;
        }
    }
}
