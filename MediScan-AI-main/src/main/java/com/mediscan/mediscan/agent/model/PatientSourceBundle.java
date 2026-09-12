package com.mediscan.mediscan.agent.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Multi-source synthetic patient bundle representing all ingested inputs.
 * Extends PatientRecord to bridge structured data model with legacy and UI views.
 * SYNTHETIC / DEIDENTIFIED DATA MODEL
 */
public class PatientSourceBundle extends PatientRecord {

    private List<MedicationRecord> medicationHistory = new ArrayList<>();
    private List<AllergyRecord> allergyList = new ArrayList<>();
    private List<LabReportRecord> labReports = new ArrayList<>();

    public PatientSourceBundle() {
        super();
    }

    public PatientSourceBundle(String patientId, String name, int age, String gender, String transcriptText) {
        super(patientId, name, age, gender, transcriptText);
    }

    @Override
    public String getConsultationTranscript() {
        return getTranscriptText();
    }

    @Override
    public void setConsultationTranscript(String consultationTranscript) {
        setTranscriptText(consultationTranscript);
    }

    public List<MedicationRecord> getMedicationHistory() {
        return medicationHistory;
    }

    public void setMedicationHistory(List<MedicationRecord> medicationHistory) {
        this.medicationHistory = medicationHistory != null ? medicationHistory : new ArrayList<>();
        // Synchronize with structured ehrMedications
        List<Medication> structured = new ArrayList<>();
        for (MedicationRecord mr : this.medicationHistory) {
            boolean active = "ACTIVE".equalsIgnoreCase(mr.getStatus());
            structured.add(new Medication(mr.getMedicationName(), mr.getDosage(), mr.getFrequency(), active, mr.getPrescribedDate()));
        }
        super.setEhrMedications(structured);
    }

    @Override
    public void setEhrMedications(List<Medication> ehrMedications) {
        super.setEhrMedications(ehrMedications);
        // Synchronize with medicationHistory
        List<MedicationRecord> records = new ArrayList<>();
        int idx = 1;
        for (Medication m : super.getEhrMedications()) {
            records.add(new MedicationRecord(
                    "MED-" + idx++,
                    m.getName(),
                    m.getDosage(),
                    m.getFrequency(),
                    "Oral",
                    m.isActive() ? "ACTIVE" : "DISCONTINUED",
                    m.getStartDate(),
                    m.getStartDate(),
                    "Attending Physician",
                    "Indication"
            ));
        }
        this.medicationHistory = records;
    }

    public List<AllergyRecord> getAllergyList() {
        return allergyList;
    }

    public void setAllergyList(List<AllergyRecord> allergyList) {
        this.allergyList = allergyList != null ? allergyList : new ArrayList<>();
        // Synchronize with structured ehrAllergies
        List<String> structured = new ArrayList<>();
        for (AllergyRecord ar : this.allergyList) {
            structured.add(ar.getAllergen());
        }
        super.setEhrAllergies(structured);
    }

    @Override
    public void setEhrAllergies(List<String> ehrAllergies) {
        super.setEhrAllergies(ehrAllergies);
        // Synchronize with allergyList
        List<AllergyRecord> records = new ArrayList<>();
        int idx = 1;
        for (String a : super.getEhrAllergies()) {
            String status = a.toUpperCase().contains("NKDA") || a.toUpperCase().contains("NO KNOWN") ? "REPORTED_NKDA" : "ACTIVE";
            records.add(new AllergyRecord(
                    "ALG-" + idx++,
                    a,
                    "DRUG",
                    "Documented reaction",
                    "HIGH",
                    status,
                    "2024-01-01"
            ));
        }
        this.allergyList = records;
    }

    public List<LabReportRecord> getLabReports() {
        return labReports;
    }

    public void setLabReports(List<LabReportRecord> labReports) {
        this.labReports = labReports != null ? labReports : new ArrayList<>();
        // Synchronize with structured labResults
        List<LabResult> structured = new ArrayList<>();
        for (LabReportRecord lrr : this.labReports) {
            for (LabResultItem it : lrr.getResults()) {
                structured.add(new LabResult(
                        it.getTestName(),
                        it.getValue(),
                        it.getUnit(),
                        lrr.getTestDate(),
                        it.getReferenceRange(),
                        it.getFlag()
                ));
            }
        }
        super.setLabResults(structured);
    }

    @Override
    public void setLabResults(List<LabResult> labResults) {
        super.setLabResults(labResults);
        // Synchronize with labReports
        List<LabReportRecord> reports = new ArrayList<>();
        List<LabResultItem> items = new ArrayList<>();
        String date = "2024-03-01";
        for (LabResult lr : super.getLabResults()) {
            if (lr.getDate() != null && !lr.getDate().isBlank()) {
                date = lr.getDate();
            }
            items.add(new LabResultItem(lr.getTestName(), lr.getValue(), lr.getUnit(), lr.getReferenceRange(), lr.getFlag()));
        }
        reports.add(new LabReportRecord("LAB-REPORT-1", date, "Diagnostic Laboratory Panel", items, "Routine diagnostic panel"));
        this.labReports = reports;
    }
}
