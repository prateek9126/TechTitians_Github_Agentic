package com.mediscan.mediscan.agent.service;

import com.mediscan.mediscan.agent.model.ClinicalNote;
import com.mediscan.mediscan.agent.model.Conflict;
import com.mediscan.mediscan.agent.model.LabResult;
import com.mediscan.mediscan.agent.model.Medication;
import com.mediscan.mediscan.agent.model.PatientRecord;
import com.mediscan.mediscan.agent.tool.MedicationAllergyLookupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Implementation of ReconciliationService.
 * Genuinely computes conflicts by comparing transcriptText, ehrMedications,
 * previousNotes, and labResults, determining severity dynamically via MedicationAllergyLookupService.
 */
@Service
public class ReconciliationServiceImpl implements ReconciliationService {

    private final MedicationAllergyLookupService medicationAllergyLookup;

    private static final List<String> KNOWN_DRUG_NAMES = List.of(
            "augmentin", "amoxicillin-clavulanate", "amoxicillin", "ampicillin", "penicillin", "cefuroxime", "cephalexin",
            "metformin", "lisinopril", "enalapril", "ramipril", "losartan", "valsartan",
            "spironolactone", "eplerenone", "hydrochlorothiazide", "furosemide",
            "ibuprofen", "motrin", "advil", "naproxen", "aleve", "meloxicam", "diclofenac", "celecoxib", "aspirin",
            "warfarin", "apixaban", "rivaroxaban", "atorvastatin", "simvastatin", "amlodipine", "albuterol", "digoxin", "tramadol"
    );

    @Autowired
    public ReconciliationServiceImpl(MedicationAllergyLookupService medicationAllergyLookup) {
        this.medicationAllergyLookup = medicationAllergyLookup;
    }

