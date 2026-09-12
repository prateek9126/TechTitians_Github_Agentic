package com.mediscan.mediscan.agent.model;

/**
 * Medication history entry in the patient EHR.
 * SYNTHETIC / DEIDENTIFIED DATA MODEL
 */
public class MedicationRecord {

    private String id;
    private String medicationName;
    private String dosage;
    private String frequency;
    private String route;
    private String status; // ACTIVE, DISCONTINUED, ON_HOLD
    private String prescribedDate;
    private String lastRefillDate;
    private String prescriber;
    private String indication;

    public MedicationRecord() {
    }

    public MedicationRecord(String id, String medicationName, String dosage,
                            String frequency, String route, String status,
                            String prescribedDate, String lastRefillDate,
                            String prescriber, String indication) {
        this.id = id;
        this.medicationName = medicationName;
        this.dosage = dosage;
        this.frequency = frequency;
        this.route = route;
        this.status = status;
        this.prescribedDate = prescribedDate;
        this.lastRefillDate = lastRefillDate;
        this.prescriber = prescriber;
        this.indication = indication;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMedicationName() {
        return medicationName;
    }

    public void setMedicationName(String medicationName) {
        this.medicationName = medicationName;
    }

    public String getDosage() {
        return dosage;
    }

    public void setDosage(String dosage) {
        this.dosage = dosage;
    }

    public String getFrequency() {
        return frequency;
    }

    public void setFrequency(String frequency) {
        this.frequency = frequency;
    }

    public String getRoute() {
        return route;
    }

    public void setRoute(String route) {
        this.route = route;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPrescribedDate() {
        return prescribedDate;
    }

    public void setPrescribedDate(String prescribedDate) {
        this.prescribedDate = prescribedDate;
    }

    public String getLastRefillDate() {
        return lastRefillDate;
    }

    public void setLastRefillDate(String lastRefillDate) {
        this.lastRefillDate = lastRefillDate;
    }

    public String getPrescriber() {
        return prescriber;
    }

    public void setPrescriber(String prescriber) {
        this.prescriber = prescriber;
    }

    public String getIndication() {
        return indication;
    }

    public void setIndication(String indication) {
        this.indication = indication;
    }
}
