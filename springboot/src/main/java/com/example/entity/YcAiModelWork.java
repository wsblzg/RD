package com.example.entity;

import java.time.LocalDateTime;

public class YcAiModelWork {
    private Long id;
    private Long userId;
    private String taskId;
    private String authorName;
    private String workCode;
    private String title;
    private String prompt;
    private String style;
    private String vessel;
    private String coverUrl;
    private String modelUrl;
    private String modelFormat;
    private String ossUrl;
    private String generationStatus;
    private String storageStatus;
    private LocalDateTime generatedAt;
    private LocalDateTime expiresAt;
    private LocalDateTime persistStartedAt;
    private LocalDateTime persistedAt;
    private Integer generationPointsCost;
    private Integer persistPointsCost;
    private String generationChargeStatus;
    private String persistChargeStatus;
    private Long modelSizeBytes;
    private String lastError;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }
    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }
    public String getWorkCode() { return workCode; }
    public void setWorkCode(String workCode) { this.workCode = workCode; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getPrompt() { return prompt; }
    public void setPrompt(String prompt) { this.prompt = prompt; }
    public String getStyle() { return style; }
    public void setStyle(String style) { this.style = style; }
    public String getVessel() { return vessel; }
    public void setVessel(String vessel) { this.vessel = vessel; }
    public String getCoverUrl() { return coverUrl; }
    public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }
    public String getModelUrl() { return modelUrl; }
    public void setModelUrl(String modelUrl) { this.modelUrl = modelUrl; }
    public String getModelFormat() { return modelFormat; }
    public void setModelFormat(String modelFormat) { this.modelFormat = modelFormat; }
    public String getOssUrl() { return ossUrl; }
    public void setOssUrl(String ossUrl) { this.ossUrl = ossUrl; }
    public String getGenerationStatus() { return generationStatus; }
    public void setGenerationStatus(String generationStatus) { this.generationStatus = generationStatus; }
    public String getStorageStatus() { return storageStatus; }
    public void setStorageStatus(String storageStatus) { this.storageStatus = storageStatus; }
    public LocalDateTime getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    public LocalDateTime getPersistStartedAt() { return persistStartedAt; }
    public void setPersistStartedAt(LocalDateTime persistStartedAt) { this.persistStartedAt = persistStartedAt; }
    public LocalDateTime getPersistedAt() { return persistedAt; }
    public void setPersistedAt(LocalDateTime persistedAt) { this.persistedAt = persistedAt; }
    public Integer getGenerationPointsCost() { return generationPointsCost; }
    public void setGenerationPointsCost(Integer generationPointsCost) { this.generationPointsCost = generationPointsCost; }
    public Integer getPersistPointsCost() { return persistPointsCost; }
    public void setPersistPointsCost(Integer persistPointsCost) { this.persistPointsCost = persistPointsCost; }
    public String getGenerationChargeStatus() { return generationChargeStatus; }
    public void setGenerationChargeStatus(String generationChargeStatus) { this.generationChargeStatus = generationChargeStatus; }
    public String getPersistChargeStatus() { return persistChargeStatus; }
    public void setPersistChargeStatus(String persistChargeStatus) { this.persistChargeStatus = persistChargeStatus; }
    public Long getModelSizeBytes() { return modelSizeBytes; }
    public void setModelSizeBytes(Long modelSizeBytes) { this.modelSizeBytes = modelSizeBytes; }
    public String getLastError() { return lastError; }
    public void setLastError(String lastError) { this.lastError = lastError; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
