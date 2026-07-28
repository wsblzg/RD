package com.example.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class YcShopOrder {
    private Long id;
    private String orderNo;
    private Long userId;
    private String username;
    private String displayName;
    private BigDecimal totalAmount;
    private Integer totalQuantity;
    private String status;
    private String receiverName;
    private String receiverPhone;
    private String receiverAddress;
    private String buyerRemark;
    private LocalDateTime paymentMarkedAt;
    private LocalDateTime paymentReviewedAt;
    private Long paymentReviewBy;
    private String paymentReviewByName;
    private String paymentReviewRemark;
    private LocalDateTime shippedAt;
    private Long shippedBy;
    private String shippedByName;
    private String shippingCompany;
    private String trackingNo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
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

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public Integer getTotalQuantity() {
        return totalQuantity;
    }

    public void setTotalQuantity(Integer totalQuantity) {
        this.totalQuantity = totalQuantity;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getReceiverName() {
        return receiverName;
    }

    public void setReceiverName(String receiverName) {
        this.receiverName = receiverName;
    }

    public String getReceiverPhone() {
        return receiverPhone;
    }

    public void setReceiverPhone(String receiverPhone) {
        this.receiverPhone = receiverPhone;
    }

    public String getReceiverAddress() {
        return receiverAddress;
    }

    public void setReceiverAddress(String receiverAddress) {
        this.receiverAddress = receiverAddress;
    }

    public String getBuyerRemark() {
        return buyerRemark;
    }

    public void setBuyerRemark(String buyerRemark) {
        this.buyerRemark = buyerRemark;
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

    public LocalDateTime getShippedAt() {
        return shippedAt;
    }

    public void setShippedAt(LocalDateTime shippedAt) {
        this.shippedAt = shippedAt;
    }

    public Long getShippedBy() {
        return shippedBy;
    }

    public void setShippedBy(Long shippedBy) {
        this.shippedBy = shippedBy;
    }

    public String getShippedByName() {
        return shippedByName;
    }

    public void setShippedByName(String shippedByName) {
        this.shippedByName = shippedByName;
    }

    public String getShippingCompany() {
        return shippingCompany;
    }

    public void setShippingCompany(String shippingCompany) {
        this.shippingCompany = shippingCompany;
    }

    public String getTrackingNo() {
        return trackingNo;
    }

    public void setTrackingNo(String trackingNo) {
        this.trackingNo = trackingNo;
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
