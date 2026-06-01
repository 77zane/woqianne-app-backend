package com.lucky.model;

/**
 * 交易记录数据类
 */
public class Transaction {
    private String serverId;
    private int userId;
    private double amount;
    private String note;
    private String date;
    private long createdAt;
    private long updatedAt;
    private Long deletedAt;

    public Transaction() {}

    public Transaction(String serverId, int userId, double amount, String note,
                       String date, long createdAt, long updatedAt, Long deletedAt) {
        this.serverId = serverId;
        this.userId = userId;
        this.amount = amount;
        this.note = note;
        this.date = date;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
    }

    public String getServerId() { return serverId; }
    public void setServerId(String serverId) { this.serverId = serverId; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    public Long getDeletedAt() { return deletedAt; }
    public void setDeletedAt(Long deletedAt) { this.deletedAt = deletedAt; }
}
