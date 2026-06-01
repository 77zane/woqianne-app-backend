package com.lucky.db;

import com.lucky.config.Config;

import java.io.File;
import java.sql.*;

/**
 * SQLite 数据库初始化 + 连接管理
 */
public class Database {
    private static final String URL = "jdbc:sqlite:" + Config.DB_PATH;

    static {
        // 确保 data 目录存在
        File dataDir = new File("data");
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }

        // 初始化表结构
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute("PRAGMA journal_mode=WAL");
            stmt.execute("PRAGMA foreign_keys=ON");

            // 用户表
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    id         INTEGER PRIMARY KEY AUTOINCREMENT,
                    email      TEXT    NOT NULL UNIQUE,
                    created_at INTEGER NOT NULL
                )
            """);

            // 验证码表
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS verify_codes (
                    id         INTEGER PRIMARY KEY AUTOINCREMENT,
                    email      TEXT    NOT NULL,
                    code       TEXT    NOT NULL,
                    expires_at INTEGER NOT NULL,
                    used_at    INTEGER,
                    created_at INTEGER NOT NULL
                )
            """);

            stmt.execute("""
                CREATE INDEX IF NOT EXISTS idx_verify_codes_email
                    ON verify_codes(email)
            """);

            // 交易记录表
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS transactions (
                    server_id  TEXT    PRIMARY KEY,
                    user_id    INTEGER NOT NULL,
                    amount     REAL    NOT NULL,
                    note       TEXT    DEFAULT '',
                    date       TEXT    NOT NULL,
                    created_at INTEGER NOT NULL,
                    updated_at INTEGER NOT NULL,
                    deleted_at INTEGER,
                    FOREIGN KEY (user_id) REFERENCES users(id)
                )
            """);

            stmt.execute("""
                CREATE INDEX IF NOT EXISTS idx_transactions_user_updated
                    ON transactions(user_id, updated_at)
            """);

            System.out.println("[Database] 数据库初始化完成: " + Config.DB_PATH);

        } catch (SQLException e) {
            throw new RuntimeException("数据库初始化失败", e);
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL);
    }
}
