package com.mediscan.mediscan.agent.tool;

import com.mediscan.mediscan.agent.model.LabReportRecord;
import com.mediscan.mediscan.agent.model.LabResult;
import com.mediscan.mediscan.agent.model.ToolCallLog;

import java.util.List;
import java.util.Optional;

/**
 * Tool interface for retrieving patient lab reports and structured laboratory results.
 */
public interface LabRetrievalService {

    List<LabReportRecord> getPatientLabReports(String patientId, boolean simulateFailure, ToolCallLog log);

    Optional<LabReportRecord> getLatestLabReport(String patientId, String panelType, boolean simulateFailure, ToolCallLog log);

    void addLabReport(String patientId, LabReportRecord report);

    // Structured LabResult queries directly against PatientRecord.labResults
    Optional<LabResult> getLatest(String patientId);

    Optional<LabResult> getLatest(String patientId, boolean simulateFailure, ToolCallLog log);

    Optional<LabResult> getByDate(String patientId, String date);

    void addLabResult(String patientId, LabResult result);
}
