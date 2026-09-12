package com.mediscan.mediscan.agent.tool;

import com.mediscan.mediscan.agent.data.SyntheticPatientRepository;
import com.mediscan.mediscan.agent.model.PatientSourceBundle;
import com.mediscan.mediscan.agent.model.ToolCallLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementation of PatientRecordLookupService.
 * Retrieves multi-source bundles from the synthetic repository with transparent execution logging.
 */
@Service
public class PatientRecordLookupServiceImpl implements PatientRecordLookupService {

    private final SyntheticPatientRepository repository;

    @Autowired
    public PatientRecordLookupServiceImpl(SyntheticPatientRepository repository) {
        this.repository = repository;
    }

    @Override
    public PatientSourceBundle getPatientRecord(String patientId, boolean simulateFailure, ToolCallLog log) {
        long startTime = System.currentTimeMillis();
        String timestamp = Instant.now().toString();

        if (log != null) {
            log.setId("TOOL-" + UUID.randomUUID().toString().substring(0, 8));
            log.setToolName("PatientRecordLookupService");
            log.setCallReason("Ingest complete synthetic patient data bundle (transcripts, notes, meds, allergies, labs)");
            log.setInputSummary("patientId: " + patientId + ", simulateFailure: " + simulateFailure);
            log.setTimestamp(timestamp);
        }

        if (simulateFailure) {
            long duration = System.currentTimeMillis() - startTime;
            if (log != null) {
                log.setStatus("FAILED");
                log.setDurationMs(duration);
                log.setOutputSummary("Simulated Connection Timeout: EHR Synthetic Gateway Unreachable (HTTP 504)");
                log.setErrorDetails("Simulated network timeout during patient record retrieval");
            }
            throw new RuntimeException("Simulated Connection Timeout: EHR Gateway Unreachable");
        }

        Optional<PatientSourceBundle> opt = repository.getPatientById(patientId);
        long duration = System.currentTimeMillis() - startTime;

        if (opt.isEmpty()) {
            if (log != null) {
                log.setStatus("FAILED");
                log.setDurationMs(duration);
                log.setOutputSummary("Patient not found: " + patientId);
            }
            throw new IllegalArgumentException("Patient not found with ID: " + patientId);
        }

        PatientSourceBundle bundle = opt.get();
        if (log != null) {
            log.setStatus("SUCCESS");
            log.setDurationMs(duration);
            log.setOutputSummary(String.format("Fetched %s (%s, %dyo). Found: %d notes, %d meds, %d allergies, %d lab panels.",
                    bundle.getName(), bundle.getGender(), bundle.getAge(),
                    bundle.getPreviousNotes().size(), bundle.getMedicationHistory().size(),
                    bundle.getAllergyList().size(), bundle.getLabReports().size()));
        }

        return bundle;
    }

    @Override
    public Optional<PatientSourceBundle> findPatientById(String patientId) {
        return repository.getPatientById(patientId);
    }

    @Override
    public void updatePatientRecord(PatientSourceBundle bundle) {
        repository.updatePatient(bundle);
    }
}
