package com.mediscan.mediscan.service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mediscan.mediscan.agent.model.LabResult;
import com.mediscan.mediscan.agent.service.LabReportParser;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class GroqService {

    @Value("${groq.api.key:demo_key}")
    private String apiKey;

    @Value("${openai.api.key:}")
    private String openAiKey;

    private final OkHttpClient client = new OkHttpClient();

    public String analyze(String reportText) {
        return analyze(reportText, "Patient", "", "");
    }

    public String cleanAndExtractJson(String raw) {
        if (raw == null) return "{}";
        String content = raw.trim();
        content = content.replace("```json", "");
        content = content.replace("```JSON", "");
        content = content.replace("```", "").trim();

        int firstBrace = content.indexOf('{');
        int lastBrace = content.lastIndexOf('}');
        if (firstBrace != -1 && lastBrace > firstBrace) {
            content = content.substring(firstBrace, lastBrace + 1).trim();
        }
        return content;
    }

    public String analyze(String reportText, String name, String age, String gender) {
        String effectiveKey = (openAiKey != null && !openAiKey.isBlank()) ? openAiKey.trim() : (apiKey != null ? apiKey.trim() : "");
        // 1. If API key is not configured or is a placeholder, engage intelligent clinical fallback analyzer
        if (effectiveKey.isBlank() || effectiveKey.equalsIgnoreCase("demo_key") || effectiveKey.contains("${")) {
            System.out.println("[AIService] API key is not configured. Engaging intelligent clinical fallback engine.");
            return fallbackAnalyze(reportText, name, age, gender);
        }

        try {
            String prompt = """
You are an expert medical AI physician.

Analyze the medical report carefully for patient %s (Age: %s, Gender: %s).

IMPORTANT RULES:
1. Return ONLY valid JSON.
2. Do NOT use markdown fences or explanations outside JSON.
3. Every field must exist.
4. Summary should be 3-6 sentences in clear language.

Return exactly this JSON structure:
{
  "summary":"",
  "riskLevel":"Low/Moderate/High/Critical",
  "problems":[],
  "recommendations":[],
  "specialist":"Specialist recommendation",
  "emergencyStatus":"Normal or Critical",
  "emergency":"Yes or No",
  "reportType":"Diagnostic Laboratory Report",
  "hospital":"MediScan AI Diagnostic Services"
}

Medical Report:
%s
""".formatted(
                    name != null && !name.isBlank() ? name : "Patient",
                    age != null && !age.isBlank() ? age : "Unknown",
                    gender != null && !gender.isBlank() ? gender : "Unknown",
                    reportText != null && !reportText.isBlank() ? reportText : "Diagnostic clinical report."
            );

            JsonObject message = new JsonObject();
            message.addProperty("role", "user");
            message.addProperty("content", prompt);

            JsonArray messages = new JsonArray();
            messages.add(message);

            boolean isOpenAI = effectiveKey.startsWith("sk-");
            String endpoint = isOpenAI ? "https://api.openai.com/v1/chat/completions" : "https://api.groq.com/openai/v1/chat/completions";
            String model = isOpenAI ? "gpt-3.5-turbo" : "llama-3.3-70b-versatile";

            JsonObject body = new JsonObject();
            body.addProperty("model", model);
            body.add("messages", messages);
            body.addProperty("temperature", 0);

            RequestBody requestBody = RequestBody.create(
                    body.toString(),
                    MediaType.parse("application/json")
            );

            Request request = new Request.Builder()
                    .url(endpoint)
                    .addHeader("Authorization", "Bearer " + effectiveKey)
                    .addHeader("Content-Type", "application/json")
                    .post(requestBody)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    String errBody = response.body() != null ? response.body().string() : "";
                    System.err.println("[AIService] " + (isOpenAI ? "OpenAI" : "Groq") + " API returned HTTP " + response.code() + ": " + errBody + ". Engaging intelligent clinical fallback engine.");
                    return fallbackAnalyze(reportText, name, age, gender);
                }

                String result = response.body().string();
                JsonObject json = new Gson().fromJson(result, JsonObject.class);

                String content = json
                        .getAsJsonArray("choices")
                        .get(0)
                        .getAsJsonObject()
                        .getAsJsonObject("message")
                        .get("content")
                        .getAsString();

                content = cleanAndExtractJson(content);

                // Validate JSON before returning
                JsonObject parsed = new Gson().fromJson(content, JsonObject.class);
                if (!parsed.has("tests") || parsed.getAsJsonArray("tests").isEmpty()) {
                    List<LabResult> labs = LabReportParser.extractLabValues(reportText);
                    parsed.add("tests", buildTestsJsonArray(labs));
                }
                return new Gson().toJson(parsed);
            }
        } catch (Exception e) {
            System.err.println("[GroqService] Groq call failed (" + e.getMessage() + "). Engaging intelligent clinical fallback engine.");
            return fallbackAnalyze(reportText, name, age, gender);
        }
    }

    /**
     * Intelligent deterministic clinical report analyzer fallback.
     * Evaluates diagnostic biomarkers, normal ranges, and clinical concerns to generate
     * a comprehensive patient-facing summary without crashing on external API 401/timeout failures.
     */
    public String fallbackAnalyze(String fullText) {
        return fallbackAnalyze(fullText, null, null, null);
    }

    public String fallbackAnalyze(String fullText, String patientName, String age, String gender) {
        if (fullText == null) fullText = "";

        // 1. Extract patient demographics if not explicitly provided
        if (patientName == null || patientName.isBlank() || "Patient".equalsIgnoreCase(patientName)) {
            patientName = "Patient";
            Matcher nameMatcher = Pattern.compile("(?i)\\bName:\\s*([^\\r\\n]+)").matcher(fullText);
            if (nameMatcher.find()) patientName = nameMatcher.group(1).trim();
        }

        if (age == null || age.isBlank()) {
            Matcher ageMatcher = Pattern.compile("(?i)\\bAge:\\s*([^\\r\\n]+)").matcher(fullText);
            if (ageMatcher.find()) age = ageMatcher.group(1).trim();
        }

        if (gender == null || gender.isBlank()) {
            Matcher genderMatcher = Pattern.compile("(?i)\\bGender:\\s*([^\\r\\n]+)").matcher(fullText);
            if (genderMatcher.find()) gender = genderMatcher.group(1).trim();
        }

        // 2. Extract lab values using LabReportParser
        List<LabResult> labs = LabReportParser.extractLabValues(fullText);

        List<String> problems = new ArrayList<>();
        List<String> normalFindings = new ArrayList<>();
        List<String> recommendations = new ArrayList<>();
        String specialist = "General Physician";
        String riskLevel = "Low";
        boolean hasCritical = false;
        boolean hasHigh = false;
        boolean hasModerate = false;

        // 3. Clinical rules evaluation on extracted biomarkers
        for (LabResult lab : labs) {
            String name = lab.getTestName();
            double val = parseNumeric(lab.getValue());
            if (val == Double.MIN_VALUE) continue;

            String nameLower = name.toLowerCase();
            String flag = lab.getFlag() != null ? lab.getFlag() : "NORMAL";

            if (nameLower.contains("potassium") || nameLower.contains("k+")) {
                if (val > 5.5) {
                    hasCritical = true;
                    problems.add(String.format("Hyperkalemia (Serum Potassium %s %s, Ref: 3.5-5.0): Elevated potassium poses cardiac dysrhythmia risk.", lab.getValue(), lab.getUnit()));
                    recommendations.add("Urgent repeat serum electrolytes and ECG evaluation to rule out cardiac conduction abnormalities.");
                    specialist = "Cardiologist";
                } else if (val > 5.0) {
                    hasHigh = true;
                    problems.add(String.format("Mild Hyperkalemia (Serum Potassium %s %s, Ref: 3.5-5.0): Mildly elevated potassium level.", lab.getValue(), lab.getUnit()));
                    recommendations.add("Review potassium-sparing medications and dietary intake with your physician.");
                    specialist = "Nephrologist";
                } else if (val < 3.5) {
                    hasModerate = true;
                    problems.add(String.format("Hypokalemia (Serum Potassium %s %s, Ref: 3.5-5.0): Low potassium levels detected.", lab.getValue(), lab.getUnit()));
                    recommendations.add("Evaluate dietary electrolyte intake and review any diuretic therapies.");
                } else {
                    normalFindings.add(String.format("Potassium (%s %s) is within normal reference limits.", lab.getValue(), lab.getUnit()));
                }
            } else if (nameLower.contains("creatinine")) {
                if (val > 2.0) {
                    hasCritical = true;
                    problems.add(String.format("Markedly Elevated Serum Creatinine (%s %s, Ref: 0.6-1.2): Suggestive of acute kidney injury or chronic renal impairment.", lab.getValue(), lab.getUnit()));
                    recommendations.add("Prompt nephrology consultation and review of all nephrotoxic medications (e.g. NSAIDs).");
                    specialist = "Nephrologist";
                } else if (val > 1.3) {
                    hasHigh = true;
                    problems.add(String.format("Elevated Serum Creatinine (%s %s, Ref: 0.6-1.2): Indicates reduced glomerular clearance.", lab.getValue(), lab.getUnit()));
                    recommendations.add("Maintain optimal hydration and schedule comprehensive kidney function reassessment.");
                    specialist = "Nephrologist";
                } else {
                    normalFindings.add(String.format("Serum Creatinine (%s %s) reflects healthy baseline renal clearance.", lab.getValue(), lab.getUnit()));
                }
            } else if (nameLower.contains("egfr")) {
                if (val < 30) {
                    hasCritical = true;
                    problems.add(String.format("Severely Decreased eGFR (%s %s, Ref: >60): Reflects advanced renal filtration compromise.", lab.getValue(), lab.getUnit()));
                    specialist = "Nephrologist";
                } else if (val < 60) {
                    hasHigh = true;
                    problems.add(String.format("Reduced eGFR (%s %s, Ref: >60): Indicative of moderate renal filtration reduction.", lab.getValue(), lab.getUnit()));
                    specialist = "Nephrologist";
                } else {
                    normalFindings.add(String.format("eGFR (%s %s) indicates adequate kidney filtration.", lab.getValue(), lab.getUnit()));
                }
            } else if (nameLower.contains("glucose") || nameLower.contains("blood sugar")) {
                if (val >= 200) {
                    hasCritical = true;
                    problems.add(String.format("Marked Hyperglycemia (Fasting Blood Glucose %s %s, Ref: 70-99): Significantly elevated blood glucose.", lab.getValue(), lab.getUnit()));
                    recommendations.add("Consult an Endocrinologist for diabetes management and glycemic optimization.");
                    specialist = "Endocrinologist";
                } else if (val >= 126) {
                    hasHigh = true;
                    problems.add(String.format("Elevated Blood Glucose (%s %s, Ref: 70-99): Diagnostic indicator of hyperglycemia / Diabetes Mellitus.", lab.getValue(), lab.getUnit()));
                    recommendations.add("Endocrine follow-up for diabetes care, HbA1c tracking, and lifestyle management.");
                    specialist = "Endocrinologist";
                } else if (val >= 100) {
                    hasModerate = true;
                    problems.add(String.format("Impaired Fasting Glucose (%s %s, Ref: 70-99): Pre-diabetic glycemic range.", lab.getValue(), lab.getUnit()));
                    recommendations.add("Lifestyle adjustments, dietary carbohydrate moderation, and repeat fasting glucose.");
                    specialist = "Endocrinologist";
                } else if (val < 70) {
                    hasHigh = true;
                    problems.add(String.format("Hypoglycemia (Blood Glucose %s %s, Ref: 70-99): Low blood glucose level.", lab.getValue(), lab.getUnit()));
                    recommendations.add("Monitor for hypoglycemic symptoms (shakiness, dizziness) and consult your doctor.");
                } else {
                    normalFindings.add(String.format("Blood Glucose (%s %s) is in the normal fasting range.", lab.getValue(), lab.getUnit()));
                }
            } else if (nameLower.contains("hba1c") || nameLower.contains("hemoglobin a1c") || nameLower.contains("glycated")) {
                if (val >= 9.0) {
                    hasCritical = true;
                    problems.add(String.format("Very High HbA1c (%s %%, Ref: <5.7): Severe sustained hyperglycemia over past 3 months.", lab.getValue()));
                    recommendations.add("Immediate endocrinology consultation for targeted antihyperglycemic pharmacotherapy.");
                    specialist = "Endocrinologist";
                } else if (val >= 6.5) {
                    hasHigh = true;
                    problems.add(String.format("Elevated HbA1c (%s %%, Ref: <5.7): Consistent with diagnosed Type 2 Diabetes Mellitus.", lab.getValue()));
                    recommendations.add("Establish a diabetes self-management education plan and routine home glucose monitoring.");
                    specialist = "Endocrinologist";
                } else if (val >= 5.7) {
                    hasModerate = true;
                    problems.add(String.format("Prediabetic HbA1c (%s %%, Ref: <5.7): Indicates increased risk of progression to diabetes.", lab.getValue()));
                    recommendations.add("Implement structured nutritional modifications and physical activity.");
                } else {
                    normalFindings.add(String.format("Hemoglobin A1c (%s %%) is optimal.", lab.getValue()));
                }
            } else if (nameLower.contains("hemoglobin") || nameLower.contains("hgb")) {
                if (val < 9.0) {
                    hasHigh = true;
                    problems.add(String.format("Moderate Anemia (Hemoglobin %s %s, Ref: 13.0-17.0): Substantially reduced oxygen-carrying capacity.", lab.getValue(), lab.getUnit()));
                    recommendations.add("Investigate iron studies, ferritin, B12, and potential occult blood loss.");
                    specialist = "Hematologist";
                } else if (val < 13.0) {
                    hasModerate = true;
                    problems.add(String.format("Mild Anemia (Hemoglobin %s %s, Ref: 13.0-17.0): Slightly below normal limits.", lab.getValue(), lab.getUnit()));
                    recommendations.add("Consider iron-rich nutrition and clinical evaluation of fatigue symptoms.");
                    specialist = "Hematologist";
                } else if (val > 18.0) {
                    hasModerate = true;
                    problems.add(String.format("Elevated Hemoglobin (%s %s, Ref: 13.0-17.0): Suggestive of hemoconcentration or polycythemia.", lab.getValue(), lab.getUnit()));
                    specialist = "Hematologist";
                } else {
                    normalFindings.add(String.format("Hemoglobin (%s %s) is within healthy limits.", lab.getValue(), lab.getUnit()));
                }
            } else if (nameLower.contains("wbc") || nameLower.contains("white blood cell") || nameLower.contains("leukocyte")) {
                double wbcNorm = val > 50.0 ? val / 1000.0 : val;
                if (wbcNorm > 14.0) {
                    hasHigh = true;
                    problems.add(String.format("Leukocytosis (WBC %s %s, Ref: 4,000-11,000): Elevated white blood cells suggestive of active infection or systemic inflammation.", lab.getValue(), lab.getUnit()));
                    recommendations.add("Clinical assessment for potential bacterial or viral infectious etiology.");
                    specialist = "General Physician";
                } else if (wbcNorm > 11.0) {
                    hasModerate = true;
                    problems.add(String.format("Mild Leukocytosis (WBC %s %s, Ref: 4,000-11,000): Mildly elevated immune cell count.", lab.getValue(), lab.getUnit()));
                    specialist = "General Physician";
                } else if (wbcNorm < 3.5) {
                    hasModerate = true;
                    problems.add(String.format("Leukopenia (WBC %s %s, Ref: 4,000-11,000): Below normal immune cell count.", lab.getValue(), lab.getUnit()));
                    specialist = "Hematologist";
                } else {
                    normalFindings.add(String.format("White Blood Cell count (%s %s) is normal.", lab.getValue(), lab.getUnit()));
                }
            } else if (nameLower.contains("platelet") || nameLower.contains("plt")) {
                double pltNorm = val > 1000.0 ? val / 1000.0 : val;
                if (pltNorm < 100.0) {
                    hasHigh = true;
                    problems.add(String.format("Thrombocytopenia (Platelets %s %s, Ref: 150,000-410,000): Low platelet count increasing bleeding risk.", lab.getValue(), lab.getUnit()));
                    recommendations.add("Avoid NSAIDs and contact hematologist for evaluation.");
                    specialist = "Hematologist";
                } else if (pltNorm < 150.0) {
                    hasModerate = true;
                    problems.add(String.format("Mild Thrombocytopenia (Platelets %s %s, Ref: 150,000-410,000): Platelets slightly below reference.", lab.getValue(), lab.getUnit()));
                    specialist = "Hematologist";
                } else if (pltNorm > 450.0) {
                    hasModerate = true;
                    problems.add(String.format("Thrombocytosis (Platelets %s %s, Ref: 150,000-410,000): Elevated platelet count.", lab.getValue(), lab.getUnit()));
                    specialist = "Hematologist";
                } else {
                    normalFindings.add(String.format("Platelet Count (%s %s) is within normal range.", lab.getValue(), lab.getUnit()));
                }
            } else if (nameLower.contains("cholesterol")) {
                if (val >= 240) {
                    hasHigh = true;
                    problems.add(String.format("Hypercholesterolemia (Total Cholesterol %s %s, Ref: <200): High blood cholesterol increasing atherosclerotic risk.", lab.getValue(), lab.getUnit()));
                    recommendations.add("Cardiology or primary care consultation for lipid-lowering therapy and cardiovascular risk assessment.");
                    if ("General Physician".equals(specialist)) specialist = "Cardiologist";
                } else if (val >= 200) {
                    hasModerate = true;
                    problems.add(String.format("Borderline High Total Cholesterol (%s %s, Ref: <200): Mildly elevated cholesterol.", lab.getValue(), lab.getUnit()));
                    recommendations.add("Adopt heart-healthy Mediterranean diet and regular aerobic exercise.");
                    if ("General Physician".equals(specialist)) specialist = "Cardiologist";
                } else {
                    normalFindings.add(String.format("Total Cholesterol (%s %s) is within favorable range.", lab.getValue(), lab.getUnit()));
                }
            } else if (nameLower.contains("sodium")) {
                if (val > 146 || val < 134) {
                    hasModerate = true;
                    problems.add(String.format("Electrolyte Imbalance (Sodium %s %s, Ref: 136-145).", lab.getValue(), lab.getUnit()));
                } else {
                    normalFindings.add(String.format("Serum Sodium (%s %s) is balanced.", lab.getValue(), lab.getUnit()));
                }
            } else if (nameLower.contains("bun") || nameLower.contains("urea")) {
                if (val > 24) {
                    hasModerate = true;
                    problems.add(String.format("Elevated Blood Urea Nitrogen (%s %s, Ref: 7-20): May reflect dehydration or reduced renal clearance.", lab.getValue(), lab.getUnit()));
                } else {
                    normalFindings.add(String.format("BUN (%s %s) is normal.", lab.getValue(), lab.getUnit()));
                }
            } else {
                if ("HIGH".equalsIgnoreCase(flag)) {
                    hasModerate = true;
                    problems.add(String.format("Elevated %s (%s %s, Ref: %s).", lab.getTestName(), lab.getValue(), lab.getUnit(), lab.getReferenceRange()));
                } else if ("LOW".equalsIgnoreCase(flag)) {
                    hasModerate = true;
                    problems.add(String.format("Low %s (%s %s, Ref: %s).", lab.getTestName(), lab.getValue(), lab.getUnit(), lab.getReferenceRange()));
                } else {
                    normalFindings.add(String.format("%s (%s %s) is within healthy limits.", lab.getTestName(), lab.getValue(), lab.getUnit()));
                }
            }
        }

        // 4. Check for clinical keywords in raw report text if problems is empty
        String textLower = fullText.toLowerCase();
        if (problems.isEmpty()) {
            if (textLower.contains("fracture") || textLower.contains("broken bone") || textLower.contains("joint dislocation")) {
                hasHigh = true;
                problems.add("Orthopedic trauma or bone structural abnormality noted in clinical findings.");
                recommendations.add("Immediate orthopedic evaluation and immobilization as clinically warranted.");
                specialist = "Orthopedic Surgeon";
            } else if (textLower.contains("hypertension") || textLower.contains("high blood pressure") || textLower.contains("cardiac")) {
                hasModerate = true;
                problems.add("Cardiovascular / blood pressure abnormalities noted.");
                recommendations.add("Cardiology follow-up and daily blood pressure tracking.");
                specialist = "Cardiologist";
            } else if (textLower.contains("infection") || textLower.contains("pneumonia") || textLower.contains("bronchitis")) {
                hasModerate = true;
                problems.add("Respiratory or systemic inflammatory symptoms identified.");
                recommendations.add("Primary care consultation for targeted antibiotic or symptomatic therapy.");
                specialist = "Pulmonologist";
            }
        }

        // Determine overall risk level
        if (hasCritical) {
            riskLevel = "Critical";
        } else if (hasHigh) {
            riskLevel = "High";
        } else if (hasModerate) {
            riskLevel = "Moderate";
        } else {
            riskLevel = "Low";
        }

        // Default recommendations if none generated
        if (recommendations.isEmpty()) {
            if (problems.isEmpty()) {
                recommendations.add("All evaluated biomarkers are within normal healthy ranges. Continue routine preventive wellness checks.");
                recommendations.add("Maintain balanced hydration, nutritious diet, and regular physical activity.");
                recommendations.add("Retain a copy of this diagnostic report in your medical file for baseline reference.");
            } else {
                recommendations.add("Schedule a review with your physician to discuss identified test variations.");
                recommendations.add("Maintain balanced hydration and follow prescribed clinical guidance.");
                recommendations.add("Retain a copy of this diagnostic report for your personal medical records.");
            }
        }

        // Build comprehensive patient summary
        StringBuilder sb = new StringBuilder();
        if (problems.isEmpty()) {
            sb.append(String.format("Evaluation of the diagnostic medical report for %s indicates that all analyzed parameters are within normal clinical reference ranges. ", patientName));
            if (!labs.isEmpty()) {
                sb.append(String.format("A total of %d laboratory biomarkers (including %s, %s, and other key parameters) were evaluated and confirmed healthy. ",
                        labs.size(), labs.get(0).getTestName(), labs.size() > 1 ? labs.get(1).getTestName() : "cellular indices"));
            }
            sb.append("No acute pathological disorders, abnormal cell counts, or clinical risks were detected. Overall physiological indicators demonstrate healthy baseline stability. A routine wellness consultation with a General Physician is recommended for preventive care.");
        } else {
            sb.append(String.format("This comprehensive medical report evaluation for %s identifies specific parameters that warrant clinical attention. ", patientName));
            if ("Critical".equals(riskLevel)) {
                sb.append("Urgent clinical attention is warranted due to one or more significantly abnormal biomarkers. ");
            } else if ("High".equals(riskLevel)) {
                sb.append("Important diagnostic elevations were identified that require timely clinical follow-up. ");
            } else if ("Moderate".equals(riskLevel)) {
                sb.append("Mild-to-moderate clinical variations were noted that should be reviewed with your healthcare provider. ");
            }
            sb.append("Key findings include: ").append(String.join("; ", problems)).append(". ");
            if (!normalFindings.isEmpty()) {
                sb.append("Reassuring normal markers observed: ").append(String.join(", ", normalFindings.subList(0, Math.min(3, normalFindings.size())))).append(". ");
            }
            sb.append(String.format("It is recommended to consult a %s to review these diagnostic results in the context of your overall medical history.", specialist));
        }

        JsonObject out = new JsonObject();
        out.addProperty("summary", sb.toString().trim());
        out.addProperty("riskLevel", riskLevel);

        JsonArray probArr = new JsonArray();
        for (String p : problems) probArr.add(p);
        out.add("problems", probArr);

        JsonArray recArr = new JsonArray();
        for (String r : recommendations) recArr.add(r);
        out.add("recommendations", recArr);

        out.addProperty("specialist", specialist);
        out.addProperty("emergencyStatus", "Critical".equals(riskLevel) ? "Critical / Immediate Attention Required" : "Normal");
        out.addProperty("emergency", "Critical".equals(riskLevel) ? "Yes" : "No");
        out.addProperty("reportType", "Diagnostic Laboratory Report");
        out.addProperty("hospital", "MediScan AI Diagnostic Services");

        // Structured lab tests array
        out.add("tests", buildTestsJsonArray(labs));

        return new Gson().toJson(out);
    }

    private JsonArray buildTestsJsonArray(List<LabResult> labs) {
        JsonArray testsArr = new JsonArray();
        if (labs == null) return testsArr;

        for (LabResult lab : labs) {
            JsonObject tObj = new JsonObject();
            tObj.addProperty("testName", lab.getTestName());
            tObj.addProperty("value", lab.getValue());
            tObj.addProperty("unit", lab.getUnit() != null ? lab.getUnit() : "");
            tObj.addProperty("referenceRange", lab.getReferenceRange() != null ? lab.getReferenceRange() : "Standard");
            String flag = lab.getFlag() != null ? lab.getFlag() : "NORMAL";
            tObj.addProperty("flag", flag);
            String status = "Normal";
            if ("HIGH".equalsIgnoreCase(flag)) status = "High";
            else if ("LOW".equalsIgnoreCase(flag)) status = "Low";
            else if ("CRITICAL".equalsIgnoreCase(flag)) status = "Critical";
            tObj.addProperty("status", status);
            testsArr.add(tObj);
        }
        return testsArr;
    }

    private double parseNumeric(String val) {
        if (val == null) return Double.MIN_VALUE;
        try {
            String clean = val.replaceAll("[^0-9.]", "").trim();
            if (clean.isEmpty()) return Double.MIN_VALUE;
            return Double.parseDouble(clean);
        } catch (Exception e) {
            return Double.MIN_VALUE;
        }
    }

    public String call(String systemPrompt, String userPrompt) throws IOException {
        if (apiKey == null || apiKey.isBlank() || apiKey.equalsIgnoreCase("demo_key")) {
            throw new IllegalStateException("Groq API key not configured or invalid.");
        }

        JsonArray messages = new JsonArray();

        if (systemPrompt != null && !systemPrompt.isBlank()) {
            JsonObject sysMsg = new JsonObject();
            sysMsg.addProperty("role", "system");
            sysMsg.addProperty("content", systemPrompt);
            messages.add(sysMsg);
        }

        JsonObject userMsg = new JsonObject();
        userMsg.addProperty("role", "user");
        userMsg.addProperty("content", userPrompt);
        messages.add(userMsg);

        JsonObject body = new JsonObject();
        body.addProperty("model", "llama-3.3-70b-versatile");
        body.add("messages", messages);
        body.addProperty("temperature", 0.1);

        RequestBody requestBody = RequestBody.create(
                body.toString(),
                MediaType.parse("application/json")
        );

        Request request = new Request.Builder()
                .url("https://api.groq.com/openai/v1/chat/completions")
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new RuntimeException("Groq API Error: " + response.code() + " : " + (response.body() != null ? response.body().string() : ""));
            }

            String result = response.body().string();
            JsonObject json = new Gson().fromJson(result, JsonObject.class);

            String content = json
                    .getAsJsonArray("choices")
                    .get(0)
                    .getAsJsonObject()
                    .getAsJsonObject("message")
                    .get("content")
                    .getAsString();

            content = cleanAndExtractJson(content);
            return content;
        }
    }

    public String executePrompt(String prompt) throws IOException {
        return call("You are an expert clinical documentation assistant. Output strictly valid JSON without markdown.", prompt);
    }
}