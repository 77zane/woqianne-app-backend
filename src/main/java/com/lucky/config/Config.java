package com.lucky.config;

/**
 * 配置常量
 * 敏感信息通过环境变量注入，开发时有默认 fallback
 */
public class Config {

    // 服务端口
    public static final int PORT = Integer.parseInt(getEnv("LUCKY_PORT", "8080"));

    // JWT 密钥（至少 256 bit，部署时必须更换）
    public static final String JWT_SECRET = getEnv("LUCKY_JWT_SECRET",
            "lucky-dev-secret-key-2026-please-change-in-production!!");

    // JWT 有效期：10 年（毫秒）
    public static final long JWT_EXPIRATION = 10L * 365 * 24 * 60 * 60 * 1000;

    // ====== 阿里云 SMTP 配置 ======
    // SMTP 服务器（阿里云邮件推送）
    public static final String SMTP_HOST = getEnv("LUCKY_SMTP_HOST", "smtpdm.aliyun.com");
    // SMTP 端口（465 = SSL，25 = 非SSL）
    public static final int SMTP_PORT = Integer.parseInt(getEnv("LUCKY_SMTP_PORT", "465"));
    // 发信地址，如 noreply@yourdomain.com（在阿里云控制台创建）
    public static final String SMTP_USERNAME = getEnv("LUCKY_SMTP_USERNAME", "");
    // SMTP 密码（在阿里云控制台设置）
    public static final String SMTP_PASSWORD = getEnv("LUCKY_SMTP_PASSWORD", "");
    // 发件人显示名称
    public static final String SMTP_FROM = getEnv("LUCKY_SMTP_FROM", "Lucky");

    // 数据库路径
    public static final String DB_PATH = "data/lucky.db";

    // 验证码有效期：5 分钟（毫秒）
    public static final long CODE_EXPIRATION = 5 * 60 * 1000;

    // 验证码重发间隔：60 秒（毫秒）
    public static final long CODE_RESEND_INTERVAL = 60 * 1000;

    // 是否为开发模式（未配置 SMTP 用户名密码时，验证码输出到控制台）
    public static boolean isDevMode() {
        return SMTP_USERNAME.isEmpty() || SMTP_PASSWORD.isEmpty();
    }

    private static String getEnv(String key, String defaultValue) {
        String value = System.getenv(key);
        return (value != null && !value.isEmpty()) ? value : defaultValue;
    }
}
