package com.mediscan.mediscan.agent.model;

/**
 * Represents a missing, ambiguous, or incomplete piece of clinical information.
 * SYNTHETIC / DEIDENTIFIED DATA MODEL
 */
public class GapItem {

    private String id;
    private String field;
    private String description;
    private String impact;
    private String status; // DETECTED, ADDRESSED, ESCALATED
    private String recommendation;
    private String resolvingTool;

    public GapItem() {
    }

    public GapItem(String id, String field, String description, String impact,
                   String status, String recommendation, String resolvingTool) {
        this.id = id;
        this.field = field;
        this.description = description;
        this.impact = impact;
        this.status = status;
        this.recommendation = recommendation;
        this.resolvingTool = resolvingTool;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getImpact() {
        return impact;
    }

    public void setImpact(String impact) {
        this.impact = impact;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRecommendation() {
        return recommendation;
    }

    public void setRecommendation(String recommendation) {
        this.recommendation = recommendation;
    }

    public String getResolvingTool() {
        return resolvingTool;
    }

    public void setResolvingTool(String resolvingTool) {
        this.resolvingTool = resolvingTool;
    }
}
