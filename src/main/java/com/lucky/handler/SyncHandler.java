package com.lucky.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.lucky.db.Database;
import com.lucky.middleware.AuthMiddleware;

import io.javalin.http.Context;

import java.sql.*;
import java.util.*;

/**
 * 同步接口处理
 * - POST /api/sync/upload   上传变更
 * - GET  /api/sync/download 下载变更
 */
public class SyncHandler {

    /**
     * 上传变更
     * POST /api/sync/upload
     * Headers: Authorization: Bearer <token>
     * Body: { "transactions": [ ... ] }
     */
    public static void upload(Context ctx) {
        // JWT 鉴权
        if (!AuthMiddleware.verify(ctx)) return;

        int userId = ctx.<Integer>attribute("userId");

        JsonNode body;
        try {
            body = ctx.bodyAsClass(JsonNode.class);
        } catch (Exception e) {
            ctx.status(400).json(Map.of("error", "请求格式错误"));
            return;
        }

        JsonNode transactions = body.get("transactions");

        if (transactions == null || !transactions.isArray() || transactions.size() == 0) {
            ctx.status(400).json(Map.of("error", "同步数据不能为空"));
            return;
        }

        List<Map<String, Object>> results = new ArrayList<>();
        long now = System.currentTimeMillis();

        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);

            PreparedStatement selectPs = conn.prepareStatement(
                "SELECT updated_at FROM transactions WHERE server_id = ? AND user_id = ?"
            );
            PreparedStatement insertPs = conn.prepareStatement(
                "INSERT INTO transactions (server_id, user_id, amount, note, date, created_at, updated_at, deleted_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)"
            );
            PreparedStatement updatePs = conn.prepareStatement(
                "UPDATE transactions SET amount=?, note=?, date=?, updated_at=?, deleted_at=? WHERE server_id=? AND user_id=?"
            );

            for (int i = 0; i < transactions.size(); i++) {
                JsonNode txn = transactions.get(i);

                // 解析字段
                String serverId = (txn.has("serverId") && !txn.get("serverId").isNull())
                    ? txn.get("serverId").asText() : null;
                double amount = txn.get("amount").asDouble();
                String note = txn.has("note") ? txn.get("note").asText() : "";
                String date = txn.get("date").asText();
                long createdAt = txn.get("createdAt").asLong();
                long updatedAt = txn.get("updatedAt").asLong();
                Long deletedAt = (txn.has("deletedAt") && !txn.get("deletedAt").isNull())
                    ? txn.get("deletedAt").asLong() : null;

                // 基本校验
                if (amount == 0) {
                    ctx.status(400).json(Map.of("error", "数据格式错误：amount 不能为 0"));
                    conn.rollback();
                    return;
                }

                if (serverId == null) {
                    // 新建 → INSERT
                    String newId = UUID.randomUUID().toString();
                    insertPs.setString(1, newId);
                    insertPs.setInt(2, userId);
                    insertPs.setDouble(3, amount);
                    insertPs.setString(4, note);
                    insertPs.setString(5, date);
                    insertPs.setLong(6, createdAt);
                    insertPs.setLong(7, updatedAt);
                    if (deletedAt != null) {
                        insertPs.setLong(8, deletedAt);
                    } else {
                        insertPs.setNull(8, Types.INTEGER);
                    }
                    insertPs.executeUpdate();

                    results.add(Map.of("serverId", newId, "updatedAt", updatedAt));

                } else {
                    // 已存在 → 检查是否需更新（Last-write-wins）
                    selectPs.setString(1, serverId);
                    selectPs.setInt(2, userId);
                    ResultSet rs = selectPs.executeQuery();

                    if (rs.next()) {
                        long dbUpdatedAt = rs.getLong("updated_at");

                        if (updatedAt > dbUpdatedAt) {
                            // 前端版本更新 → UPDATE
                            updatePs.setDouble(1, amount);
                            updatePs.setString(2, note);
                            updatePs.setString(3, date);
                            updatePs.setLong(4, updatedAt);
                            if (deletedAt != null) {
                                updatePs.setLong(5, deletedAt);
                            } else {
                                updatePs.setNull(5, Types.INTEGER);
                            }
                            updatePs.setString(6, serverId);
                            updatePs.setInt(7, userId);
                            updatePs.executeUpdate();
                        }
                        // 以较大时间戳为准
                        results.add(Map.of("serverId", serverId, "updatedAt", Math.max(updatedAt, dbUpdatedAt)));

                    } else {
                        // serverId 不在该用户名下 → INSERT
                        insertPs.setString(1, serverId);
                        insertPs.setInt(2, userId);
                        insertPs.setDouble(3, amount);
                        insertPs.setString(4, note);
                        insertPs.setString(5, date);
                        insertPs.setLong(6, createdAt);
                        insertPs.setLong(7, updatedAt);
                        if (deletedAt != null) {
                            insertPs.setLong(8, deletedAt);
                        } else {
                            insertPs.setNull(8, Types.INTEGER);
                        }
                        insertPs.executeUpdate();

                        results.add(Map.of("serverId", serverId, "updatedAt", updatedAt));
                    }
                }
            }

            conn.commit();

            System.out.println("[Sync] 上传完成: user=" + userId + " count=" + transactions.size());

            ctx.json(Map.of("data", Map.of(
                "results", results,
                "serverTime", now
            )));

        } catch (SQLException e) {
            e.printStackTrace();
            ctx.status(500).json(Map.of("error", "服务器错误"));
        }
    }

    /**
     * 下载变更
     * GET /api/sync/download?since={lastSyncTime}
     * Headers: Authorization: Bearer <token>
     */
    public static void download(Context ctx) {
        // JWT 鉴权
        if (!AuthMiddleware.verify(ctx)) return;

        int userId = ctx.<Integer>attribute("userId");

        String sinceStr = ctx.queryParam("since");
        long since;
        try {
            since = Long.parseLong(sinceStr);
        } catch (NumberFormatException e) {
            ctx.status(400).json(Map.of("error", "since 参数格式错误"));
            return;
        }

        long now = System.currentTimeMillis();

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "SELECT server_id, amount, note, date, created_at, updated_at, deleted_at " +
                 "FROM transactions WHERE user_id = ? AND updated_at > ? ORDER BY updated_at ASC"
             )) {

            ps.setInt(1, userId);
            ps.setLong(2, since);
            ResultSet rs = ps.executeQuery();

            List<Map<String, Object>> transactions = new ArrayList<>();
            while (rs.next()) {
                Map<String, Object> txn = new LinkedHashMap<>();
                txn.put("serverId", rs.getString("server_id"));
                txn.put("amount", rs.getDouble("amount"));
                txn.put("note", rs.getString("note"));
                txn.put("date", rs.getString("date"));
                txn.put("createdAt", rs.getLong("created_at"));
                txn.put("updatedAt", rs.getLong("updated_at"));
                long deletedAt = rs.getLong("deleted_at");
                txn.put("deletedAt", rs.wasNull() ? null : deletedAt);
                transactions.add(txn);
            }

            System.out.println("[Sync] 下载完成: user=" + userId + " since=" + since + " count=" + transactions.size());

            ctx.json(Map.of("data", Map.of(
                "transactions", transactions,
                "serverTime", now
            )));

        } catch (SQLException e) {
            e.printStackTrace();
            ctx.status(500).json(Map.of("error", "服务器错误"));
        }
        }
}
