package com.mediscan.mediscan.agent.model;

/**
 * Structured Medication record in patient EHR.
 */
public class Medication {

    private String name;
    private String dosage;
    private String frequency;
    private boolean active;
    private String startDate;

    public Medication() {
    }

    public Medication(String name, String dosage, String frequency, boolean active, String startDate) {
        this.name = name;
        this.dosage = dosage;
        this.frequency = frequency;
        this.active = active;
        this.startDate = startDate;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    @Override
    public String toString() {
        return name + " " + (dosage != null ? dosage : "") + " (" + (frequency != null ? frequency : "") + ") [active=" + active + "]";
    }
}
