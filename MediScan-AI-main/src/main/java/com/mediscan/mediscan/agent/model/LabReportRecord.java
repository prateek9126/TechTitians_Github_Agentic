package com.mediscan.mediscan.agent.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Diagnostic laboratory panel report.
 * SYNTHETIC / DEIDENTIFIED DATA MODEL
 */
public class LabReportRecord {

    private String reportId;
    private String testDate;
    private String panelName; // e.g. "Comprehensive Metabolic Panel", "Complete Blood Count", "HbA1c"
    private List<LabResultItem> results = new ArrayList<>();
    private String notes;

    public LabReportRecord() {
    }

    public LabReportRecord(String reportId, String testDate, String panelName,
                           List<LabResultItem> results, String notes) {
        this.reportId = reportId;
        this.testDate = testDate;
        this.panelName = panelName;
        this.results = results;
        this.notes = notes;
    }

    public String getReportId() {
        return reportId;
    }

    public void setReportId(String reportId) {
        this.reportId = reportId;
    }

    public String getTestDate() {
        return testDate;
    }

    public void setTestDate(String testDate) {
        this.testDate = testDate;
    }

    public String getPanelName() {
        return panelName;
    }

    public void setPanelName(String panelName) {
        this.panelName = panelName;
    }

    public List<LabResultItem> getResults() {
        return results;
    }

    public void setResults(List<LabResultItem> results) {
        this.results = results;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
