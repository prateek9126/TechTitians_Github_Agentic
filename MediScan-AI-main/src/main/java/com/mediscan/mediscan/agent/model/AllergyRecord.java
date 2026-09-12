package com.mediscan.mediscan.agent.model;

/**
 * Allergy and adverse drug reaction entry.
 * SYNTHETIC / DEIDENTIFIED DATA MODEL
 */
public class AllergyRecord {

    private String id;
    private String allergen;
    private String allergenType; // DRUG, FOOD, ENVIRONMENTAL
    private String reaction;
    private String severity;     // MILD, MODERATE, SEVERE
    private String status;       // ACTIVE, RESOLVED, REPORTED_NKDA
    private String recordedDate;

    public AllergyRecord() {
    }

    public AllergyRecord(String id, String allergen, String allergenType,
                         String reaction, String severity, String status,
                         String recordedDate) {
        this.id = id;
        this.allergen = allergen;
        this.allergenType = allergenType;
        this.reaction = reaction;
        this.severity = severity;
        this.status = status;
        this.recordedDate = recordedDate;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAllergen() {
        return allergen;
    }

    public void setAllergen(String allergen) {
        this.allergen = allergen;
    }

    public String getAllergenType() {
        return allergenType;
    }

    public void setAllergenType(String allergenType) {
        this.allergenType = allergenType;
    }

    public String getReaction() {
        return reaction;
    }

    public void setReaction(String reaction) {
        this.reaction = reaction;
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

    public String getRecordedDate() {
        return recordedDate;
    }

    public void setRecordedDate(String recordedDate) {
        this.recordedDate = recordedDate;
    }
}
