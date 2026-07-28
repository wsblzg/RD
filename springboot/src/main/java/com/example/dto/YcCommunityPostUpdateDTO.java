package com.example.dto;

import java.util.List;

public class YcCommunityPostUpdateDTO {
    private String title;
    private String contentHtml;
    private String summary;
    private String category;
    private List<String> tags;
    private List<String> imageUrls;
    private Long aiWorkId;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContentHtml() {
        return contentHtml;
    }

    public void setContentHtml(String contentHtml) {
        this.contentHtml = contentHtml;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public List<String> getImageUrls() {
        return imageUrls;
    }

    public void setImageUrls(List<String> imageUrls) {
        this.imageUrls = imageUrls;
    }

    public Long getAiWorkId() {
        return aiWorkId;
    }

    public void setAiWorkId(Long aiWorkId) {
        this.aiWorkId = aiWorkId;
    }
}
