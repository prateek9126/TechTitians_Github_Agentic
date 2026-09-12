package com.mediscan.mediscan.agent;

import com.mediscan.mediscan.agent.data.SyntheticPatientRepository;
import com.mediscan.mediscan.agent.model.*;
import com.mediscan.mediscan.agent.service.ClinicalAgentService;
import com.mediscan.mediscan.agent.service.ReconciliationServiceImpl;
import com.mediscan.mediscan.agent.tool.MedicationAllergyLookupServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for multi-source reconciliation diff engine.
 * Proves that editing ANY field changes pipeline behavior with ZERO code changes.
 */
class ReconciliationDiffTest {

    private ClinicalAgentService agentService;
    private ReconciliationServiceImpl reconciliationService;
    private MedicationAllergyLookupServiceImpl lookupService;
    private SyntheticPatientRepository repository;

    @BeforeEach
    void setUp() {
        repository = new SyntheticPatientRepository();
        lookupService = new MedicationAllergyLookupServiceImpl();
        reconciliationService = new ReconciliationServiceImpl(lookupService);
        agentService = new ClinicalAgentService(null, reconciliationService, lookupService, null, null, null, null);
    }

    @Test
    void testDetectsAllergyContradictionForPatient101() {
        PatientSourceBundle p1 = repository.getPatientById("PAT-101").orElseThrow();

        List<ConflictItem> conflicts = agentService.reconcileSources(p1);

        assertNotNull(conflicts);
        assertFalse(conflicts.isEmpty(), "Should detect at least one conflict");

        // Verify allergy contradiction detected
        ConflictItem allergyConflict = conflicts.stream()
                .filter(c -> c.getField().toLowerCase().contains("allergy"))
                .findFirst()
                .orElse(null);

        assertNotNull(allergyConflict, "Must detect allergy documentation conflict");
        assertEquals("HIGH", allergyConflict.getSeverity(), "Allergy contradiction must be HIGH severity");
        assertTrue(allergyConflict.getValueA().contains("NKDA"), "Source A should be NKDA");
        assertTrue(allergyConflict.getValueB().toLowerCase().contains("anaphylactoid") ||
                allergyConflict.getValueB().toLowerCase().contains("urticaria"), "Source B should mention severe reaction");
    }

    @Test
    void testEditingAllergyResolvesConflictWithZeroCodeChanges() {
        PatientSourceBundle p1 = repository.getPatientById("PAT-101").orElseThrow();

        // Initially detects allergy conflict
        List<ConflictItem> initialConflicts = agentService.reconcileSources(p1);
        boolean initialHasAllergyConflict = initialConflicts.stream().anyMatch(c -> c.getField().toLowerCase().contains("allergy"));
        assertTrue(initialHasAllergyConflict, "Initially should have allergy conflict");

        // CORE ACCEPTANCE TEST: Edit allergy field by updating EHR to document Penicillin allergy
        p1.setEhrAllergies(List.of("Severe Penicillin / Augmentin allergy (Anaphylactoid)"));

        // Re-reconcile without ANY code changes
        List<ConflictItem> updatedConflicts = agentService.reconcileSources(p1);
        boolean updatedHasAllergyConflict = updatedConflicts.stream().anyMatch(c -> c.getField().toLowerCase().contains("allergy"));
        assertFalse(updatedHasAllergyConflict, "Updating EHR allergy field must eliminate conflict dynamically");
    }

    @Test
    void testDetectsMedicationCessationDiscrepancyForPatient102() {
        PatientSourceBundle p2 = repository.getPatientById("PAT-102").orElseThrow();

        List<ConflictItem> conflicts = agentService.reconcileSources(p2);

        assertNotNull(conflicts);
        assertFalse(conflicts.isEmpty(), "Should detect medication adherence conflict");

        ConflictItem medConflict = conflicts.stream()
                .filter(c -> c.getField().toLowerCase().contains("metformin"))
                .findFirst()
                .orElse(null);

        assertNotNull(medConflict, "Must detect Metformin adherence discrepancy");
        assertTrue(medConflict.getValueA().toLowerCase().contains("discontinuing") || medConflict.getValueA().toLowerCase().contains("stopped"), "Transcript reports stopping Metformin");
        assertTrue(medConflict.getValueB().toLowerCase().contains("active"), "EHR lists Metformin as active");
    }

    @Test
    void testEditingMedicationStatusChangesBehaviorWithZeroCodeChanges() {
        PatientSourceBundle p2 = repository.getPatientById("PAT-102").orElseThrow();

        // Initially active in EHR, causing conflict
        List<ConflictItem> before = agentService.reconcileSources(p2);
        assertTrue(before.stream().anyMatch(c -> c.getField().toLowerCase().contains("metformin")));

        // CORE ACCEPTANCE TEST: Set Metformin to inactive in structured EHR medications
        List<Medication> updatedMeds = new ArrayList<>();
        for (Medication m : p2.getEhrMedications()) {
            if (m.getName().toLowerCase().contains("metformin")) {
                updatedMeds.add(new Medication(m.getName(), m.getDosage(), m.getFrequency(), false, m.getStartDate()));
            } else {
                updatedMeds.add(m);
            }
        }
        p2.setEhrMedications(updatedMeds);

        // Re-reconcile: adherence conflict should no longer be triggered since EHR already marks it discontinued!
        List<ConflictItem> after = agentService.reconcileSources(p2);
        assertFalse(after.stream().anyMatch(c -> c.getField().toLowerCase().contains("metformin")),
                "Marking Metformin inactive in EHR must remove adherence conflict with zero code changes");
    }

    @Test
    void testDetectsTripleWhammyInteractionForPatient103() {
        PatientSourceBundle p3 = repository.getPatientById("PAT-103").orElseThrow();

        List<ConflictItem> conflicts = agentService.reconcileSources(p3);

        assertNotNull(conflicts);
        ConflictItem triadConflict = conflicts.stream()
                .filter(c -> c.getField().toLowerCase().contains("triple whammy") || c.getField().toLowerCase().contains("polypharmacy"))
                .findFirst()
                .orElse(null);

        assertNotNull(triadConflict, "Must detect triple whammy interaction");
        assertEquals("HIGH", triadConflict.getSeverity(), "Triple whammy interaction must be HIGH severity");
        assertTrue(triadConflict.getValueA().toLowerCase().contains("ibuprofen"));
        assertTrue(triadConflict.getValueB().toLowerCase().contains("lisinopril"));
    }

    @Test
    void testDetectsLabContradictionForPatient104() {
        PatientSourceBundle p4 = repository.getPatientById("PAT-104").orElseThrow();

        List<ConflictItem> conflicts = agentService.reconcileSources(p4);

        assertNotNull(conflicts);
        assertFalse(conflicts.isEmpty(), "Should detect lab contradiction conflict for PAT-104");

        ConflictItem labConflict = conflicts.stream()
                .filter(c -> c.getField().toLowerCase().contains("lab") || c.getField().toLowerCase().contains("glucose"))
                .findFirst()
                .orElse(null);

        assertNotNull(labConflict, "Must detect glycemic lab discrepancy");
        assertEquals("HIGH", labConflict.getSeverity());
        assertTrue(labConflict.getValueB().contains("245"), "Source B should reflect objective lab glucose of 245");
    }
}
