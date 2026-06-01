package com.lucky.handler;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.fasterxml.jackson.databind.JsonNode;
import com.lucky.config.Config;
import com.lucky.db.Database;
import com.lucky.service.EmailService;

import io.javalin.http.Context;

import java.sql.*;
import java.util.Date;
import java.util.Map;

/**
 * 认证接口处理
 * - POST /api/auth/send-code  发送验证码
 * - POST /api/auth/login      登录（登录即注册）
 */
public class AuthHandler {

    /**
     * 发送验证码
     * POST /api/auth/send-code
     * Body: { "email": "user@example.com" }
     */
    public static void sendCode(Context ctx) {
        JsonNode body;
        try {
            body = ctx.bodyAsClass(JsonNode.class);
        } catch (Exception e) {
            ctx.status(400).json(Map.of("error", "请求格式错误"));
            return;
        }

        String email = body.has("email") ? body.get("email").asText() : null;

        // 1. 校验邮箱格式
        if (email == null || !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            ctx.status(400).json(Map.of("error", "邮箱格式不正确"));
            return;
        }

        // 2. 检查 60 秒内是否已发送
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "SELECT created_at FROM verify_codes WHERE email = ? ORDER BY id DESC LIMIT 1"
             )) {
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                long lastSent = rs.getLong("created_at");
                if (System.currentTimeMillis() - lastSent < Config.CODE_RESEND_INTERVAL) {
                    ctx.status(429).json(Map.of("error", "请求过于频繁，请60秒后重试"));
                    return;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            ctx.status(500).json(Map.of("error", "服务器错误"));
            return;
        }

        // 3. 生成 6 位数字验证码
        String code = String.valueOf(100000 + (int) (Math.random() * 900000));

        // 4. 存入数据库
        long now = System.currentTimeMillis();
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "INSERT INTO verify_codes (email, code, expires_at, created_at) VALUES (?, ?, ?, ?)"
             )) {
            ps.setString(1, email);
            ps.setString(2, code);
            ps.setLong(3, now + Config.CODE_EXPIRATION);
            ps.setLong(4, now);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            ctx.status(500).json(Map.of("error", "服务器错误"));
            return;
        }

        // 5. 发送邮件（开发模式控制台输出）
        try {
            EmailService.sendVerificationCode(email, code);
        } catch (Exception e) {
            e.printStackTrace();
            ctx.status(500).json(Map.of("error", "验证码发送失败，请稍后重试"));
            return;
        }

        ctx.json(Map.of("data", Map.of("message", "验证码已发送")));
    }

    /**
     * 登录（登录即注册）
     * POST /api/auth/login
     * Body: { "email": "user@example.com", "code": "654321" }
     */
    public static void login(Context ctx) {
        JsonNode body;
        try {
            body = ctx.bodyAsClass(JsonNode.class);
        } catch (Exception e) {
            ctx.status(400).json(Map.of("error", "请求格式错误"));
            return;
        }

        String email = body.has("email") ? body.get("email").asText() : null;
        String code = body.has("code") ? body.get("code").asText() : null;

        // 1. 基本校验
        if (email == null || email.isBlank() || code == null || code.isBlank()) {
            ctx.status(400).json(Map.of("error", "邮箱和验证码不能为空"));
            return;
        }

        long now = System.currentTimeMillis();

        try (Connection conn = Database.getConnection()) {

            // 2. 校验验证码
            PreparedStatement ps = conn.prepareStatement(
                "SELECT id, code, expires_at FROM verify_codes WHERE email = ? AND used_at IS NULL ORDER BY id DESC LIMIT 1"
            );
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();

            if (!rs.next()) {
                ctx.status(401).json(Map.of("error", "验证码错误"));
                return;
            }

            String dbCode = rs.getString("code");
            long expiresAt = rs.getLong("expires_at");
            long codeId = rs.getLong("id");

            if (!dbCode.equals(code)) {
                ctx.status(401).json(Map.of("error", "验证码错误"));
                return;
            }

            if (now > expiresAt) {
                ctx.status(410).json(Map.of("error", "验证码已过期，请重新发送"));
                return;
            }

            // 3. 标记验证码已使用
            PreparedStatement updatePs = conn.prepareStatement(
                "UPDATE verify_codes SET used_at = ? WHERE id = ?"
            );
            updatePs.setLong(1, now);
            updatePs.setLong(2, codeId);
            updatePs.executeUpdate();

            // 4. 查找或创建用户
            PreparedStatement userPs = conn.prepareStatement(
                "SELECT id FROM users WHERE email = ?"
            );
            userPs.setString(1, email);
            ResultSet userRs = userPs.executeQuery();

            int userId;
            boolean isNewUser = false;

            if (userRs.next()) {
                userId = userRs.getInt("id");
            } else {
                // 新用户 → 创建
                PreparedStatement createPs = conn.prepareStatement(
                    "INSERT INTO users (email, created_at) VALUES (?, ?)",
                    Statement.RETURN_GENERATED_KEYS
                );
                createPs.setString(1, email);
                createPs.setLong(2, now);
                createPs.executeUpdate();
                ResultSet genKeys = createPs.getGeneratedKeys();
                if (genKeys.next()) {
                    userId = genKeys.getInt(1);
                } else {
                    ctx.status(500).json(Map.of("error", "服务器错误"));
                    return;
                }
                isNewUser = true;
                System.out.println("[Auth] 新用户注册: " + email + " (id=" + userId + ")");
            }

            // 5. 签发 JWT
            String token = JWT.create()
                .withSubject(String.valueOf(userId))
                .withClaim("email", email)
                .withIssuedAt(new Date(now))
                .withExpiresAt(new Date(now + Config.JWT_EXPIRATION))
                .sign(Algorithm.HMAC256(Config.JWT_SECRET));

            System.out.println("[Auth] 用户登录: " + email + " (isNew=" + isNewUser + ")");

            ctx.json(Map.of("data", Map.of(
                "token", token,
                "isNewUser", isNewUser
            )));

        } catch (SQLException e) {
            e.printStackTrace();
            ctx.status(500).json(Map.of("error", "服务器错误"));
        }
    }
}
