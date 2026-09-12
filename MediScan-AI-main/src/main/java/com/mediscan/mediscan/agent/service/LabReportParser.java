package com.mediscan.mediscan.agent.service;

import com.mediscan.mediscan.agent.model.LabResult;

import java.time.LocalDate;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility parser for extracting structured LabResult items from unstructured raw clinical lab text.
 * Reused exclusively for text-to-data parsing without calling LLM single-shot summarization.
 */
public class LabReportParser {

    private static final Pattern DATE_PATTERN = Pattern.compile(
            "\\b(20\\d{2}[-/.]\\d{1,2}[-/.]\\d{1,2}|\\d{1,2}[-/.]\\d{1,2}[-/.]20\\d{2}|(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*\\s+\\d{1,2},?\\s+20\\d{2})\\b",
            Pattern.CASE_INSENSITIVE
    );

    // Standard clinical biomarker regex mappings: [Test Display Name, Regex Pattern, Default Unit]
    private static final List<BiomarkerDefinition> BIOMARKERS = List.of(
            new BiomarkerDefinition("Potassium", "(?i)\\b(?:potassium|k\\+)\\b[:\\s]*([<>]?\\s*\\d+(?:\\.\\d+)?)\\s*([a-zA-Z/]+)?", "mEq/L"),
            new BiomarkerDefinition("Serum Creatinine", "(?i)\\b(?:serum\\s+)?creatinine\\b[:\\s]*([<>]?\\s*\\d+(?:\\.\\d+)?)\\s*([a-zA-Z/]+)?", "mg/dL"),
            new BiomarkerDefinition("eGFR", "(?i)\\begfr\\b[:\\s]*([<>]?\\s*\\d+(?:\\.\\d+)?)\\s*([a-zA-Z0-9/.\u00B2\\^2]+)?", "mL/min/1.73m2"),
            new BiomarkerDefinition("Fasting Blood Glucose", "(?i)\\b(?:fasting\\s+)?(?:blood\\s+)?glucose\\b[:\\s]*([<>]?\\s*\\d+(?:\\.\\d+)?)\\s*([a-zA-Z/]+)?", "mg/dL"),
            new BiomarkerDefinition("Hemoglobin A1c", "(?i)\\b(?:hba1c|hemoglobin\\s+a1c|glycated\\s+hemoglobin)\\b[:\\s]*([<>]?\\s*\\d+(?:\\.\\d+)?)\\s*([%a-zA-Z/]+)?", "%"),
            new BiomarkerDefinition("Sodium", "(?i)\\b(?:sodium|na\\+)\\b[:\\s]*([<>]?\\s*\\d+(?:\\.\\d+)?)\\s*([a-zA-Z/]+)?", "mEq/L"),
            new BiomarkerDefinition("Blood Urea Nitrogen (BUN)", "(?i)\\b(?:blood\\s+urea\\s+nitrogen|bun)\\b[:\\s]*([<>]?\\s*\\d+(?:\\.\\d+)?)\\s*([a-zA-Z/]+)?", "mg/dL"),
            new BiomarkerDefinition("Hemoglobin", "(?i)\\b(?:hemoglobin|hgb)\\b[:\\s]*([<>]?\\s*\\d+(?:\\.\\d+)?)\\s*([a-zA-Z/]+)?", "g/dL"),
            new BiomarkerDefinition("Platelet Count", "(?i)\\b(?:platelets?|plt)\\b[:\\s]*([<>]?\\s*\\d+(?:\\.\\d+)?)\\s*([a-zA-Z0-9/]+)?", "10^3/uL"),
            new BiomarkerDefinition("White Blood Cells (WBC)", "(?i)\\b(?:wbc|white\\s+blood\\s+cells?)\\b[:\\s]*([<>]?\\s*\\d+(?:\\.\\d+)?)\\s*([a-zA-Z0-9/]+)?", "10^3/uL"),
            new BiomarkerDefinition("Calcium", "(?i)\\bcalcium\\b[:\\s]*([<>]?\\s*\\d+(?:\\.\\d+)?)\\s*([a-zA-Z/]+)?", "mg/dL"),
            new BiomarkerDefinition("Total Cholesterol", "(?i)\\b(?:total\\s+)?cholesterol\\b[:\\s]*([<>]?\\s*\\d+(?:\\.\\d+)?)\\s*([a-zA-Z/]+)?", "mg/dL")
    );

    // Generic line pattern matching: "Test Name: 12.3 mg/dL" or "Test Name  12.3  mg/dL"
    private static final Pattern GENERIC_LINE_PATTERN = Pattern.compile(
            "^[\\s*•-]*([A-Za-z0-9/\\s()_-]{2,35}?)\\s*[:=\\-|\t]\\s*([<>]?\\s*\\d+(?:\\.\\d+)?)\\s*([a-zA-Z/%]+(?:/[a-zA-Z0-9.]+)?)\\b",
            Pattern.MULTILINE
    );

    /**
     * Extracts structured lab results from raw text.
     */
    public static List<LabResult> extractLabValues(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            return Collections.emptyList();
        }

        List<LabResult> results = new ArrayList<>();
        Set<String> matchedKeys = new HashSet<>();

        // 1. Detect test date if present
        String defaultDate = LocalDate.now().toString();
        Matcher dateMatcher = DATE_PATTERN.matcher(rawText);
        if (dateMatcher.find()) {
            defaultDate = dateMatcher.group(1).trim();
        }

        // 2. Specific known biomarker extraction
        for (BiomarkerDefinition def : BIOMARKERS) {
            Pattern p = Pattern.compile(def.regex());
            Matcher m = p.matcher(rawText);
            if (m.find()) {
                String val = m.group(1).replaceAll("\\s+", "");
                String unit = m.groupCount() >= 2 && m.group(2) != null && !m.group(2).isBlank()
                        ? m.group(2).trim()
                        : def.defaultUnit();

                results.add(new LabResult(def.name(), val, unit, defaultDate));
                matchedKeys.add(def.name().toLowerCase());
            }
        }

        // 3. Fallback to generic line extraction for tests not in the standard list
        Matcher lineMatcher = GENERIC_LINE_PATTERN.matcher(rawText);
        while (lineMatcher.find()) {
            String test = lineMatcher.group(1).trim();
            String val = lineMatcher.group(2).replaceAll("\\s+", "");
            String unit = lineMatcher.group(3).trim();

            // Filter out noise or already matched tests
            String testLower = test.toLowerCase();
            if (testLower.contains("date") || testLower.contains("page") || testLower.contains("patient") ||
                    testLower.contains("doctor") || testLower.contains("hospital") || testLower.contains("phone") ||
                    testLower.contains("age") || testLower.contains("id")) {
                continue;
            }

            boolean alreadyCovered = false;
            for (String key : matchedKeys) {
                if (testLower.contains(key) || key.contains(testLower)) {
                    alreadyCovered = true;
                    break;
                }
            }

            if (!alreadyCovered && !matchedKeys.contains(testLower) && test.length() >= 2) {
                results.add(new LabResult(test, val, unit, defaultDate));
                matchedKeys.add(testLower);
            }
        }

        return results;
    }

    private record BiomarkerDefinition(String name, String regex, String defaultUnit) {}
}
