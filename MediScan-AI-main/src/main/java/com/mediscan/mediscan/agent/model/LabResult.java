package com.mediscan.mediscan.agent.model;

/**
 * Structured Lab Result record in patient EHR.
 */
public class LabResult {

    private String testName;
    private String value;
    private String unit;
    private String date;
    private String referenceRange;
    private String flag; // NORMAL, HIGH, LOW, CRITICAL

    public LabResult() {
    }

    public LabResult(String testName, String value, String unit, String date) {
        this.testName = testName;
        this.value = value;
        this.unit = unit;
        this.date = date;
    }

    public LabResult(String testName, String value, String unit, String date, String referenceRange, String flag) {
        this.testName = testName;
        this.value = value;
        this.unit = unit;
        this.date = date;
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

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
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

    @Override
    public String toString() {
        return testName + ": " + value + " " + (unit != null ? unit : "") + " (" + (date != null ? date : "") + ")" + (flag != null ? " [" + flag + "]" : "");
    }
}
