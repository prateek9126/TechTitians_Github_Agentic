package com.mediscan.mediscan.agent.tool;

import com.mediscan.mediscan.agent.model.AllergyRecord;
import com.mediscan.mediscan.agent.model.ToolCallLog;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

/**
 * Implementation of MedicationAllergyLookupService.
 * Maintains static interaction map of known drug-interaction pairs and evaluates real safety profiles.
 */
@Service
public class MedicationAllergyLookupServiceImpl implements MedicationAllergyLookupService {

    // Real static Map of known drug-interaction pairs (normalized to lowercase generic/class names)
    private static final Map<String, Set<String>> INTERACTION_MAP = new HashMap<>();

    static {
        // 1. NSAIDs
        Set<String> nsaidInteractions = Set.of(
                "lisinopril", "enalapril", "ramipril", "captopril", "benazepril", "losartan", "valsartan", // ACEi / ARBs
                "spironolactone", "eplerenone", "triamterene", "furosemide", "hydrochlorothiazide",        // Diuretics
                "warfarin", "apixaban", "rivaroxaban", "dabigatran", "heparin",                             // Anticoagulants
                "methotrexate", "lithium"
        );
        for (String nsaid : List.of("ibuprofen", "motrin", "advil", "naproxen", "aleve", "meloxicam", "diclofenac", "indomethacin", "celecoxib", "nsaid")) {
            INTERACTION_MAP.put(nsaid, nsaidInteractions);
        }

        // 2. ACE Inhibitors & ARBs
        Set<String> aceiInteractions = Set.of(
                "spironolactone", "eplerenone", "triamterene", // K-sparing diuretics (hyperkalemia risk)
                "ibuprofen", "naproxen", "meloxicam", "diclofenac", "celecoxib", // NSAIDs
                "potassium", "lithium"
        );
        for (String acei : List.of("lisinopril", "enalapril", "ramipril", "captopril", "benazepril", "losartan", "valsartan")) {
            INTERACTION_MAP.put(acei, aceiInteractions);
        }

        // 3. Potassium-Sparing Diuretics
        Set<String> diureticInteractions = Set.of(
                "lisinopril", "enalapril", "ramipril", "losartan", "valsartan",
                "ibuprofen", "naproxen", "meloxicam", "diclofenac",
                "potassium"
        );
        for (String kDiuretic : List.of("spironolactone", "eplerenone", "triamterene")) {
            INTERACTION_MAP.put(kDiuretic, diureticInteractions);
        }

        // 4. Warfarin / Anticoagulants
        Set<String> warfarinInteractions = Set.of(
                "ibuprofen", "naproxen", "aspirin", "meloxicam", "diclofenac", "celecoxib",
                "amiodarone", "fluconazole", "metronidazole", "ciprofloxacin"
        );
        INTERACTION_MAP.put("warfarin", warfarinInteractions);

        // 5. Digoxin
        Set<String> digoxinInteractions = Set.of(
                "amiodarone", "verapamil", "clarithromycin", "spironolactone"
        );
        INTERACTION_MAP.put("digoxin", digoxinInteractions);

        // 6. Statins (Simvastatin / Atorvastatin)
        Set<String> statinInteractions = Set.of(
                "gemfibrozil", "clarithromycin", "erythromycin", "amiodarone", "ketoconazole"
        );
        for (String statin : List.of("simvastatin", "atorvastatin", "lovastatin")) {
            INTERACTION_MAP.put(statin, statinInteractions);
        }

        // 7. Ciprofloxacin / Fluoroquinolones
        Set<String> ciproInteractions = Set.of(
                "theophylline", "tizanidine", "warfarin"
        );
        INTERACTION_MAP.put("ciprofloxacin", ciproInteractions);

        // 8. Tramadol
        Set<String> tramadolInteractions = Set.of(
                "fluoxetine", "sertraline", "paroxetine", "citalopram", "escitalopram", "linezolid" // Serotonin syndrome
        );
        INTERACTION_MAP.put("tramadol", tramadolInteractions);

        // 9. Methotrexate
        Set<String> mtxInteractions = Set.of(
                "ibuprofen", "naproxen", "diclofenac", "aspirin"
        );
        INTERACTION_MAP.put("methotrexate", mtxInteractions);
    }

