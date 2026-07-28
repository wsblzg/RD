package com.example.dto;

public class YcCreateRedeemCodeDTO {
    private String code;
    private Long itemId;
    private String issuedChannel;
    private String expireAt;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }

    public String getIssuedChannel() {
        return issuedChannel;
    }

    public void setIssuedChannel(String issuedChannel) {
        this.issuedChannel = issuedChannel;
    }

    public String getExpireAt() {
        return expireAt;
    }

    public void setExpireAt(String expireAt) {
        this.expireAt = expireAt;
    }
}
