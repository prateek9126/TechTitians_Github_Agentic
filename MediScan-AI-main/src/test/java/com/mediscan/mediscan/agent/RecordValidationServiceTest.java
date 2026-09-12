package com.mediscan.mediscan.agent;

import com.mediscan.mediscan.agent.model.ActionItem;
import com.mediscan.mediscan.agent.model.ConflictItem;
import com.mediscan.mediscan.agent.model.FollowUpRecord;
import com.mediscan.mediscan.agent.model.ValidationResult;
import com.mediscan.mediscan.agent.model.ValidationResult.RuleCheck;
import com.mediscan.mediscan.agent.tool.RecordValidationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for automated rule-based RecordValidationService.
 * Proves that all 5 rules can genuinely pass and genuinely fail.
 */
class RecordValidationServiceTest {

    private RecordValidationServiceImpl validationService;

    @BeforeEach
    void setUp() {
        validationService = new RecordValidationServiceImpl();
    }

    private FollowUpRecord createStandardValidRecord() {
        FollowUpRecord record = new FollowUpRecord();
        record.setPatientId("PAT-101");
        record.setPatientName("Robert Shayne");
        record.setEncounterDate("2024-03-15");
        record.setChiefComplaint("Cough with mild fever for 5 days.");
        record.setHistoryOfPresentIllness("58-year-old male with productive cough and mild fever. Rhonchi in lower lung fields.");
        record.setObjectiveFindingsSummary("Vitals stable: BP 128/82, HR 74. WBC 11.4 K/uL.");
        record.setReconciledMedications(List.of("Lisinopril 10mg PO Daily", "Atorvastatin 20mg PO QHS"));
        record.setAllergyAlerts(List.of("Severe Penicillin Allergy Alert"));
        record.setAssessmentSummary("Findings suggestive of acute bacterial tracheobronchitis; review antibiotic selection.");
        record.setProposedFollowUpPlan("Recommend clinician review chest X-ray and evaluate non-beta-lactam option.");
        record.setDisclaimer("Draft for clinician review — not a diagnosis. Synthetic/deidentified data only.");
        return record;
    }

    private List<ActionItem> createStandardActions() {
        return List.of(new ActionItem("ACT-1", "Clinician review of allergy chart.", "CLINICIAN", "HIGH", "ALLERGY_VERIFICATION", "PENDING_CLINICIAN_REVIEW"));
    }

    @Test
    void testValidRecordPassesAllRules() {
        FollowUpRecord record = createStandardValidRecord();
        List<ActionItem> actions = createStandardActions();
        List<ConflictItem> conflicts = new ArrayList<>();

        ValidationResult result = validationService.validateRecord(record, conflicts, actions);

        assertTrue(result.isValid(), "Standard complete record should pass validation");
        assertFalse(result.isEscalationRequired(), "No escalations present");
        assertTrue(result.getErrorMessages().isEmpty(), "No error messages expected");
        assertEquals(5, result.getRules().size(), "Must execute exactly 5 distinct rules");
    }

    @Test
    void testRuleRequiredFieldsFailsWhenFieldsMissing() {
        FollowUpRecord record = createStandardValidRecord();
        record.setChiefComplaint(""); // Missing field
        record.setAssessmentSummary(null); // Missing field

        RuleCheck check = validationService.checkRequiredFields(record, createStandardActions());
        assertFalse(check.isPassed(), "RULE_REQUIRED_FIELDS must genuinely fail when required fields are missing");
        assertEquals("RULE_REQUIRED_FIELDS", check.getRuleName());
        assertTrue(check.getDetails().contains("Missing required fields"));
    }

    @Test
    void testRuleHighSeverityConflictsFailsWhenUnresolved() {
        List<ConflictItem> conflicts = List.of(
                new ConflictItem("CONF-1", "Penicillin Allergy", "EHR", "NKDA", "Note", "Anaphylaxis", "HIGH", "DETECTED", "Unresolved contradiction", null, "High risk")
        );

        RuleCheck check = validationService.checkHighSeverityConflicts(conflicts);
        assertFalse(check.isPassed(), "RULE_HIGH_SEVERITY_CONFLICTS must genuinely fail on un-escalated HIGH severity conflict");
        assertEquals("RULE_HIGH_SEVERITY_CONFLICTS", check.getRuleName());
        assertTrue(check.getDetails().contains("unresolved HIGH severity"));
    }

