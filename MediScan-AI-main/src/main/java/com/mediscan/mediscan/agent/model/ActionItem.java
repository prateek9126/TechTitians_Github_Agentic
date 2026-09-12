package com.mediscan.mediscan.agent.model;

/**
 * Actionable clinical follow-up task generated for healthcare staff or patient.
 * SYNTHETIC / DEIDENTIFIED DATA MODEL
 */
public class ActionItem {

    private String id;
    private String description;
    private String targetRole; // CLINICIAN, NURSE, PHARMACIST, PATIENT
    private String priority;   // HIGH, MEDIUM, ROUTINE
    private String category;   // MEDICATION_SAFETY, DIAGNOSTIC_FOLLOW_UP, PATIENT_EDUCATION, ALLERGY_VERIFICATION
    private String status;     // PENDING_CLINICIAN_REVIEW, COMPLETED

    public ActionItem() {
    }

    public ActionItem(String id, String description, String targetRole,
                      String priority, String category, String status) {
        this.id = id;
        this.description = description;
        this.targetRole = targetRole;
        this.priority = priority;
        this.category = category;
        this.status = status;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTargetRole() {
        return targetRole;
    }

    public void setTargetRole(String targetRole) {
        this.targetRole = targetRole;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
