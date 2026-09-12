package com.mediscan.mediscan.agent.tool;

import com.mediscan.mediscan.agent.model.ActionItem;
import com.mediscan.mediscan.agent.model.ConflictItem;
import com.mediscan.mediscan.agent.model.FollowUpRecord;
import com.mediscan.mediscan.agent.model.ValidationResult;
import com.mediscan.mediscan.agent.model.ValidationResult.RuleCheck;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implementation of RecordValidationService.
 * Executes automated rule-based checks on clinical records across 5 distinct rules:
 * Rule 1: RULE_REQUIRED_FIELDS
 * Rule 2: RULE_HIGH_SEVERITY_CONFLICTS
 * Rule 3: RULE_TEMPORAL_VALUE_CONSISTENCY
 * Rule 4: RULE_GUARDRAIL_SAFETY
 * Rule 5: RULE_PHYSIOLOGICAL_BOUNDS
 */
@Service
public class RecordValidationServiceImpl implements RecordValidationService {

    // Regex patterns for forbidden prescriptive and definitive diagnostic phrases
    private static final Pattern[] FORBIDDEN_GUARDRAIL_PATTERNS = new Pattern[]{
            Pattern.compile("(?i)\\b(you have)\\b"),
            Pattern.compile("(?i)\\b(prescribe)\\b"),
            Pattern.compile("(?i)\\b(take \\d+ ?mg)\\b"),
            Pattern.compile("(?i)\\b(diagnosed with)\\b"),
            Pattern.compile("(?i)\\b(you must take|patient must take)\\b")
    };

    // Patterns for physiological bounds verification
    private static final Pattern POTASSIUM_PATTERN = Pattern.compile("(?i)\\b(?:potassium|k\\+)\\s*(?:is|was|=|:)?\\s*([0-9]+(?:\\.[0-9]+)?)\\b");
    private static final Pattern EGFR_PATTERN = Pattern.compile("(?i)\\begfr\\s*(?:is|was|=|:)?\\s*(-?[0-9]+(?:\\.[0-9]+)?)\\b");
    private static final Pattern HR_PATTERN = Pattern.compile("(?i)\\bhr\\s*(?:is|was|=|:)?\\s*([0-9]+)\\b");
    private static final Pattern BP_PATTERN = Pattern.compile("(?i)\\bbp\\s*(?:is|was|=|:)?\\s*([0-9]+)/([0-9]+)\\b");
    private static final Pattern TEMP_PATTERN = Pattern.compile("(?i)\\btemp(?:erature)?\\s*(?:is|was|=|:)?\\s*([0-9]+(?:\\.[0-9]+)?)\\s*f\\b");
    private static final Pattern GLUCOSE_PATTERN = Pattern.compile("(?i)\\b(?:glucose|blood sugar)\\s*(?:is|was|=|:)?\\s*([0-9]+)\\b");

    @Override
    public ValidationResult validateRecord(FollowUpRecord record,
                                           List<ConflictItem> conflicts,
                                           List<ActionItem> actions) {
        ValidationResult result = new ValidationResult();
        List<RuleCheck> rules = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        boolean escalationRequired = false;

        // Rule 1: Required Fields
        RuleCheck r1 = checkRequiredFields(record, actions);
        rules.add(r1);
        if (!r1.isPassed()) {
            errors.add(r1.getDetails());
        }

        // Rule 2: High Severity Conflicts
        RuleCheck r2 = checkHighSeverityConflicts(conflicts);
        rules.add(r2);
        if (!r2.isPassed()) {
            errors.add(r2.getDetails());
        } else if ("WARNING".equalsIgnoreCase(r2.getSeverity())) {
            warnings.add(r2.getDetails());
            escalationRequired = true;
        }

        // Rule 3: Temporal & Value Consistency
        RuleCheck r3 = checkTemporalValueConsistency(record);
        rules.add(r3);
        if (!r3.isPassed()) {
            errors.add(r3.getDetails());
        }

        // Rule 4: Guardrail Safety
        RuleCheck r4 = checkGuardrailSafety(record);
        rules.add(r4);
        if (!r4.isPassed()) {
            errors.add(r4.getDetails());
        }

        // Rule 5: Physiological Bounds
        RuleCheck r5 = checkPhysiologicalBounds(record);
        rules.add(r5);
        if (!r5.isPassed()) {
            errors.add(r5.getDetails());
        }

        boolean overallValid = r1.isPassed() && r2.isPassed() && r3.isPassed() && r4.isPassed() && r5.isPassed();
        result.setValid(overallValid);
        result.setEscalationRequired(escalationRequired);
        result.setRules(rules);
        result.setErrorMessages(errors);
        result.setWarnings(warnings);

        return result;
    }

