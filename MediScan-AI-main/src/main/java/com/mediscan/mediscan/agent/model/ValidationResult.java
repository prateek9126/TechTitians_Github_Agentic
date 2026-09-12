package com.mediscan.mediscan.agent.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Result of automated rule-based validation checks on clinical documentation.
 */
public class ValidationResult {

    private boolean valid;
    private boolean escalationRequired;
    private List<RuleCheck> rules = new ArrayList<>();
    private List<String> errorMessages = new ArrayList<>();
    private List<String> warnings = new ArrayList<>();

    public static class RuleCheck {
        private String ruleName;
        private boolean passed;
        private String severity; // CRITICAL, WARNING, INFO
        private String details;

        public RuleCheck() {
        }

        public RuleCheck(String ruleName, boolean passed, String severity, String details) {
            this.ruleName = ruleName;
            this.passed = passed;
            this.severity = severity;
            this.details = details;
        }

        public String getRuleName() {
            return ruleName;
        }

        public void setRuleName(String ruleName) {
            this.ruleName = ruleName;
        }

        public boolean isPassed() {
            return passed;
        }

        public void setPassed(boolean passed) {
            this.passed = passed;
        }

        public String getSeverity() {
            return severity;
        }

        public void setSeverity(String severity) {
            this.severity = severity;
        }

        public String getDetails() {
            return details;
        }

        public void setDetails(String details) {
            this.details = details;
        }

        public String getMessage() {
            return details;
        }

        public void setMessage(String message) {
            this.details = message;
        }
    }

    public ValidationResult() {
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public boolean isEscalationRequired() {
        return escalationRequired;
    }

    public void setEscalationRequired(boolean escalationRequired) {
        this.escalationRequired = escalationRequired;
    }

    public List<RuleCheck> getRules() {
        return rules;
    }

    public void setRules(List<RuleCheck> rules) {
        this.rules = rules;
    }

    public List<String> getErrorMessages() {
        return errorMessages;
    }

    public void setErrorMessages(List<String> errorMessages) {
        this.errorMessages = errorMessages;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public void setWarnings(List<String> warnings) {
        this.warnings = warnings;
    }
}
