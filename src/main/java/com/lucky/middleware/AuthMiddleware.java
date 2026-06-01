package com.lucky.middleware;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.lucky.config.Config;

import io.javalin.http.Context;

import java.util.Map;

/**
 * JWT 鉴权中间件
 * 从 Authorization Header 中提取并验证 Bearer Token
 */
public class AuthMiddleware {

    private static final JWTVerifier verifier = JWT.require(Algorithm.HMAC256(Config.JWT_SECRET)).build();

    /**
     * 验证请求中的 JWT Token
     * 验证通过后将 userId 存入 ctx.attribute
     */
    /**
     * 验证请求中的 JWT Token
     * 验证通过后将 userId 存入 ctx.attribute 并返回 true
     * 验证失败则写入 401 响应并返回 false
     */
    public static boolean verify(Context ctx) {
        String header = ctx.header("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            ctx.status(401).json(Map.of("error", "未登录或登录已过期"));
            return false;
        }

        String token = header.substring(7);
        try {
            DecodedJWT jwt = verifier.verify(token);

            String userIdStr = jwt.getSubject();
            String email = jwt.getClaim("email").asString();

            ctx.attribute("userId", Integer.parseInt(userIdStr));
            ctx.attribute("userEmail", email);
            return true;

        } catch (JWTVerificationException | NumberFormatException e) {
            ctx.status(401).json(Map.of("error", "未登录或登录已过期"));
            return false;
        }
    }
}
