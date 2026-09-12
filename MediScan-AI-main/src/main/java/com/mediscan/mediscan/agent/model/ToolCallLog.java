package com.mediscan.mediscan.agent.model;

/**
 * Transparent audit log entry for tool/environment interaction.
 * Captures tool name, autonomous decision rationale, input, output, and latency.
 */
public class ToolCallLog {

    private String id;
    private String toolName;
    private String callReason;
    private String inputSummary;
    private String outputSummary;
    private String timestamp;
    private long durationMs;
    private String status; // SUCCESS, RETRY, FAILED
    private String errorDetails;
    private String displayName;

    public ToolCallLog() {
    }

    public ToolCallLog(String id, String toolName, String callReason,
                       String inputSummary, String outputSummary, String timestamp,
                       long durationMs, String status, String errorDetails) {
        this.id = id;
        this.toolName = toolName;
        this.callReason = callReason;
        this.inputSummary = inputSummary;
        this.outputSummary = outputSummary;
        this.timestamp = timestamp;
        this.durationMs = durationMs;
        this.status = status;
        this.errorDetails = errorDetails;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getToolName() {
        return toolName;
    }

    public void setToolName(String toolName) {
        this.toolName = toolName;
    }

    public String getCallReason() {
        return callReason;
    }

    public void setCallReason(String callReason) {
        this.callReason = callReason;
    }

    public String getInputSummary() {
        return inputSummary;
    }

    public void setInputSummary(String inputSummary) {
        this.inputSummary = inputSummary;
    }

    public String getOutputSummary() {
        return outputSummary;
    }

    public void setOutputSummary(String outputSummary) {
        this.outputSummary = outputSummary;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(long durationMs) {
        this.durationMs = durationMs;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getErrorDetails() {
        return errorDetails;
    }

    public void setErrorDetails(String errorDetails) {
        this.errorDetails = errorDetails;
    }

    public String getDisplayName() {
        if (displayName != null && !displayName.isBlank()) {
            return displayName;
        }
        if ("PatientRecordLookupService".equals(toolName)) return "Fetching Patient File";
        if ("MedicationAllergyLookupService".equals(toolName)) return "Checking Medicine Safety";
        if ("ClinicalGuidelineRAGService".equals(toolName)) return "Looking Up Medical Guidelines";
        if ("LabRetrievalService".equals(toolName)) return "Pulling Latest Lab Report";
        if ("RecordValidationService".equals(toolName)) return "Double-Checking the Report";
        if (toolName != null && toolName.startsWith("LabRetrievalService")) {
            return toolName.replace("LabRetrievalService", "Pulling Latest Lab Report");
        }
        return toolName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
}
