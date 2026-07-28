package com.example.dto;

import java.util.List;

public class YcKilnGuideHotspotDTO {
    private String hotspotCode;
    private String name;
    private String shortLabel;
    private String position;
    private String normal;
    private String focusOrbit;
    private String focusTarget;
    private String summary;
    private List<String> points;
    private List<Integer> temperatureCurve;
    private Integer sortNo;
    private Integer status;

    public String getHotspotCode() {
        return hotspotCode;
    }

    public void setHotspotCode(String hotspotCode) {
        this.hotspotCode = hotspotCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getShortLabel() {
        return shortLabel;
    }

    public void setShortLabel(String shortLabel) {
        this.shortLabel = shortLabel;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public String getNormal() {
        return normal;
    }

    public void setNormal(String normal) {
        this.normal = normal;
    }

    public String getFocusOrbit() {
        return focusOrbit;
    }

    public void setFocusOrbit(String focusOrbit) {
        this.focusOrbit = focusOrbit;
    }

    public String getFocusTarget() {
        return focusTarget;
    }

    public void setFocusTarget(String focusTarget) {
        this.focusTarget = focusTarget;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public List<String> getPoints() {
        return points;
    }

    public void setPoints(List<String> points) {
        this.points = points;
    }

    public List<Integer> getTemperatureCurve() {
        return temperatureCurve;
    }

    public void setTemperatureCurve(List<Integer> temperatureCurve) {
        this.temperatureCurve = temperatureCurve;
    }

    public Integer getSortNo() {
        return sortNo;
    }

    public void setSortNo(Integer sortNo) {
        this.sortNo = sortNo;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