    private static final Set<String> PENICILLIN_DRUGS = Set.of(
            "amoxicillin", "augmentin", "amoxicillin-clavulanate", "ampicillin", "penicillin", "piperacillin", "cefuroxime", "cephalexin"
    );

    @Override
    public List<String> check(List<String> currentMeds, String newDrug) {
        if (newDrug == null || currentMeds == null || currentMeds.isEmpty()) {
            return Collections.emptyList();
        }

        String normalizedNew = normalizeDrugName(newDrug);
        Set<String> conflictingWithNew = INTERACTION_MAP.getOrDefault(normalizedNew, Collections.emptySet());

        List<String> conflicts = new ArrayList<>();
        for (String med : currentMeds) {
            String normalizedCurrent = normalizeDrugName(med);
            // Check direct interaction map lookup both directions
            if (conflictingWithNew.contains(normalizedCurrent)) {
                conflicts.add(med);
            } else {
                Set<String> conflictingWithCurrent = INTERACTION_MAP.getOrDefault(normalizedCurrent, Collections.emptySet());
                if (conflictingWithCurrent.contains(normalizedNew)) {
                    conflicts.add(med);
                }
            }
        }
        return conflicts;
    }

    @Override
    public List<String> checkAllergies(List<String> medications, List<String> documentedAllergies) {
        if (medications == null || documentedAllergies == null) {
            return Collections.emptyList();
        }

        boolean hasPenicillinAllergy = documentedAllergies.stream().anyMatch(a -> {
            String lower = a.toLowerCase();
            return (lower.contains("penicillin") || lower.contains("amoxicillin") || lower.contains("augmentin"))
                    && !lower.contains("nkda") && !lower.contains("no known");
        });

        List<String> flagged = new ArrayList<>();
        for (String med : medications) {
            String normalized = normalizeDrugName(med);
            if (hasPenicillinAllergy && PENICILLIN_DRUGS.contains(normalized)) {
                flagged.add(med);
            }
        }
        return flagged;
    }

