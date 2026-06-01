package com.lucky.model;

/**
 * 验证码数据类
 */
public class VerifyCode {
    private int id;
    private String email;
    private String code;
    private long expiresAt;
    private Long usedAt;
    private long createdAt;

    public VerifyCode() {}

    public VerifyCode(int id, String email, String code, long expiresAt, Long usedAt, long createdAt) {
        this.id = id;
        this.email = email;
        this.code = code;
        this.expiresAt = expiresAt;
        this.usedAt = usedAt;
        this.createdAt = createdAt;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public long getExpiresAt() { return expiresAt; }
    public void setExpiresAt(long expiresAt) { this.expiresAt = expiresAt; }

    public Long getUsedAt() { return usedAt; }
    public void setUsedAt(Long usedAt) { this.usedAt = usedAt; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
