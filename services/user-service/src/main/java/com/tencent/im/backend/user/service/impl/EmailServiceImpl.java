package com.tencent.im.backend.user.service.impl;

import com.tencent.im.backend.user.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * 邮件服务实现类
 *
 * @author IM Backend Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String from;

    @Value("${user.email.verification.template:您的验证码是：%s，有效期5分钟，请勿泄露给他人。}")
    private String template;

    @Override
    public boolean sendVerificationCode(String to, String code) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(to);
            message.setSubject("IM后端系统邮箱验证码");
            message.setText(String.format(template, code));
            
            mailSender.send(message);
            log.info("验证码邮件发送成功，收件人：{}", to);
            return true;
        } catch (Exception e) {
            log.error("验证码邮件发送失败，收件人：{}，错误：{}", to, e.getMessage(), e);
            return false;
        }
    }
}
