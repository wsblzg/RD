package com.example.dto;

import java.util.List;

public class YcKilnGuideConfigUpdateDTO {
    private List<YcKilnGuideHotspotDTO> hotspots;
    private List<YcKilnGuideStepDTO> steps;

    public List<YcKilnGuideHotspotDTO> getHotspots() {
        return hotspots;
    }

    public void setHotspots(List<YcKilnGuideHotspotDTO> hotspots) {
        this.hotspots = hotspots;
    }

    public List<YcKilnGuideStepDTO> getSteps() {
        return steps;
    }

    public void setSteps(List<YcKilnGuideStepDTO> steps) {
        this.steps = steps;
    }
}
