package com.example.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class YcPointsRechargeOrder {
    private Long id;
    private String rechargeNo;
    private Long userId;
    private String username;
    private String displayName;
    private BigDecimal amount;
    private Integer pointsAmount;
    private String status;
    private LocalDateTime paymentMarkedAt;
    private LocalDateTime paymentReviewedAt;
    private Long paymentReviewBy;
    private String paymentReviewByName;
    private String paymentReviewRemark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRechargeNo() {
        return rechargeNo;
    }

    public void setRechargeNo(String rechargeNo) {
        this.rechargeNo = rechargeNo;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public Integer getPointsAmount() {
        return pointsAmount;
    }

    public void setPointsAmount(Integer pointsAmount) {
        this.pointsAmount = pointsAmount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getPaymentMarkedAt() {
        return paymentMarkedAt;
    }

    public void setPaymentMarkedAt(LocalDateTime paymentMarkedAt) {
        this.paymentMarkedAt = paymentMarkedAt;
    }

    public LocalDateTime getPaymentReviewedAt() {
        return paymentReviewedAt;
    }

    public void setPaymentReviewedAt(LocalDateTime paymentReviewedAt) {
        this.paymentReviewedAt = paymentReviewedAt;
    }

    public Long getPaymentReviewBy() {
        return paymentReviewBy;
    }

    public void setPaymentReviewBy(Long paymentReviewBy) {
        this.paymentReviewBy = paymentReviewBy;
    }

    public String getPaymentReviewByName() {
        return paymentReviewByName;
    }

    public void setPaymentReviewByName(String paymentReviewByName) {
        this.paymentReviewByName = paymentReviewByName;
    }

    public String getPaymentReviewRemark() {
        return paymentReviewRemark;
    }

    public void setPaymentReviewRemark(String paymentReviewRemark) {
        this.paymentReviewRemark = paymentReviewRemark;
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