    @Override
    public RuleCheck checkRequiredFields(FollowUpRecord record, List<ActionItem> actions) {
        List<String> missing = new ArrayList<>();
        if (record == null) {
            return new RuleCheck("RULE_REQUIRED_FIELDS", false, "CRITICAL", "FollowUpRecord object is null.");
        }
        if (record.getPatientId() == null || record.getPatientId().isBlank()) missing.add("patientId");
        if (record.getPatientName() == null || record.getPatientName().isBlank()) missing.add("patientName");
        if (record.getChiefComplaint() == null || record.getChiefComplaint().trim().isEmpty()) missing.add("chiefComplaint");
        if (record.getHistoryOfPresentIllness() == null || record.getHistoryOfPresentIllness().trim().isEmpty()) missing.add("historyOfPresentIllness");
        if (record.getReconciledMedications() == null || record.getReconciledMedications().isEmpty()) missing.add("reconciledMedications");
        if (record.getAssessmentSummary() == null || record.getAssessmentSummary().trim().isEmpty()) missing.add("assessmentSummary");
        if (record.getProposedFollowUpPlan() == null || record.getProposedFollowUpPlan().trim().isEmpty()) missing.add("proposedFollowUpPlan");
        if (actions == null || actions.isEmpty()) missing.add("actionItems (at least 1 required)");

        if (!missing.isEmpty()) {
            return new RuleCheck("RULE_REQUIRED_FIELDS", false, "CRITICAL", "Missing required fields: " + String.join(", ", missing));
        }
        return new RuleCheck("RULE_REQUIRED_FIELDS", true, "INFO", "All mandatory clinical record sections present and populated.");
    }

    @Override
    public RuleCheck checkHighSeverityConflicts(List<ConflictItem> conflicts) {
        int unresolvedCount = 0;
        int escalatedCount = 0;

        if (conflicts != null) {
            for (ConflictItem c : conflicts) {
                if ("HIGH".equalsIgnoreCase(c.getSeverity())) {
                    if ("ESCALATED".equalsIgnoreCase(c.getStatus())) {
                        escalatedCount++;
                    } else if (!"RESOLVED".equalsIgnoreCase(c.getStatus())) {
                        unresolvedCount++;
                    }
                }
            }
        }

        if (unresolvedCount > 0) {
            return new RuleCheck("RULE_HIGH_SEVERITY_CONFLICTS", false, "CRITICAL",
                    String.format("Found %d unresolved HIGH severity conflict(s). Safety policy requires resolution or clinician escalation.", unresolvedCount));
        }
        if (escalatedCount > 0) {
            return new RuleCheck("RULE_HIGH_SEVERITY_CONFLICTS", true, "WARNING",
                    String.format("Passed with safety escalation: %d high-risk conflict(s) flagged for mandatory clinician sign-off.", escalatedCount));
        }
        return new RuleCheck("RULE_HIGH_SEVERITY_CONFLICTS", true, "INFO", "Zero unresolved high severity conflicts.");
    }

    @Override
    public RuleCheck checkTemporalValueConsistency(FollowUpRecord record) {
        if (record == null) {
            return new RuleCheck("RULE_TEMPORAL_VALUE_CONSISTENCY", false, "CRITICAL", "Record is null.");
        }

        // Validate encounter date format and reasonable chronology
        if (record.getEncounterDate() != null && !record.getEncounterDate().isBlank()) {
            try {
                LocalDate encounter = LocalDate.parse(record.getEncounterDate());
                if (encounter.getYear() < 2000 || encounter.getYear() > 2100) {
                    return new RuleCheck("RULE_TEMPORAL_VALUE_CONSISTENCY", false, "CRITICAL",
                            "Encounter date " + record.getEncounterDate() + " is outside reasonable temporal bounds.");
                }
            } catch (Exception e) {
                return new RuleCheck("RULE_TEMPORAL_VALUE_CONSISTENCY", false, "CRITICAL",
                        "Encounter date format is invalid: " + record.getEncounterDate());
            }
        }

        String fullText = (record.getHistoryOfPresentIllness() + " " +
                record.getObjectiveFindingsSummary() + " " +
                record.getAssessmentSummary() + " " +
                record.getProposedFollowUpPlan()).toLowerCase();

        // Check for paradoxical allergy claims: claiming NKDA while also documenting anaphylaxis without alert banner
        if (fullText.contains("nkda") && (fullText.contains("anaphylaxis") || fullText.contains("urticaria") || fullText.contains("severe allergy"))) {
            if (record.getAllergyAlerts() == null || record.getAllergyAlerts().isEmpty()) {
                return new RuleCheck("RULE_TEMPORAL_VALUE_CONSISTENCY", false, "CRITICAL",
                        "Contradictory allergy narrative detected without prominent allergy alert banner.");
            }
        }

        return new RuleCheck("RULE_TEMPORAL_VALUE_CONSISTENCY", true, "INFO", "Temporal and value consistency verified successfully.");
    }

