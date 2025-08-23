package com.tencent.im.backend.user.dto;

import lombok.Data;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * 发送邮箱验证码请求DTO
 *
 * @author IM Backend Team
 */
@Data
public class SendVerificationCodeRequest {

    /**
     * 邮箱
     */
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    /**
     * 验证码用途：1-注册，2-找回密码，3-修改邮箱
     */
    private Integer purpose = 1; // 默认为注册
}
