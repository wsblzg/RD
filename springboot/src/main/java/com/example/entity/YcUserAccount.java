package com.example.entity;

import java.time.LocalDateTime;

public class YcUserAccount {
    private Long id;
    private String username;
    private String passwordHash;
    private String displayName;
    private String role;
    private Integer status;
    private Integer pointsBalance;
    private Integer pointsTotalRecharged;
    private Integer pointsTotalSpent;
    private Integer pointsIsUnlimited;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Integer getPointsBalance() {
        return pointsBalance;
    }

    public void setPointsBalance(Integer pointsBalance) {
        this.pointsBalance = pointsBalance;
    }

    public Integer getPointsTotalRecharged() {
        return pointsTotalRecharged;
    }

    public void setPointsTotalRecharged(Integer pointsTotalRecharged) {
        this.pointsTotalRecharged = pointsTotalRecharged;
    }

    public Integer getPointsTotalSpent() {
        return pointsTotalSpent;
    }

    public void setPointsTotalSpent(Integer pointsTotalSpent) {
        this.pointsTotalSpent = pointsTotalSpent;
    }

    public Integer getPointsIsUnlimited() {
        return pointsIsUnlimited;
    }

    public void setPointsIsUnlimited(Integer pointsIsUnlimited) {
        this.pointsIsUnlimited = pointsIsUnlimited;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