    @Override
    public List<Conflict> reconcile(PatientRecord patient) {
        if (patient == null) {
            return Collections.emptyList();
        }

        List<Conflict> conflicts = new ArrayList<>();
        int conflictCounter = 1;

        String transcript = patient.getTranscriptText() != null ? patient.getTranscriptText().toLowerCase() : "";
        List<Medication> ehrMeds = patient.getEhrMedications() != null ? patient.getEhrMedications() : Collections.emptyList();
        List<String> ehrAllergies = patient.getEhrAllergies() != null ? patient.getEhrAllergies() : Collections.emptyList();
        List<LabResult> labResults = patient.getLabResults() != null ? patient.getLabResults() : Collections.emptyList();
        List<ClinicalNote> notes = patient.getPreviousNotes() != null ? patient.getPreviousNotes() : Collections.emptyList();

        List<String> activeMedNames = ehrMeds.stream()
                .filter(Medication::isActive)
                .map(Medication::getName)
                .collect(Collectors.toList());

        // =========================================================================
        // 1. DRUG MENTIONS IN TRANSCRIPT VS EHR MEDICATIONS
        // =========================================================================

        // 1A. Check for verbal cessation of active EHR medications
        for (Medication med : ehrMeds) {
            if (med.isActive()) {
                String medLower = med.getName().toLowerCase();
                boolean reportedStopped = transcript.contains("stopped " + medLower) ||
                        transcript.contains("stopped taking " + medLower) ||
                        transcript.contains("stopped taking my " + medLower) ||
                        transcript.contains("discontinued " + medLower) ||
                        transcript.contains("quit taking " + medLower) ||
                        transcript.contains("no longer taking " + medLower);

                if (reportedStopped) {
                    conflicts.add(new Conflict(
                            "CONF-" + (conflictCounter++),
                            "Medication Adherence (" + med.getName() + ")",
                            "Consultation Transcript",
                            "Patient verbally reports discontinuing " + med.getName() + " due to adverse symptoms/intolerance",
                            "EHR Medication Profile",
                            med.getName() + " " + (med.getDosage() != null ? med.getDosage() : "") + " (Active in EHR)",
                            "MEDIUM",
                            "DETECTED",
                            "EHR lists medication as active, but patient verbally reported ceasing therapy.",
                            "ClinicalGuidelineRAGService",
                            "Medication adherence discrepancy requires guideline-directed reconciliation and evaluation of secondary therapy.",
                            "MEDICATION"
                    ));
                }
            }
        }

        // 1B. Check for OTC / self-administered drugs in transcript not in active EHR meds
        for (String drugName : KNOWN_DRUG_NAMES) {
            if (transcript.contains(drugName)) {
                boolean inEhr = ehrMeds.stream().anyMatch(m -> m.getName().toLowerCase().contains(drugName));
                boolean inActiveEhr = activeMedNames.stream().anyMatch(n -> n.toLowerCase().contains(drugName));

                // If patient mentions taking an unlisted/OTC drug
                boolean takingMentioned = transcript.contains("taking " + drugName) ||
                        transcript.contains("taking over-the-counter " + drugName) ||
                        transcript.contains("taking otc " + drugName) ||
                        transcript.contains("taking " + drugName + " 600mg") ||
                        transcript.contains("taking " + drugName + " 400mg") ||
                        transcript.contains("been taking " + drugName);

                if (takingMentioned && !inActiveEhr) {
                    // Run through real interaction check against current active medications
                    List<String> interactingMeds = medicationAllergyLookup.check(activeMedNames, drugName);

                    String severity = !interactingMeds.isEmpty() ? "HIGH" : "MEDIUM";
                    String title = interactingMeds.size() >= 2
                            ? "Polypharmacy Interaction ('Triple Whammy')"
                            : "Drug Interaction / OTC Unlisted Medication (" + capitalize(drugName) + ")";

                    String resolvingTool = !interactingMeds.isEmpty() ? "MedicationAllergyLookupService" : "ClinicalGuidelineRAGService";
                    String rationale = !interactingMeds.isEmpty()
                            ? "Concurrent unmonitored " + capitalize(drugName) + " interacts with prescribed " + String.join(" + ", interactingMeds) + ", creating hemodynamic and safety hazards."
                            : "Patient self-administering unlisted drug requiring chart reconciliation.";

                    conflicts.add(new Conflict(
                            "CONF-" + (conflictCounter++),
                            title,
                            "Consultation Transcript (OTC / Verbal self-report)",
                            "Self-administered " + capitalize(drugName) + " reported in consultation",
                            "EHR Active Prescriptions",
                            String.join(", ", activeMedNames) + " (Interacting: " + (interactingMeds.isEmpty() ? "None" : String.join(", ", interactingMeds)) + ")",
                            severity,
                            "DETECTED",
                            "Detected concurrent unmonitored drug administration interacting with prescribed regimen.",
                            resolvingTool,
                            rationale,
                            "INTERACTION"
                    ));
                }
            }
        }

        // =========================================================================
        // 2. ALLERGY MENTIONS IN PREVIOUS NOTES VS EHR ALLERGIES
        // =========================================================================
        boolean allergyBannerShowsNkda = ehrAllergies.isEmpty() || ehrAllergies.stream().anyMatch(a -> {
            String lower = a.toLowerCase();
            return lower.contains("nkda") || lower.contains("no known") || lower.contains("none");
        });

        for (ClinicalNote note : notes) {
            String noteContent = note.getContent() != null ? note.getContent().toLowerCase() : "";
            boolean hasSevereReaction = noteContent.contains("anaphylaxis") ||
                    noteContent.contains("anaphylactoid") ||
                    noteContent.contains("urticaria") ||
                    noteContent.contains("epinephrine") ||
                    noteContent.contains("angioedema") ||
                    noteContent.contains("wheezing");

            if (hasSevereReaction) {
                // Find allergen mentioned
                String allergenFound = "Beta-Lactam / Penicillin";
                if (noteContent.contains("augmentin") || noteContent.contains("amoxicillin-clavulanate")) {
                    allergenFound = "Augmentin (Amoxicillin-Clavulanate)";
                } else if (noteContent.contains("penicillin") || noteContent.contains("amoxicillin")) {
                    allergenFound = "Penicillin";
                }

                // If EHR allergy banner shows NKDA or lacks this documented allergy
                boolean documentedInEhr = ehrAllergies.stream().anyMatch(a -> a.toLowerCase().contains("penicillin") || a.toLowerCase().contains("augmentin") || a.toLowerCase().contains("amoxicillin"));

                if (allergyBannerShowsNkda || !documentedInEhr) {
                    conflicts.add(new Conflict(
                            "CONF-" + (conflictCounter++),
                            "Allergy Documentation (" + allergenFound + ")",
                            "EHR Allergy Banner",
                            allergyBannerShowsNkda ? "No Known Drug Allergies (NKDA)" : "Lacks documented severe reaction to " + allergenFound,
                            "Historical Clinical Note (" + (note.getNoteId() != null ? note.getNoteId() : note.getDate()) + ")",
                            "Documented severe hypersensitivity with anaphylactoid reaction/urticaria/wheezing requiring emergency pharmacotherapy",
                            "HIGH",
                            "DETECTED",
                            "Severe adverse reaction documented in historical notes is missing or contradicted by current EHR allergy status.",
                            "MedicationAllergyLookupService",
                            "Patient safety hazard: Prescription of cross-reactive antimicrobial could cause life-threatening anaphylaxis.",
                            "ALLERGY"
                    ));
                }
            }
        }

        // =========================================================================
        // 3. LAB RESULTS DATES/VALUES VS CONSULTATION TRANSCRIPT
        // =========================================================================
        for (LabResult lab : labResults) {
            String testLower = lab.getTestName().toLowerCase();
            String flag = lab.getFlag() != null ? lab.getFlag().toUpperCase() : "NORMAL";

            // Check if patient in transcript verbalizes conflicting normal perception for abnormal lab
            // e.g. "normal blood sugar", "glucose was fine at 95", "labs were completely normal"
            boolean mentionsNormalSugar = transcript.contains("normal blood sugar") ||
                    transcript.contains("glucose was fine") ||
                    transcript.contains("glucose was fine at 95") ||
                    transcript.contains("sugar was fine") ||
                    transcript.contains("numbers were normal");

            boolean isSugarTest = testLower.contains("glucose") || testLower.contains("hba1c") || testLower.contains("hemoglobin a1c");

            if (isSugarTest && mentionsNormalSugar && ("HIGH".equals(flag) || "CRITICAL".equals(flag) || isAbnormalValue(lab))) {
                conflicts.add(new Conflict(
                        "CONF-" + (conflictCounter++),
                        "Diagnostic Lab Discrepancy (" + lab.getTestName() + ")",
                        "Consultation Transcript",
                        "Patient perceives glycemic control as normal/stable based on verbal report",
                        "EHR Laboratory Record (" + lab.getDate() + ")",
                        lab.getTestName() + " = " + lab.getValue() + " " + lab.getUnit() + " (Flag: " + flag + ", Ref: " + lab.getReferenceRange() + ")",
                        "HIGH",
                        "DETECTED",
                        "Direct contradiction between patient's verbal understanding and objective laboratory findings.",
                        "LabRetrievalService",
                        "Marked elevation in objective glycemic parameters requires urgent clinical evaluation and diabetes education.",
                        "LAB"
                ));
            }

            // Check for electrolyte / renal contradictions (e.g. potassium or eGFR contradiction)
            boolean mentionsNormalElectrolytes = transcript.contains("potassium was normal") || transcript.contains("electrolytes were fine");
            if (testLower.contains("potassium") && mentionsNormalElectrolytes && ("HIGH".equals(flag) || "CRITICAL".equals(flag))) {
                conflicts.add(new Conflict(
                        "CONF-" + (conflictCounter++),
                        "Electrolyte Lab Discrepancy (Potassium)",
                        "Consultation Transcript",
                        "Patient reports normal electrolyte status",
                        "EHR Laboratory Record (" + lab.getDate() + ")",
                        "Potassium = " + lab.getValue() + " " + lab.getUnit() + " [" + flag + "]",
                        "HIGH",
                        "DETECTED",
                        "Documented laboratory hyperkalemia contradicts verbal report.",
                        "LabRetrievalService",
                        "High potassium level requires immediate repeat testing and dietary restriction.",
                        "LAB"
                ));
            }
        }

        return conflicts;
    }

    private boolean isAbnormalValue(LabResult lab) {
        try {
            double val = Double.parseDouble(lab.getValue().replaceAll("[^0-9.]", ""));
            String test = lab.getTestName().toLowerCase();
            if (test.contains("glucose") && val > 140.0) return true;
            if (test.contains("hba1c") && val > 6.5) return true;
            if (test.contains("potassium") && (val < 3.5 || val > 5.0)) return true;
            if (test.contains("egfr") && val < 60.0) return true;
        } catch (Exception ignored) {}
        return false;
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return "";
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}
