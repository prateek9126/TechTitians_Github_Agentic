package com.mediscan.mediscan.agent.model;

/**
 * Individual test result within a laboratory panel.
 * SYNTHETIC / DEIDENTIFIED DATA MODEL
 */
public class LabResultItem {

    private String testName;
    private String value;
    private String unit;
    private String referenceRange;
    private String flag; // NORMAL, HIGH, LOW, CRITICAL

    public LabResultItem() {
    }

    public LabResultItem(String testName, String value, String unit, String referenceRange, String flag) {
        this.testName = testName;
        this.value = value;
        this.unit = unit;
        this.referenceRange = referenceRange;
        this.flag = flag;
    }

    public String getTestName() {
        return testName;
    }

    public void setTestName(String testName) {
        this.testName = testName;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getReferenceRange() {
        return referenceRange;
    }

    public void setReferenceRange(String referenceRange) {
        this.referenceRange = referenceRange;
    }

    public String getFlag() {
        return flag;
    }

    public void setFlag(String flag) {
        this.flag = flag;
    }
}