    @Override
    public MedicationSafetyReport evaluateSafety(List<String> medications,
                                                List<AllergyRecord> documentedAllergies,
                                                List<String> historicalReactionsFromNotes,
                                                boolean simulateFailure,
                                                ToolCallLog log) {
        long startTime = System.currentTimeMillis();
        String timestamp = Instant.now().toString();

        if (log != null) {
            log.setId("TOOL-" + UUID.randomUUID().toString().substring(0, 8));
            log.setToolName("MedicationAllergyLookupService");
            log.setCallReason("Evaluate pharmacologic cross-reactivity and adverse drug-drug interactions");
            log.setInputSummary("Meds: " + medications + " | Allergies: " + (documentedAllergies != null ? documentedAllergies.size() : 0));
            log.setTimestamp(timestamp);
        }

        if (simulateFailure) {
            long duration = System.currentTimeMillis() - startTime;
            if (log != null) {
                log.setStatus("FAILED");
                log.setDurationMs(duration);
                log.setOutputSummary("Simulated Failure: Drug Interaction Knowledge Base timed out (504)");
                log.setErrorDetails("Timeout reading National Drug Database table");
            }
            throw new RuntimeException("Simulated Failure: MedicationAllergyLookupService timed out");
        }

        List<String> allergyWarnings = new ArrayList<>();
        List<String> interactionWarnings = new ArrayList<>();
        List<String> recommendations = new ArrayList<>();

        // 1. Cross-reference documented allergies & historical note reactions
        List<String> allAllergies = new ArrayList<>();
        if (documentedAllergies != null) {
            for (AllergyRecord ar : documentedAllergies) {
                allAllergies.add(ar.getAllergen());
            }
        }
        if (historicalReactionsFromNotes != null) {
            allAllergies.addAll(historicalReactionsFromNotes);
        }

        List<String> allergicMeds = checkAllergies(medications, allAllergies);
        for (String med : allergicMeds) {
            allergyWarnings.add(String.format("CRITICAL CONTRAINDICATION: Proposed medication '%s' has cross-reactivity with documented beta-lactam hypersensitivity. High risk of anaphylaxis.", med));
            recommendations.add(String.format("Withhold '%s'; recommend non-beta-lactam antimicrobial alternative (e.g. Azithromycin or Doxycycline per clinician evaluation).", med));
        }

        // 2. Cross-reference Drug-Drug Interactions from real map
        Set<String> reportedPairs = new HashSet<>();
        for (int i = 0; i < medications.size(); i++) {
            String medA = medications.get(i);
            List<String> currentRest = new ArrayList<>(medications);
            currentRest.remove(i);
            List<String> conflicts = check(currentRest, medA);
            for (String conf : conflicts) {
                String pairKey = medA.compareToIgnoreCase(conf) < 0 ? medA + "+" + conf : conf + "+" + medA;
                if (!reportedPairs.contains(pairKey.toLowerCase())) {
                    reportedPairs.add(pairKey.toLowerCase());
                    String normA = normalizeDrugName(medA);
                    String normConf = normalizeDrugName(conf);

                    // Check specifically if Triple Whammy is present (NSAID + ACEi + Diuretic)
                    boolean hasNsaid = List.of(normA, normConf).stream().anyMatch(n -> INTERACTION_MAP.containsKey(n) && (n.equals("ibuprofen") || n.equals("naproxen") || n.equals("diclofenac")));
                    boolean hasAcei = List.of(normA, normConf).stream().anyMatch(n -> n.equals("lisinopril") || n.equals("enalapril") || n.equals("losartan"));
                    boolean hasDiuretic = medications.stream().map(this::normalizeDrugName).anyMatch(n -> n.equals("spironolactone") || n.equals("hydrochlorothiazide"));

                    if (hasNsaid && hasAcei && hasDiuretic) {
                        interactionWarnings.add("HIGH-RISK 'TRIPLE WHAMMY' INTERACTION: Concurrent NSAID (" + medA + ") + ACE-Inhibitor + Potassium-sparing Diuretic exponentially increases risk of acute renal hemodynamic failure and dangerous hyperkalemia.");
                        recommendations.add("Advise patient to immediately discontinue OTC NSAIDs; evaluate topical analgesics (e.g. Topical Voltaren) or Acetaminophen; order repeat Basic Metabolic Panel within 7-14 days.");
                    } else {
                        interactionWarnings.add(String.format("KNOWN DRUG INTERACTION: '%s' and '%s' have a clinically significant documented interaction.", medA, conf));
                        recommendations.add(String.format("Review concurrent use of '%s' and '%s' for potential dose adjustment or alternative therapy.", medA, conf));
                    }
                }
            }
        }

        boolean safe = allergyWarnings.isEmpty() && interactionWarnings.isEmpty();
        long duration = System.currentTimeMillis() - startTime;

        if (log != null) {
            log.setStatus("SUCCESS");
            log.setDurationMs(duration);
            log.setOutputSummary(String.format("Safety Evaluation: Safe=%b | Allergy Warnings: %d | Interaction Warnings: %d",
                    safe, allergyWarnings.size(), interactionWarnings.size()));
        }

        return new MedicationSafetyReport(safe, allergyWarnings, interactionWarnings, recommendations);
    }

    private String normalizeDrugName(String raw) {
        if (raw == null) return "";
        String clean = raw.toLowerCase().trim();
        for (String known : List.of(
                "amoxicillin-clavulanate", "amoxicillin", "augmentin", "ampicillin", "penicillin", "cefuroxime",
                "ibuprofen", "motrin", "advil", "naproxen", "aleve", "meloxicam", "diclofenac", "celecoxib",
                "lisinopril", "enalapril", "ramipril", "losartan", "valsartan",
                "spironolactone", "eplerenone", "hydrochlorothiazide", "furosemide",
                "warfarin", "metformin", "amlodipine", "atorvastatin", "simvastatin",
                "digoxin", "theophylline", "tramadol", "methotrexate"
        )) {
            if (clean.contains(known)) {
                return known;
            }
        }
        return clean.split("\\s+")[0];
    }
}
