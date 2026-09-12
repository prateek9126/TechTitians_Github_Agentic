package com.mediscan.mediscan.agent.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Structured follow-up documentation produced by the agent.
 * Subject to guardrails: strictly framed as clinician provisional review without diagnostic or prescriptive commands.
 * SYNTHETIC / DEIDENTIFIED DATA MODEL
 */
public class FollowUpRecord {

    private String patientId;
    private String patientName;
    private String encounterDate;
    private String chiefComplaint;
    private String historyOfPresentIllness;
    private String objectiveFindingsSummary;
    private List<String> reconciledMedications = new ArrayList<>();
    private List<String> allergyAlerts = new ArrayList<>();
    private String assessmentSummary;
    private String proposedFollowUpPlan;
    private String guardrailComplianceNotice = "PASSED: Content verified against diagnostic/prescriptive guardrail filters.";
    private String disclaimer = "Draft for clinician review — not a diagnosis. Synthetic/deidentified data only.";

    public FollowUpRecord() {
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

    public String getEncounterDate() {
        return encounterDate;
    }

    public void setEncounterDate(String encounterDate) {
        this.encounterDate = encounterDate;
    }

    public String getChiefComplaint() {
        return chiefComplaint;
    }

    public void setChiefComplaint(String chiefComplaint) {
        this.chiefComplaint = chiefComplaint;
    }

    public String getHistoryOfPresentIllness() {
        return historyOfPresentIllness;
    }

    public void setHistoryOfPresentIllness(String historyOfPresentIllness) {
        this.historyOfPresentIllness = historyOfPresentIllness;
    }

    public String getObjectiveFindingsSummary() {
        return objectiveFindingsSummary;
    }

    public void setObjectiveFindingsSummary(String objectiveFindingsSummary) {
        this.objectiveFindingsSummary = objectiveFindingsSummary;
    }

    public List<String> getReconciledMedications() {
        return reconciledMedications;
    }

    public void setReconciledMedications(List<String> reconciledMedications) {
        this.reconciledMedications = reconciledMedications;
    }

    public List<String> getAllergyAlerts() {
        return allergyAlerts;
    }

    public void setAllergyAlerts(List<String> allergyAlerts) {
        this.allergyAlerts = allergyAlerts;
    }

    public String getAssessmentSummary() {
        return assessmentSummary;
    }

    public void setAssessmentSummary(String assessmentSummary) {
        this.assessmentSummary = assessmentSummary;
    }

    public String getProposedFollowUpPlan() {
        return proposedFollowUpPlan;
    }

    public void setProposedFollowUpPlan(String proposedFollowUpPlan) {
        this.proposedFollowUpPlan = proposedFollowUpPlan;
    }

    public String getGuardrailComplianceNotice() {
        return guardrailComplianceNotice;
    }

    public void setGuardrailComplianceNotice(String guardrailComplianceNotice) {
        this.guardrailComplianceNotice = guardrailComplianceNotice;
    }

    public String getDisclaimer() {
        return disclaimer;
    }

    public void setDisclaimer(String disclaimer) {
        this.disclaimer = disclaimer;
    }
}
