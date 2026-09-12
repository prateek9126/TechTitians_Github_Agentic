package com.mediscan.mediscan.agent.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Structured PatientRecord model representing patient EHR data, notes, labs, and transcript.
 * SYNTHETIC / DEIDENTIFIED DATA MODEL
 */
public class PatientRecord {

    private String patientId;
    private String name;
    private int age;
    private String gender;
    private String transcriptText;
    private List<Medication> ehrMedications = new ArrayList<>();
    private List<String> ehrAllergies = new ArrayList<>();
    private List<LabResult> labResults = new ArrayList<>();
    private List<ClinicalNote> previousNotes = new ArrayList<>();

    // Metadata for UI presentation
    private String scenarioTitle;
    private String scenarioDescription;
    private String deliberateContradictionDescription;
    private String syntheticNotice = "SYNTHETIC / DEIDENTIFIED DATA - FOR EVALUATION & DEMONSTRATION PURPOSES ONLY";

    public PatientRecord() {
    }

    public PatientRecord(String patientId, String name, int age, String gender, String transcriptText) {
        this.patientId = patientId;
        this.name = name;
        this.age = age;
        this.gender = gender;
        this.transcriptText = transcriptText;
    }

    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getTranscriptText() {
        return transcriptText;
    }

    public void setTranscriptText(String transcriptText) {
        this.transcriptText = transcriptText;
    }

    // Alias for existing codebase and UI bindings
    public String getConsultationTranscript() {
        return transcriptText;
    }

    public void setConsultationTranscript(String transcript) {
        this.transcriptText = transcript;
    }

    public List<Medication> getEhrMedications() {
        return ehrMedications;
    }

    public void setEhrMedications(List<Medication> ehrMedications) {
        this.ehrMedications = ehrMedications != null ? ehrMedications : new ArrayList<>();
    }

    public List<String> getEhrAllergies() {
        return ehrAllergies;
    }

    public void setEhrAllergies(List<String> ehrAllergies) {
        this.ehrAllergies = ehrAllergies != null ? ehrAllergies : new ArrayList<>();
    }

    public List<LabResult> getLabResults() {
        return labResults;
    }

    public void setLabResults(List<LabResult> labResults) {
        this.labResults = labResults != null ? labResults : new ArrayList<>();
    }

    public List<ClinicalNote> getPreviousNotes() {
        return previousNotes;
    }

    public void setPreviousNotes(List<ClinicalNote> previousNotes) {
        this.previousNotes = previousNotes != null ? previousNotes : new ArrayList<>();
    }

    public String getScenarioTitle() {
        return scenarioTitle;
    }

    public void setScenarioTitle(String scenarioTitle) {
        this.scenarioTitle = scenarioTitle;
    }

    public String getScenarioDescription() {
        return scenarioDescription;
    }

    public void setScenarioDescription(String scenarioDescription) {
        this.scenarioDescription = scenarioDescription;
    }

    public String getDeliberateContradictionDescription() {
        return deliberateContradictionDescription;
    }

    public void setDeliberateContradictionDescription(String deliberateContradictionDescription) {
        this.deliberateContradictionDescription = deliberateContradictionDescription;
    }

    public String getSyntheticNotice() {
        return syntheticNotice;
    }

    public void setSyntheticNotice(String syntheticNotice) {
        this.syntheticNotice = syntheticNotice;
    }
}
