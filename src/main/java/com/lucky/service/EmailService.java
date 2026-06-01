package com.lucky.service;

import com.lucky.config.Config;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.util.Properties;

/**
 * 邮件发送服务（阿里云 SMTP）
 * 开发模式（未配置 SMTP 用户名密码）：验证码输出到控制台
 * 生产模式：通过阿里云 SMTP（smtpdm.aliyun.com）发送邮件
 */
public class EmailService {

    /**
     * 发送验证码邮件
     * @param toEmail 收件人邮箱
     * @param code    6 位验证码
     */
    public static void sendVerificationCode(String toEmail, String code) throws Exception {
        if (Config.isDevMode()) {
            // 开发模式：控制台输出验证码
            System.out.println("========================================");
            System.out.println("  [DEV] 验证码邮件（未真实发送）");
            System.out.println("  收件人: " + toEmail);
            System.out.println("  验证码: " + code);
            System.out.println("  如需真实发送，请设置环境变量：");
            System.out.println("    LUCKY_SMTP_USERNAME=你的发信地址");
            System.out.println("    LUCKY_SMTP_PASSWORD=你的SMTP密码");
            System.out.println("========================================");
            return;
        }

        // 生产模式：通过阿里云 SMTP 发送
        Properties props = new Properties();
        props.put("mail.smtp.host", Config.SMTP_HOST);
        props.put("mail.smtp.port", Config.SMTP_PORT);
        props.put("mail.smtp.ssl.enable", "true");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "10000");

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(Config.SMTP_USERNAME, Config.SMTP_PASSWORD);
            }
        });

        String html = String.format("""
            <div style="font-family: sans-serif; max-width: 400px; margin: 0 auto;">
                <h2>您的验证码</h2>
                <p style="font-size: 32px; font-weight: bold; letter-spacing: 8px;
                          background: #f5f5f5; padding: 16px; border-radius: 8px;
                          text-align: center;">%s</p>
                <p>有效期 5 分钟，请勿泄露给他人。</p>
                <p style="color: #999; font-size: 14px;">如非本人操作，请忽略此邮件。</p>
            </div>
        """, code);

        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(Config.SMTP_USERNAME, Config.SMTP_FROM));
        message.setRecipient(Message.RecipientType.TO, new InternetAddress(toEmail));
        message.setSubject("您的验证码是 " + code);
        message.setContent(html, "text/html; charset=utf-8");

        Transport.send(message);

        System.out.println("[Email] 验证码已发送至 " + toEmail);
    }
}
