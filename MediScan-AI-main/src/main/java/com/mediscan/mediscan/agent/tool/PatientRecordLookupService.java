package com.mediscan.mediscan.agent.tool;

import com.mediscan.mediscan.agent.model.PatientSourceBundle;
import com.mediscan.mediscan.agent.model.ToolCallLog;

import java.util.Optional;

/**
 * Tool interface for ingesting and fetching full synthetic patient source bundles.
 */
public interface PatientRecordLookupService {

    PatientSourceBundle getPatientRecord(String patientId, boolean simulateFailure, ToolCallLog log);

    Optional<PatientSourceBundle> findPatientById(String patientId);

    void updatePatientRecord(PatientSourceBundle bundle);
}