    @Override
    public RuleCheck checkGuardrailSafety(FollowUpRecord record) {
        if (record == null) {
            return new RuleCheck("RULE_GUARDRAIL_SAFETY", false, "CRITICAL", "Record is null.");
        }

        List<String> violations = new ArrayList<>();
        String checkText = ((record.getAssessmentSummary() != null ? record.getAssessmentSummary() : "") + " " +
                (record.getProposedFollowUpPlan() != null ? record.getProposedFollowUpPlan() : "") + " " +
                (record.getHistoryOfPresentIllness() != null ? record.getHistoryOfPresentIllness() : "")).trim();

        for (Pattern p : FORBIDDEN_GUARDRAIL_PATTERNS) {
            Matcher m = p.matcher(checkText);
            if (m.find()) {
                violations.add("'" + m.group() + "'");
            }
        }

        if (!violations.isEmpty()) {
            return new RuleCheck("RULE_GUARDRAIL_SAFETY", false, "CRITICAL",
                    "Guardrail safety policy violation: Prohibited diagnostic/prescriptive phrasing detected: " + String.join(", ", violations));
        }

        // Disclaimer verification
        if (record.getDisclaimer() == null || !record.getDisclaimer().toLowerCase().contains("clinician review")) {
            return new RuleCheck("RULE_GUARDRAIL_SAFETY", false, "CRITICAL",
                    "Guardrail safety policy violation: Mandatory clinician review disclaimer is missing.");
        }

        return new RuleCheck("RULE_GUARDRAIL_SAFETY", true, "INFO",
                "Guardrail compliance verified: No unauthorized diagnostic or prescriptive directives.");
    }

    @Override
    public RuleCheck checkPhysiologicalBounds(FollowUpRecord record) {
        if (record == null) {
            return new RuleCheck("RULE_PHYSIOLOGICAL_BOUNDS", false, "CRITICAL", "Record is null.");
        }

        String combined = (record.getObjectiveFindingsSummary() + " " +
                record.getAssessmentSummary() + " " +
                record.getHistoryOfPresentIllness()).toLowerCase();

        List<String> violations = new ArrayList<>();

        // Potassium (acceptable 1.5 - 9.0)
        Matcher kMatcher = POTASSIUM_PATTERN.matcher(combined);
        while (kMatcher.find()) {
            try {
                double val = Double.parseDouble(kMatcher.group(1));
                if (val < 1.5 || val > 9.0) {
                    violations.add("Potassium value " + val + " mEq/L is outside human physiological bounds (1.5 - 9.0)");
                }
            } catch (NumberFormatException ignored) {}
        }

        // eGFR (acceptable 0 - 250)
        Matcher egfrMatcher = EGFR_PATTERN.matcher(combined);
        while (egfrMatcher.find()) {
            try {
                double val = Double.parseDouble(egfrMatcher.group(1));
                if (val < 0.0 || val > 250.0) {
                    violations.add("eGFR value " + val + " mL/min is outside physiological bounds (0 - 250)");
                }
            } catch (NumberFormatException ignored) {}
        }

        // Heart Rate (acceptable 30 - 250)
        Matcher hrMatcher = HR_PATTERN.matcher(combined);
        while (hrMatcher.find()) {
            try {
                int val = Integer.parseInt(hrMatcher.group(1));
                if (val < 30 || val > 250) {
                    violations.add("Heart rate " + val + " bpm is outside physiological bounds (30 - 250)");
                }
            } catch (NumberFormatException ignored) {}
        }

        // Blood Pressure (systolic 50 - 260, diastolic 30 - 160)
        Matcher bpMatcher = BP_PATTERN.matcher(combined);
        while (bpMatcher.find()) {
            try {
                int sys = Integer.parseInt(bpMatcher.group(1));
                int dia = Integer.parseInt(bpMatcher.group(2));
                if (sys < 50 || sys > 260 || dia < 30 || dia > 160) {
                    violations.add("Blood pressure " + sys + "/" + dia + " mmHg is outside physiological bounds");
                }
            } catch (NumberFormatException ignored) {}
        }

        // Temperature (acceptable 85.0 - 115.0 F)
        Matcher tempMatcher = TEMP_PATTERN.matcher(combined);
        while (tempMatcher.find()) {
            try {
                double val = Double.parseDouble(tempMatcher.group(1));
                if (val < 85.0 || val > 115.0) {
                    violations.add("Temperature " + val + " F is outside physiological bounds (85 - 115 F)");
                }
            } catch (NumberFormatException ignored) {}
        }

        // Glucose (acceptable 20 - 1200 mg/dL)
        Matcher glucoseMatcher = GLUCOSE_PATTERN.matcher(combined);
        while (glucoseMatcher.find()) {
            try {
                int val = Integer.parseInt(glucoseMatcher.group(1));
                if (val < 20 || val > 1200) {
                    violations.add("Glucose value " + val + " mg/dL is outside physiological bounds (20 - 1200)");
                }
            } catch (NumberFormatException ignored) {}
        }

        if (!violations.isEmpty()) {
            return new RuleCheck("RULE_PHYSIOLOGICAL_BOUNDS", false, "CRITICAL",
                    "Physiological bounds violation: " + String.join("; ", violations));
        }

        return new RuleCheck("RULE_PHYSIOLOGICAL_BOUNDS", true, "INFO", "Physiological bounds verified successfully.");
    }
}
