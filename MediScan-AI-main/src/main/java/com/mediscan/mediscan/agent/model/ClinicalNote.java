package com.mediscan.mediscan.agent.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Historical or recent clinical note in the patient EHR.
 * SYNTHETIC / DEIDENTIFIED DATA MODEL
 */
public class ClinicalNote {

    private String noteId;
    private String date;
    private String author;
    private String department;
    private String noteType; // OUTPATIENT_CONSULT, INPATIENT_DISCHARGE, EMERGENCY_ENCOUNTER
    private String content;
    private String recordedVitals;
    private List<String> recordedAllergies = new ArrayList<>();
    private List<String> recordedDiagnoses = new ArrayList<>();

    public ClinicalNote() {
    }

    public ClinicalNote(String date, String text) {
        this.date = date;
        this.content = text;
        this.noteId = "NOTE-" + System.currentTimeMillis();
    }

    public ClinicalNote(String noteId, String date, String author, String department,
                        String noteType, String content, String recordedVitals,
                        List<String> recordedAllergies, List<String> recordedDiagnoses) {
        this.noteId = noteId;
        this.date = date;
        this.author = author;
        this.department = department;
        this.noteType = noteType;
        this.content = content;
        this.recordedVitals = recordedVitals;
        this.recordedAllergies = recordedAllergies;
        this.recordedDiagnoses = recordedDiagnoses;
    }

    public String getText() {
        return content;
    }

    public void setText(String text) {
        this.content = text;
    }

    public String getNoteId() {
        return noteId;
    }

    public void setNoteId(String noteId) {
        this.noteId = noteId;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getNoteType() {
        return noteType;
    }

    public void setNoteType(String noteType) {
        this.noteType = noteType;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getRecordedVitals() {
        return recordedVitals;
    }

    public void setRecordedVitals(String recordedVitals) {
        this.recordedVitals = recordedVitals;
    }

    public List<String> getRecordedAllergies() {
        return recordedAllergies;
    }

    public void setRecordedAllergies(List<String> recordedAllergies) {
        this.recordedAllergies = recordedAllergies;
    }

    public List<String> getRecordedDiagnoses() {
        return recordedDiagnoses;
    }

    public void setRecordedDiagnoses(List<String> recordedDiagnoses) {
        this.recordedDiagnoses = recordedDiagnoses;
    }
}
