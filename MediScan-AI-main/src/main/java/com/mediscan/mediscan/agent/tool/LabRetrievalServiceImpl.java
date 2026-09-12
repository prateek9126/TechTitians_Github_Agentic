package com.mediscan.mediscan.agent.tool;

import com.mediscan.mediscan.agent.data.SyntheticPatientRepository;
import com.mediscan.mediscan.agent.model.LabReportRecord;
import com.mediscan.mediscan.agent.model.LabResult;
import com.mediscan.mediscan.agent.model.LabResultItem;
import com.mediscan.mediscan.agent.model.PatientSourceBundle;
import com.mediscan.mediscan.agent.model.ToolCallLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

/**
 * Implementation of LabRetrievalService.
 * Fetches latest and date-specific laboratory findings directly from PatientRecord.
 */
@Service
public class LabRetrievalServiceImpl implements LabRetrievalService {

    private final SyntheticPatientRepository repository;

    @Autowired
    public LabRetrievalServiceImpl(SyntheticPatientRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<LabResult> getLatest(String patientId) {
        return getLatest(patientId, false, null);
    }

    @Override
    public Optional<LabResult> getLatest(String patientId, boolean simulateFailure, ToolCallLog log) {
        long startTime = System.currentTimeMillis();
        String timestamp = Instant.now().toString();

        if (log != null) {
            log.setId("TOOL-" + UUID.randomUUID().toString().substring(0, 8));
            log.setToolName("LabRetrievalService");
            log.setCallReason("Query latest laboratory result on patient record");
            log.setInputSummary("patientId: " + patientId + ", simulateFailure: " + simulateFailure);
            log.setTimestamp(timestamp);
        }

        if (simulateFailure) {
            long duration = System.currentTimeMillis() - startTime;
            if (log != null) {
                log.setStatus("FAILED");
                log.setDurationMs(duration);
                log.setOutputSummary("Simulated Failure: LIS Gateway Connection Timeout (HTTP 504)");
                log.setErrorDetails("LIS server gateway timeout during lab query for patient " + patientId);
            }
            throw new RuntimeException("Simulated Failure: LIS Gateway Connection Timeout (HTTP 504)");
        }

        Optional<PatientSourceBundle> opt = repository.getPatientById(patientId);
        if (opt.isEmpty() || opt.get().getLabResults().isEmpty()) {
            if (log != null) {
                log.setStatus("SUCCESS");
                log.setDurationMs(System.currentTimeMillis() - startTime);
                log.setOutputSummary("No lab results recorded for patient " + patientId);
            }
            return Optional.empty();
        }

        List<LabResult> results = opt.get().getLabResults();
        LabResult latest = results.get(results.size() - 1);

        long duration = System.currentTimeMillis() - startTime;
        if (log != null) {
            log.setStatus("SUCCESS");
            log.setDurationMs(duration);
            log.setOutputSummary("Retrieved latest lab result: " + latest.getTestName() + " = " + latest.getValue() + " " + latest.getUnit() + " (" + latest.getDate() + ")");
        }

        return Optional.of(latest);
    }

    @Override
    public Optional<LabResult> getByDate(String patientId, String date) {
        Optional<PatientSourceBundle> opt = repository.getPatientById(patientId);
        if (opt.isEmpty()) return Optional.empty();

        return opt.get().getLabResults().stream()
                .filter(r -> r.getDate() != null && r.getDate().equals(date))
                .findFirst();
    }

    @Override
    public void addLabResult(String patientId, LabResult result) {
        repository.getPatientById(patientId).ifPresent(bundle -> {
            bundle.getLabResults().add(result);
            // Also synchronize with legacy labReports panel
            List<LabReportRecord> reports = bundle.getLabReports();
            if (reports.isEmpty()) {
                List<LabResultItem> items = new ArrayList<>();
                items.add(new LabResultItem(result.getTestName(), result.getValue(), result.getUnit(), result.getReferenceRange(), result.getFlag()));
                reports.add(new LabReportRecord("LAB-REPORT-NEW", result.getDate(), "STAT Diagnostic Panel", items, "STAT Lab update"));
            } else {
                reports.get(reports.size() - 1).getResults().add(
                        new LabResultItem(result.getTestName(), result.getValue(), result.getUnit(), result.getReferenceRange(), result.getFlag())
                );
            }
        });
    }

    @Override
    public List<LabReportRecord> getPatientLabReports(String patientId, boolean simulateFailure, ToolCallLog log) {
        long startTime = System.currentTimeMillis();
        String timestamp = Instant.now().toString();

        if (log != null) {
            log.setId("TOOL-" + UUID.randomUUID().toString().substring(0, 8));
            log.setToolName("LabRetrievalService");
            log.setCallReason("Retrieve complete diagnostic laboratory history");
            log.setInputSummary("patientId: " + patientId);
            log.setTimestamp(timestamp);
        }

        if (simulateFailure) {
            long duration = System.currentTimeMillis() - startTime;
            if (log != null) {
                log.setStatus("FAILED");
                log.setDurationMs(duration);
                log.setOutputSummary("Simulated Failure: Lab Information System (LIS) Connection Timeout (504)");
                log.setErrorDetails("LIS server did not respond within timeout window");
            }
            throw new RuntimeException("Simulated Failure: LIS Gateway Connection Timeout (HTTP 504)");
        }

        Optional<PatientSourceBundle> opt = repository.getPatientById(patientId);
        List<LabReportRecord> reports = opt.map(PatientSourceBundle::getLabReports).orElse(Collections.emptyList());
        long duration = System.currentTimeMillis() - startTime;

        if (log != null) {
            log.setStatus("SUCCESS");
            log.setDurationMs(duration);
            log.setOutputSummary(String.format("Retrieved %d lab panels for patient %s.", reports.size(), patientId));
        }

        return reports;
    }

    @Override
    public Optional<LabReportRecord> getLatestLabReport(String patientId, String panelType, boolean simulateFailure, ToolCallLog log) {
        long startTime = System.currentTimeMillis();
        String timestamp = Instant.now().toString();

        if (log != null) {
            log.setId("TOOL-" + UUID.randomUUID().toString().substring(0, 8));
            log.setToolName("LabRetrievalService");
            log.setCallReason("Retrieve most recent diagnostic lab panel by filter: " + (panelType == null ? "LATEST" : panelType));
            log.setInputSummary("patientId: " + patientId + ", panelType: " + panelType);
            log.setTimestamp(timestamp);
        }

        if (simulateFailure) {
            long duration = System.currentTimeMillis() - startTime;
            if (log != null) {
                log.setStatus("FAILED");
                log.setDurationMs(duration);
                log.setOutputSummary("Simulated Failure: LIS Connection Timeout (504)");
                log.setErrorDetails("LIS timeout retrieving latest panel");
            }
            throw new RuntimeException("Simulated Failure: LIS Gateway Connection Timeout (HTTP 504)");
        }

        Optional<PatientSourceBundle> opt = repository.getPatientById(patientId);
        if (opt.isEmpty() || opt.get().getLabReports().isEmpty()) {
            if (log != null) {
                log.setStatus("SUCCESS");
                log.setDurationMs(System.currentTimeMillis() - startTime);
                log.setOutputSummary("No lab reports found for patient " + patientId);
            }
            return Optional.empty();
        }

        List<LabReportRecord> reports = opt.get().getLabReports();
        LabReportRecord latest = null;

        for (int i = reports.size() - 1; i >= 0; i--) {
            LabReportRecord r = reports.get(i);
            if (panelType == null || panelType.isBlank() || r.getPanelName().toLowerCase().contains(panelType.toLowerCase())) {
                latest = r;
                break;
            }
        }

        long duration = System.currentTimeMillis() - startTime;
        if (log != null) {
            log.setStatus("SUCCESS");
            log.setDurationMs(duration);
            if (latest != null) {
                StringBuilder sb = new StringBuilder();
                sb.append(latest.getPanelName()).append(" (").append(latest.getTestDate()).append("): ");
                for (LabResultItem it : latest.getResults()) {
                    sb.append(it.getTestName()).append("=").append(it.getValue()).append(" ").append(it.getUnit());
                    if (!"NORMAL".equalsIgnoreCase(it.getFlag())) {
                        sb.append(" [").append(it.getFlag()).append("] ");
                    }
                    sb.append("; ");
                }
                log.setOutputSummary(sb.toString());
            } else {
                log.setOutputSummary("No matching lab panel found for criteria: " + panelType);
            }
        }

        return Optional.ofNullable(latest);
    }

    @Override
    public void addLabReport(String patientId, LabReportRecord report) {
        repository.getPatientById(patientId).ifPresent(bundle -> {
            bundle.getLabReports().add(report);
            for (LabResultItem it : report.getResults()) {
                bundle.getLabResults().add(new LabResult(it.getTestName(), it.getValue(), it.getUnit(), report.getTestDate(), it.getReferenceRange(), it.getFlag()));
            }
        });
    }
}
