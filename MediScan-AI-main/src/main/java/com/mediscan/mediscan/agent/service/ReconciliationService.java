package com.mediscan.mediscan.agent.service;

import com.mediscan.mediscan.agent.model.Conflict;
import com.mediscan.mediscan.agent.model.PatientRecord;

import java.util.List;

/**
 * Service responsible for multi-source clinical data reconciliation and contradiction detection.
 */
public interface ReconciliationService {

    /**
     * Reconciles consultation transcript, EHR medications, allergies, notes, and lab results.
     * Computes genuine mismatches dynamically based on patient data.
     *
     * @param patient the patient record to reconcile
     * @return list of dynamically detected conflicts
     */
    List<Conflict> reconcile(PatientRecord patient);
}
