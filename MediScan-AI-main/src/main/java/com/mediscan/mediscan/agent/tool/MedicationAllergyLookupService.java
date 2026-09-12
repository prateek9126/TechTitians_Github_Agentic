package com.mediscan.mediscan.agent.tool;

import com.mediscan.mediscan.agent.model.AllergyRecord;
import com.mediscan.mediscan.agent.model.ToolCallLog;

import java.util.List;

/**
 * Tool interface for cross-referencing medications against patient allergy lists and drug interaction tables.
 */
public interface MedicationAllergyLookupService {

    List<String> check(List<String> currentMeds, String newDrug);

    List<String> checkAllergies(List<String> medications, List<String> documentedAllergies);

    MedicationSafetyReport evaluateSafety(List<String> medications,
                                          List<AllergyRecord> documentedAllergies,
                                          List<String> historicalReactionsFromNotes,
                                          boolean simulateFailure,
                                          ToolCallLog log);

    class MedicationSafetyReport {
        private boolean safe;
        private List<String> severeAllergyWarnings;
        private List<String> drugInteractionWarnings;
        private List<String> clinicalRecommendations;

        public MedicationSafetyReport() {
        }

        public MedicationSafetyReport(boolean safe, List<String> severeAllergyWarnings,
                                      List<String> drugInteractionWarnings,
                                      List<String> clinicalRecommendations) {
            this.safe = safe;
            this.severeAllergyWarnings = severeAllergyWarnings;
            this.drugInteractionWarnings = drugInteractionWarnings;
            this.clinicalRecommendations = clinicalRecommendations;
        }

        public boolean isSafe() {
            return safe;
        }

        public void setSafe(boolean safe) {
            this.safe = safe;
        }

        public List<String> getSevereAllergyWarnings() {
            return severeAllergyWarnings;
        }

        public void setSevereAllergyWarnings(List<String> severeAllergyWarnings) {
            this.severeAllergyWarnings = severeAllergyWarnings;
        }

        public List<String> getDrugInteractionWarnings() {
            return drugInteractionWarnings;
        }

        public void setDrugInteractionWarnings(List<String> drugInteractionWarnings) {
            this.drugInteractionWarnings = drugInteractionWarnings;
        }

        public List<String> getClinicalRecommendations() {
            return clinicalRecommendations;
        }

        public void setClinicalRecommendations(List<String> clinicalRecommendations) {
            this.clinicalRecommendations = clinicalRecommendations;
        }
    }
}
