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
            "\\b(20\\d{2}[-/.]\\d{1,2}[-/.]\\d{1,2}|\\d{1,2}[-/.]\\d{1,2}[-/.]20\\d{2}|\\d{1,2}-(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*-20\\d{2}|(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*\\s+\\d{1,2},?\\s+20\\d{2})\\b",
            Pattern.CASE_INSENSITIVE
    );

    // Standard clinical biomarker definitions:
    // [Name, Regex, Default Unit, Default Ref Range, Normal Min, Normal Max]
    private static final List<BiomarkerDefinition> BIOMARKERS = List.of(
            // --- Complete Blood Count (CBC) ---
            new BiomarkerDefinition("Hemoglobin (Hb)", "(?i)\\b(?:hemoglobin|hgb|hb)\\b(?:\\s*\\([a-z]+\\))?[:\\s]*([<>]?\\s*\\d+(?:\\.\\d+)?)\\s*([a-zA-Z/]+)?", "g/dL", "13.0 - 17.0", 13.0, 17.0),
            new BiomarkerDefinition("RBC Count", "(?i)\\b(?:rbc(?:\\s*count)?|red\\s+blood\\s+cells?)\\b[:\\s]*([<>]?\\s*\\d+(?:\\.\\d+)?)\\s*([a-zA-Z/]+)?", "million/cumm", "4.5 - 5.5", 4.5, 5.5),
            new BiomarkerDefinition("Hematocrit (PCV)", "(?i)\\b(?:hematocrit|pcv|packed\\s+cell\\s+volume)\\b(?:\\s*\\([a-z]+\\))?[:\\s]*([<>]?\\s*\\d+(?:\\.\\d+)?)\\s*([%a-zA-Z/]+)?", "%", "40 - 50", 40.0, 50.0),
            new BiomarkerDefinition("MCV", "(?i)\\b(?:mcv|mean\\s+corpuscular\\s+volume)\\b[:\\s]*([<>]?\\s*\\d+(?:\\.\\d+)?)\\s*([a-zA-Z/]+)?", "fL", "83 - 101", 83.0, 101.0),
            new BiomarkerDefinition("MCH", "(?i)\\b(?:mch|mean\\s+corpuscular\\s+hemoglobin(?!\\s*concentration))\\b[:\\s]*([<>]?\\s*\\d+(?:\\.\\d+)?)\\s*([a-zA-Z/]+)?", "pg", "27 - 32", 27.0, 32.0),
            new BiomarkerDefinition("MCHC", "(?i)\\b(?:mchc|mean\\s+corpuscular\\s+hemoglobin\\s+concentration)\\b[:\\s]*([<>]?\\s*\\d+(?:\\.\\d+)?)\\s*([a-zA-Z/]+)?", "g/dL", "31.5 - 34.5", 31.5, 34.5),
            new BiomarkerDefinition("RDW", "(?i)\\b(?:rdw(?:\\s*-\\s*cv)?|red\\s+cell\\s+distribution\\s+width)\\b[:\\s]*([<>]?\\s*\\d+(?:\\.\\d+)?)\\s*([%a-zA-Z/]+)?", "%", "11.6 - 14.0", 11.6, 14.0),
            new BiomarkerDefinition("Total WBC Count", "(?i)\\b(?:total\\s+)?(?:wbc(?:\\s*count)?|white\\s+blood\\s+cells?|leukocytes?)\\b[:\\s]*([<>]?\\s*\\d{1,3}(?:,\\d{3})*(?:\\.\\d+)?|\\d+(?:\\.\\d+)?)\\s*([a-zA-Z0-9/%]+)?", "/cumm", "4,000 - 11,000", 4000.0, 11000.0),
            new BiomarkerDefinition("Neutrophils", "(?i)\\b(?:neutrophils?|polys?)\\b[:\\s]*([<>]?\\s*\\d+(?:\\.\\d+)?)\\s*([%a-zA-Z/]+)?", "%", "40 - 80", 40.0, 80.0),
            new BiomarkerDefinition("Lymphocytes", "(?i)\\b(?:lymphocytes?|lymphs?)\\b[:\\s]*([<>]?\\s*\\d+(?:\\.\\d+)?)\\s*([%a-zA-Z/]+)?", "%", "20 - 40", 20.0, 40.0),
            new BiomarkerDefinition("Eosinophils", "(?i)\\b(?:eosinophils?|eos)\\b[:\\s]*([<>]?\\s*\\d+(?:\\.\\d+)?)\\s*([%a-zA-Z/]+)?", "%", "1 - 6", 1.0, 6.0),
            new BiomarkerDefinition("Monocytes", "(?i)\\b(?:monocytes?|monos?)\\b[:\\s]*([<>]?\\s*\\d+(?:\\.\\d+)?)\\s*([%a-zA-Z/]+)?", "%", "2 - 10", 2.0, 10.0),
            new BiomarkerDefinition("Basophils", "(?i)\\b(?:basophils?|basos?)\\b[:\\s]*([<>]?\\s*\\d+(?:\\.\\d+)?)\\s*([%a-zA-Z/]+)?", "%", "0 - 2", 0.0, 2.0),
            new BiomarkerDefinition("Platelet Count", "(?i)\\b(?:platelets?(?:\\s*count)?|plt)\\b[:\\s]*([<>]?\\s*\\d{1,3}(?:,\\d{3})*(?:\\.\\d+)?|\\d+(?:\\.\\d+)?)\\s*([a-zA-Z0-9/%]+)?", "/cumm", "150,000 - 410,000", 150000.0, 410000.0),

            // --- Renal & Electrolytes (KFT/BMP) ---
            new BiomarkerDefinition("Potassium", "(?i)\\b(?:potassium|k\\+)\\b[:\\s]*([<>]?\\s*\\d+(?:\\.\\d+)?)\\s*([a-zA-Z/]+)?", "mEq/L", "3.5 - 5.0", 3.5, 5.0),
            new BiomarkerDefinition("Serum Creatinine", "(?i)\\b(?:serum\\s+)?creatinine\\b[:\\s]*([<>]?\\s*\\d+(?:\\.\\d+)?)\\s*([a-zA-Z/]+)?", "mg/dL", "0.6 - 1.2", 0.6, 1.2),
            new BiomarkerDefinition("eGFR", "(?i)\\begfr\\b[:\\s]*([<>]?\\s*\\d+(?:\\.\\d+)?)\\s*([a-zA-Z0-9/.²^2]+)?", "mL/min/1.73m2", "> 60", 60.0, 1000.0),
            new BiomarkerDefinition("Sodium", "(?i)\\b(?:sodium|na\\+)\\b[:\\s]*([<>]?\\s*\\d+(?:\\.\\d+)?)\\s*([a-zA-Z/]+)?", "mEq/L", "136 - 145", 136.0, 145.0),
            new BiomarkerDefinition("Blood Urea Nitrogen (BUN)", "(?i)\\b(?:blood\\s+urea\\s+nitrogen|bun|serum\\s+urea)\\b[:\\s]*([<>]?\\s*\\d+(?:\\.\\d+)?)\\s*([a-zA-Z/]+)?", "mg/dL", "7 - 20", 7.0, 20.0),
            new BiomarkerDefinition("Calcium", "(?i)\\bcalcium\\b[:\\s]*([<>]?\\s*\\d+(?:\\.\\d+)?)\\s*([a-zA-Z/]+)?", "mg/dL", "8.5 - 10.2", 8.5, 10.2),

            // --- Glycemic ---
            new BiomarkerDefinition("Fasting Blood Glucose", "(?i)\\b(?:fasting\\s+)?(?:blood\\s+)?glucose\\b[:\\s]*([<>]?\\s*\\d+(?:\\.\\d+)?)\\s*([a-zA-Z/]+)?", "mg/dL", "70 - 99", 70.0, 99.0),
            new BiomarkerDefinition("Hemoglobin A1c", "(?i)\\b(?:hba1c|hemoglobin\\s+a1c|glycated\\s+hemoglobin)\\b[:\\s]*([<>]?\\s*\\d+(?:\\.\\d+)?)\\s*([%a-zA-Z/]+)?", "%", "< 5.7", 0.0, 5.6),

            // --- Lipid Profile ---
            new BiomarkerDefinition("Total Cholesterol", "(?i)\\b(?:total\\s+)?cholesterol\\b[:\\s]*([<>]?\\s*\\d+(?:\\.\\d+)?)\\s*([a-zA-Z/]+)?", "mg/dL", "< 200", 0.0, 200.0),
            new BiomarkerDefinition("HDL Cholesterol", "(?i)\\b(?:hdl|hdl\\s+cholesterol)\\b[:\\s]*([<>]?\\s*\\d+(?:\\.\\d+)?)\\s*([a-zA-Z/]+)?", "mg/dL", "> 40", 40.0, 1000.0),
            new BiomarkerDefinition("LDL Cholesterol", "(?i)\\b(?:ldl|ldl\\s+cholesterol)\\b[:\\s]*([<>]?\\s*\\d+(?:\\.\\d+)?)\\s*([a-zA-Z/]+)?", "mg/dL", "< 100", 0.0, 100.0),
            new BiomarkerDefinition("Triglycerides", "(?i)\\btriglycerides?\\b[:\\s]*([<>]?\\s*\\d+(?:\\.\\d+)?)\\s*([a-zA-Z/]+)?", "mg/dL", "< 150", 0.0, 150.0),

            // --- Liver Function Test (LFT) ---
            new BiomarkerDefinition("Total Bilirubin", "(?i)\\b(?:total\\s+)?bilirubin\\b[:\\s]*([<>]?\\s*\\d+(?:\\.\\d+)?)\\s*([a-zA-Z/]+)?", "mg/dL", "0.2 - 1.2", 0.2, 1.2),
            new BiomarkerDefinition("SGPT (ALT)", "(?i)\\b(?:sgpt|alt|alanine\\s+aminotransferase)\\b[:\\s]*([<>]?\\s*\\d+(?:\\.\\d+)?)\\s*([a-zA-Z/]+)?", "U/L", "7 - 56", 7.0, 56.0),
            new BiomarkerDefinition("SGOT (AST)", "(?i)\\b(?:sgot|ast|aspartate\\s+aminotransferase)\\b[:\\s]*([<>]?\\s*\\d+(?:\\.\\d+)?)\\s*([a-zA-Z/]+)?", "U/L", "10 - 40", 10.0, 40.0),
            new BiomarkerDefinition("Alkaline Phosphatase (ALP)", "(?i)\\b(?:alkaline\\s+phosphatase|alp)\\b[:\\s]*([<>]?\\s*\\d+(?:\\.\\d+)?)\\s*([a-zA-Z/]+)?", "U/L", "44 - 147", 44.0, 147.0),

            // --- Thyroid & Vitamins ---
            new BiomarkerDefinition("TSH", "(?i)\\b(?:tsh|thyroid\\s+stimulating\\s+hormone)\\b[:\\s]*([<>]?\\s*\\d+(?:\\.\\d+)?)\\s*([a-zA-Z/]+)?", "uIU/mL", "0.4 - 4.0", 0.4, 4.0),
            new BiomarkerDefinition("Vitamin D (25-OH)", "(?i)\\b(?:vitamin\\s+d|25-oh\\s+vitamin\\s+d)\\b[:\\s]*([<>]?\\s*\\d+(?:\\.\\d+)?)\\s*([a-zA-Z/]+)?", "ng/mL", "30 - 100", 30.0, 100.0),
            new BiomarkerDefinition("Vitamin B12", "(?i)\\b(?:vitamin\\s+b12|b12)\\b[:\\s]*([<>]?\\s*\\d+(?:\\.\\d+)?)\\s*([a-zA-Z/]+)?", "pg/mL", "200 - 900", 200.0, 900.0)
    );

    // Generic tabular line regex supporting space-delimited or colon/tab-delimited rows with optional reference range:
    // e.g., "Hemoglobin (Hb) 14.2 g/dL 13.0 - 17.0" or "Potassium: 4.8 mEq/L (Reference: 3.5 - 5.0)"
    private static final Pattern GENERIC_TABLE_LINE = Pattern.compile(
            "^[\\s*•-]*([A-Za-z0-9/(). -]{2,40}?)\\s*[:=\\-|\t]?\\s+([<>]?\\s*\\d{1,3}(?:,\\d{3})*(?:\\.\\d+)?|\\d+(?:\\.\\d+)?)\\s*([a-zA-Z/%]+(?:/[a-zA-Z0-9.]+)?)(?:\\s+[(]?(?:Reference:?\\s*)?([<>]?\\s*[\\d,.]+\\s*(?:[-–—toTO\\s\ufffd]+\\s*[\\d,.]+)?)[)]?)?",
            Pattern.MULTILINE
    );

    /**
     * Extracts structured lab results with clinical status evaluation from raw text.
     */
    public static List<LabResult> extractLabValues(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            return Collections.emptyList();
        }

        List<LabResult> results = new ArrayList<>();
        Set<String> matchedKeys = new HashSet<>();

        // 1. Detect test collection/report date if present
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

                String refRange = def.defaultRefRange();
                String flag = evaluateBiomarkerFlag(val, unit, def, rawText);

                results.add(new LabResult(def.name(), val, unit, defaultDate, refRange, flag));
                matchedKeys.add(normalizeKey(def.name()));
            }
        }

        // 3. Fallback to generic line extraction for any laboratory tests not in the standard list
        Matcher lineMatcher = GENERIC_TABLE_LINE.matcher(rawText);
        while (lineMatcher.find()) {
            String test = lineMatcher.group(1).trim();
            String val = lineMatcher.group(2).replaceAll("\\s+", "");
            String unit = lineMatcher.group(3).trim();
            String rawRef = lineMatcher.groupCount() >= 4 && lineMatcher.group(4) != null ? lineMatcher.group(4).trim() : "";

            // Filter out non-lab noise lines
            String testLower = test.toLowerCase();
            if (isNoiseLine(testLower)) {
                continue;
            }

            String normKey = normalizeKey(test);
            boolean alreadyCovered = matchedKeys.contains(normKey);
            if (!alreadyCovered) {
                for (String key : matchedKeys) {
                    if (normKey.contains(key) || key.contains(normKey)) {
                        alreadyCovered = true;
                        break;
                    }
                }
            }

            if (!alreadyCovered && test.length() >= 2) {
                String cleanRef = cleanRefRange(rawRef);
                String flag = evaluateGenericFlag(val, cleanRef);
                results.add(new LabResult(test, val, unit, defaultDate, cleanRef.isEmpty() ? "Standard" : cleanRef, flag));
                matchedKeys.add(normKey);
            }
        }

        return results;
    }

    private static String normalizeKey(String name) {
        return name.toLowerCase()
                .replaceAll("\\s*\\([^)]*\\)", "")
                .replaceAll("[^a-z0-9]", "")
                .trim();
    }

    private static boolean isNoiseLine(String lineLower) {
        return lineLower.contains("patient") || lineLower.contains("doctor") || lineLower.contains("hospital") ||
                lineLower.contains("investigation") || lineLower.contains("specimen") || lineLower.contains("date") ||
                lineLower.contains("page") || lineLower.contains("phone") || lineLower.contains("age") ||
                lineLower.contains("sex") || lineLower.contains("gender") || lineLower.contains("id") ||
                lineLower.contains("sample") || lineLower.contains("collection") || lineLower.contains("status") ||
                lineLower.contains("result") || lineLower.contains("reference") || lineLower.contains("unit") ||
                lineLower.contains("interpretation") || lineLower.contains("important") || lineLower.contains("note");
    }

    private static String cleanRefRange(String raw) {
        if (raw == null || raw.isBlank()) return "";
        return raw.replaceAll("[\ufffd\u2013\u2014]", "-")
                .replaceAll("(?i)\\bto\\b", "-")
                .replaceAll("\\s*-\\s*", " - ")
                .trim();
    }

    private static String evaluateBiomarkerFlag(String valStr, String unit, BiomarkerDefinition def, String fullText) {
        double val = parseNumeric(valStr);
        if (val == Double.MIN_VALUE) return "NORMAL";

        double min = def.minNormal();
        double max = def.maxNormal();

        if (def.name().contains("WBC") && val < 50.0) {
            min = 4.0;
            max = 11.0;
        } else if (def.name().contains("Platelet") && val < 1000.0) {
            min = 150.0;
            max = 410.0;
        }

        if (min > 0 && val < min) return "LOW";
        if (max > 0 && val > max) return "HIGH";
        return "NORMAL";
    }

    private static String evaluateGenericFlag(String valStr, String refRange) {
        double val = parseNumeric(valStr);
        if (val == Double.MIN_VALUE || refRange == null || refRange.isBlank()) {
            return "NORMAL";
        }

        try {
            if (refRange.contains("-")) {
                String[] parts = refRange.split("-");
                if (parts.length == 2) {
                    double min = parseNumeric(parts[0]);
                    double max = parseNumeric(parts[1]);
                    if (min != Double.MIN_VALUE && val < min) return "LOW";
                    if (max != Double.MIN_VALUE && val > max) return "HIGH";
                    return "NORMAL";
                }
            } else if (refRange.startsWith("<")) {
                double max = parseNumeric(refRange.substring(1));
                if (max != Double.MIN_VALUE && val > max) return "HIGH";
                return "NORMAL";
            } else if (refRange.startsWith(">")) {
                double min = parseNumeric(refRange.substring(1));
                if (min != Double.MIN_VALUE && val < min) return "LOW";
                return "NORMAL";
            }
        } catch (Exception ignored) {
        }
        return "NORMAL";
    }

    private static double parseNumeric(String s) {
        if (s == null) return Double.MIN_VALUE;
        try {
            String clean = s.replaceAll("[^0-9.]", "").trim();
            if (clean.isEmpty()) return Double.MIN_VALUE;
            return Double.parseDouble(clean);
        } catch (Exception e) {
            return Double.MIN_VALUE;
        }
    }

    private record BiomarkerDefinition(
            String name,
            String regex,
            String defaultUnit,
            String defaultRefRange,
            double minNormal,
            double maxNormal
    ) {}
}
