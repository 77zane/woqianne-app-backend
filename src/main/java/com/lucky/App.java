package com.lucky;

import com.lucky.config.Config;
import com.lucky.handler.AuthHandler;
import com.lucky.handler.SyncHandler;
import com.lucky.middleware.AuthMiddleware;

import io.javalin.Javalin;
import io.javalin.http.HttpStatus;

import java.util.Map;

/**
 * 应用入口
 */
public class App {

    public static void main(String[] args) {
        Javalin app = Javalin.create(config -> {
            // CORS 配置
            config.bundledPlugins.enableCors(cors -> {
                cors.addRule(rule -> {
                    rule.anyHost();
                    rule.allowCredentials = false;
                });
            });

            // 全局 404 / 500 处理
            config.router.apiBuilder(() -> {
                // 路由在主 app 中注册
            });

        }).start(Config.PORT);

        // ========== 认证接口（无需鉴权）==========
        app.post("/api/auth/send-code", AuthHandler::sendCode);
        app.post("/api/auth/login", AuthHandler::login);

        // ========== 同步接口（需鉴权，鉴权在 Handler 内部执行）==========
        app.post("/api/sync/upload", SyncHandler::upload);
        app.get("/api/sync/download", SyncHandler::download);

        // ========== 健康检查 ==========
        app.get("/api/health", ctx -> {
            ctx.json(Map.of("status", "ok"));
        });

        // ========== 全局异常处理 ==========
        app.exception(Exception.class, (e, ctx) -> {
            e.printStackTrace();
            if (ctx.status().getCode() == 200) {
                ctx.status(500);
            }
            ctx.json(Map.of("error", "服务器内部错误"));
        });

        System.out.println("=============================================");
        System.out.println("  Lucky Server 已启动");
        System.out.println("  端口: " + Config.PORT);
        System.out.println("  模式: " + (Config.isDevMode() ? "开发（验证码控制台输出）" : "生产"));
        System.out.println("  API: http://localhost:" + Config.PORT + "/api");
        System.out.println("=============================================");
    }
}
