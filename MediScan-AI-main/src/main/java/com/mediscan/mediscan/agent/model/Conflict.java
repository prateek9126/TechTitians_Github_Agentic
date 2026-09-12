package com.mediscan.mediscan.agent.model;

/**
 * Structured Conflict model representing a computed contradiction between patient data sources.
 */
public class Conflict extends ConflictItem {

    private String conflictType; // MEDICATION, ALLERGY, LAB, INTERACTION
    private boolean resolved;

    public Conflict() {
        super();
    }

    public Conflict(String id, String field, String sourceA, String valueA,
                    String sourceB, String valueB, String severity,
                    String status, String resolutionNotes, String resolvingTool,
                    String autonomousRationale) {
        super(id, field, sourceA, valueA, sourceB, valueB, severity, status, resolutionNotes, resolvingTool, autonomousRationale);
        this.resolved = "RESOLVED".equalsIgnoreCase(status);
    }

    public Conflict(String id, String field, String sourceA, String valueA,
                    String sourceB, String valueB, String severity,
                    String status, String resolutionNotes, String resolvingTool,
                    String autonomousRationale, String conflictType) {
        super(id, field, sourceA, valueA, sourceB, valueB, severity, status, resolutionNotes, resolvingTool, autonomousRationale);
        this.conflictType = conflictType;
        this.resolved = "RESOLVED".equalsIgnoreCase(status);
    }

    public String getConflictType() {
        return conflictType;
    }

    public void setConflictType(String conflictType) {
        this.conflictType = conflictType;
    }

    public boolean isResolved() {
        return resolved || "RESOLVED".equalsIgnoreCase(getStatus());
    }

    public void setResolved(boolean resolved) {
        this.resolved = resolved;
        if (resolved) {
            setStatus("RESOLVED");
        }
    }
}
