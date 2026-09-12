package com.mediscan.mediscan.agent.model;

/**
 * Represents a detected contradiction between two or more patient data sources.
 * SYNTHETIC / DEIDENTIFIED DATA MODEL
 */
public class ConflictItem {

    private String id;
    private String field;
    private String sourceA;
    private String valueA;
    private String sourceB;
    private String valueB;
    private String severity; // HIGH, MEDIUM, LOW
    private String status;   // DETECTED, RESOLVED, ESCALATED
    private String resolutionNotes;
    private String resolvingTool;
    private String autonomousRationale;
    private String conflictType;

    public ConflictItem() {
    }

    public ConflictItem(String id, String field, String sourceA, String valueA,
                        String sourceB, String valueB, String severity,
                        String status, String resolutionNotes, String resolvingTool,
                        String autonomousRationale) {
        this.id = id;
        this.field = field;
        this.sourceA = sourceA;
        this.valueA = valueA;
        this.sourceB = sourceB;
        this.valueB = valueB;
        this.severity = severity;
        this.status = status;
        this.resolutionNotes = resolutionNotes;
        this.resolvingTool = resolvingTool;
        this.autonomousRationale = autonomousRationale;
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

    public String getSourceA() {
        return sourceA;
    }

    public void setSourceA(String sourceA) {
        this.sourceA = sourceA;
    }

    public String getValueA() {
        return valueA;
    }

    public void setValueA(String valueA) {
        this.valueA = valueA;
    }

    public String getSourceB() {
        return sourceB;
    }

    public void setSourceB(String sourceB) {
        this.sourceB = sourceB;
    }

    public String getValueB() {
        return valueB;
    }

    public void setValueB(String valueB) {
        this.valueB = valueB;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getResolutionNotes() {
        return resolutionNotes;
    }

    public void setResolutionNotes(String resolutionNotes) {
        this.resolutionNotes = resolutionNotes;
    }

    public String getResolvingTool() {
        return resolvingTool;
    }

    public void setResolvingTool(String resolvingTool) {
        this.resolvingTool = resolvingTool;
    }

    public String getAutonomousRationale() {
        return autonomousRationale;
    }

    public void setAutonomousRationale(String autonomousRationale) {
        this.autonomousRationale = autonomousRationale;
    }

    public String getConflictType() {
        return conflictType;
    }

    public void setConflictType(String conflictType) {
        this.conflictType = conflictType;
    }

    public String getType() {
        return conflictType;
    }

    public void setType(String type) {
        this.conflictType = type;
    }

    public boolean isResolved() {
        return "RESOLVED".equalsIgnoreCase(status);
    }

    public void setResolved(boolean resolved) {
        if (resolved) {
            this.status = "RESOLVED";
        }
    }
}
