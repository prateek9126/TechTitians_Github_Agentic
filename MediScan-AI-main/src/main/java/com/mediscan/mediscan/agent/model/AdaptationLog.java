package com.mediscan.mediscan.agent.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Audit log recording incremental adaptation when a patient data source updates mid-session.
 */
public class AdaptationLog {

    private String timestamp;
    private String triggeredBy;
    private String sourceUpdated;
    private List<String> affectedFields = new ArrayList<>();
    private String priorStateSummary;
    private String newStateSummary;
    private String rationale;

    // Structured before/after diff fields
    private String oldNoteExcerpt;
    private String newNoteExcerpt;
    private String diffSummary;

    public AdaptationLog() {
    }

    public AdaptationLog(String timestamp, String triggeredBy, String sourceUpdated,
                         List<String> affectedFields, String priorStateSummary,
                         String newStateSummary, String rationale) {
        this.timestamp = timestamp;
        this.triggeredBy = triggeredBy;
        this.sourceUpdated = sourceUpdated;
        this.affectedFields = affectedFields;
        this.priorStateSummary = priorStateSummary;
        this.newStateSummary = newStateSummary;
        this.rationale = rationale;
    }

    public AdaptationLog(String timestamp, String triggeredBy, String sourceUpdated,
                         List<String> affectedFields, String priorStateSummary,
                         String newStateSummary, String rationale,
                         String oldNoteExcerpt, String newNoteExcerpt, String diffSummary) {
        this(timestamp, triggeredBy, sourceUpdated, affectedFields, priorStateSummary, newStateSummary, rationale);
        this.oldNoteExcerpt = oldNoteExcerpt;
        this.newNoteExcerpt = newNoteExcerpt;
        this.diffSummary = diffSummary;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getTriggeredBy() {
        return triggeredBy;
    }

    public void setTriggeredBy(String triggeredBy) {
        this.triggeredBy = triggeredBy;
    }

    // UI property alias
    public String getTriggerEvent() {
        return triggeredBy;
    }

    public String getSourceUpdated() {
        return sourceUpdated;
    }

    public void setSourceUpdated(String sourceUpdated) {
        this.sourceUpdated = sourceUpdated;
    }

    // UI property alias
    public String getSourceInterface() {
        return sourceUpdated;
    }

    public List<String> getAffectedFields() {
        return affectedFields;
    }

    public void setAffectedFields(List<String> affectedFields) {
        this.affectedFields = affectedFields;
    }

    public String getPriorStateSummary() {
        return priorStateSummary;
    }

    public void setPriorStateSummary(String priorStateSummary) {
        this.priorStateSummary = priorStateSummary;
    }

    // UI property alias
    public String getPriorState() {
        return priorStateSummary;
    }

    public String getNewStateSummary() {
        return newStateSummary;
    }

    public void setNewStateSummary(String newStateSummary) {
        this.newStateSummary = newStateSummary;
    }

    // UI property alias
    public String getNewState() {
        return newStateSummary;
    }

    public String getRationale() {
        return rationale;
    }

    public void setRationale(String rationale) {
        this.rationale = rationale;
    }

    public String getOldNoteExcerpt() {
        return oldNoteExcerpt;
    }

    public void setOldNoteExcerpt(String oldNoteExcerpt) {
        this.oldNoteExcerpt = oldNoteExcerpt;
    }

    public String getNewNoteExcerpt() {
        return newNoteExcerpt;
    }

    public void setNewNoteExcerpt(String newNoteExcerpt) {
        this.newNoteExcerpt = newNoteExcerpt;
    }

    public String getDiffSummary() {
        return diffSummary;
    }

    public void setDiffSummary(String diffSummary) {
        this.diffSummary = diffSummary;
    }
}
