package com.lucky.model;

/**
 * 用户数据类
 */
public class User {
    private int id;
    private String email;
    private long createdAt;

    public User() {}

    public User(int id, String email, long createdAt) {
        this.id = id;
        this.email = email;
        this.createdAt = createdAt;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