    @Test
    void testRuleHighSeverityConflictsPassesWithWarningWhenEscalated() {
        List<ConflictItem> conflicts = List.of(
                new ConflictItem("CONF-1", "Penicillin Allergy", "EHR", "NKDA", "Note", "Anaphylaxis", "HIGH", "ESCALATED", "Safely escalated to clinician", "MedicationAllergyLookupService", "Safety rule applied")
        );

        RuleCheck check = validationService.checkHighSeverityConflicts(conflicts);
        assertTrue(check.isPassed(), "Escalated high-severity conflict must pass with safety warning");
        assertEquals("WARNING", check.getSeverity());
    }

    @Test
    void testRuleTemporalConsistencyFailsOnInvalidDate() {
        FollowUpRecord record = createStandardValidRecord();
        record.setEncounterDate("1850-01-01"); // Impossible historical date

        RuleCheck check = validationService.checkTemporalValueConsistency(record);
        assertFalse(check.isPassed(), "RULE_TEMPORAL_VALUE_CONSISTENCY must genuinely fail on out-of-bounds dates");
        assertEquals("RULE_TEMPORAL_VALUE_CONSISTENCY", check.getRuleName());
    }

    @Test
    void testRuleGuardrailSafetyFailsWhenDraftContainsYouHaveHypertension() {
        FollowUpRecord record = createStandardValidRecord();
        // MANDATORY REQUIREMENT TEST: "you have hypertension"
        record.setAssessmentSummary("Based on today's triage blood pressure of 158/96, you have hypertension.");

        RuleCheck check = validationService.checkGuardrailSafety(record);
        assertFalse(check.isPassed(), "RULE_GUARDRAIL_SAFETY MUST genuinely fail when draft contains 'you have hypertension'");
        assertEquals("RULE_GUARDRAIL_SAFETY", check.getRuleName());
        assertTrue(check.getDetails().contains("'you have'"), "Details must explicitly flag the forbidden 'you have' phrase");

        // Confirm full validation result also fails
        ValidationResult valResult = validationService.validateRecord(record, Collections.emptyList(), createStandardActions());
        assertFalse(valResult.isValid(), "Overall validation must fail when guardrail check fails");
    }

    @Test
    void testRuleGuardrailSafetyFailsOnPrescribeAndDiagnosedWith() {
        FollowUpRecord record1 = createStandardValidRecord();
        record1.setProposedFollowUpPlan("I will prescribe 875 mg Augmentin today.");
        RuleCheck check1 = validationService.checkGuardrailSafety(record1);
        assertFalse(check1.isPassed(), "Must fail on 'prescribe'");

        FollowUpRecord record2 = createStandardValidRecord();
        record2.setAssessmentSummary("Patient is definitively diagnosed with bacterial bronchitis.");
        RuleCheck check2 = validationService.checkGuardrailSafety(record2);
        assertFalse(check2.isPassed(), "Must fail on 'diagnosed with'");
    }

    @Test
    void testRulePhysiologicalBoundsFailsOnImpossibleValues() {
        FollowUpRecord record = createStandardValidRecord();
        // Inject impossible potassium and eGFR values
        record.setObjectiveFindingsSummary("Labs show Potassium 14.5 mEq/L and eGFR -5 mL/min.");

        RuleCheck check = validationService.checkPhysiologicalBounds(record);
        assertFalse(check.isPassed(), "RULE_PHYSIOLOGICAL_BOUNDS must genuinely fail on impossible vitals/lab values");
        assertEquals("RULE_PHYSIOLOGICAL_BOUNDS", check.getRuleName());
        assertTrue(check.getDetails().contains("Potassium value 14.5") || check.getDetails().contains("eGFR"));
    }
}
