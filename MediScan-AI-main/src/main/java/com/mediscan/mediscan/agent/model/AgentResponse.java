package com.mediscan.mediscan.agent.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Top-level response object returned by the Clinical Documentation & Follow-up Agent.
 * SYNTHETIC / DEIDENTIFIED DATA MODEL
 */
public class AgentResponse {

    private String overallStatus; // VERIFIED, VERIFIED_WITH_ESCALATIONS, ESCALATED_FAILURE
    private String patientId;
    private String patientName;
    private FollowUpRecord followUpRecord;
    private List<ActionItem> actionItems = new ArrayList<>();
    private List<ConflictItem> conflicts = new ArrayList<>();
    private List<GapItem> gaps = new ArrayList<>();
    private List<ToolCallLog> toolCallLogs = new ArrayList<>();
    private ValidationResult validationResult;
    private AdaptationLog adaptationLog;
    private double confidenceScore;
    private String executionTimestamp;
    private String disclaimer = "Draft for clinician review — not a diagnosis. Synthetic/deidentified data only.";

    public AgentResponse() {
    }

    public String getOverallStatus() {
        return overallStatus;
    }

    public void setOverallStatus(String overallStatus) {
        this.overallStatus = overallStatus;
    }

    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public FollowUpRecord getFollowUpRecord() {
        return followUpRecord;
    }

    public void setFollowUpRecord(FollowUpRecord followUpRecord) {
        this.followUpRecord = followUpRecord;
    }

    public List<ActionItem> getActionItems() {
        return actionItems;
    }

    public void setActionItems(List<ActionItem> actionItems) {
        this.actionItems = actionItems;
    }

    public List<ConflictItem> getConflicts() {
        return conflicts;
    }

    public void setConflicts(List<ConflictItem> conflicts) {
        this.conflicts = conflicts;
    }

    public List<GapItem> getGaps() {
        return gaps;
    }

    public void setGaps(List<GapItem> gaps) {
        this.gaps = gaps;
    }

    public List<ToolCallLog> getToolCallLogs() {
        return toolCallLogs;
    }

    public void setToolCallLogs(List<ToolCallLog> toolCallLogs) {
        this.toolCallLogs = toolCallLogs;
    }

    public ValidationResult getValidationResult() {
        return validationResult;
    }

    public void setValidationResult(ValidationResult validationResult) {
        this.validationResult = validationResult;
    }

    public AdaptationLog getAdaptationLog() {
        return adaptationLog;
    }

    public void setAdaptationLog(AdaptationLog adaptationLog) {
        this.adaptationLog = adaptationLog;
    }

    public double getConfidenceScore() {
        return confidenceScore;
    }

    public void setConfidenceScore(double confidenceScore) {
        this.confidenceScore = confidenceScore;
    }

    public String getExecutionTimestamp() {
        return executionTimestamp;
    }

    public void setExecutionTimestamp(String executionTimestamp) {
        this.executionTimestamp = executionTimestamp;
    }

    public String getDisclaimer() {
        return disclaimer;
    }

    public void setDisclaimer(String disclaimer) {
        this.disclaimer = disclaimer;
    }
}
