package com.mediscan.mediscan.agent.tool;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mediscan.mediscan.agent.model.ToolCallLog;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of ClinicalGuidelineRAGService.
 * Dynamically loads clinical guideline knowledge base from JSON resource and supports grounded keyword-overlap retrieval.
 */
@Service
public class ClinicalGuidelineRAGServiceImpl implements ClinicalGuidelineRAGService {

    private final List<GuidelineSnippet> corpus = new ArrayList<>();

    public ClinicalGuidelineRAGServiceImpl() {
        initCorpus();
    }

    private void initCorpus() {
        try (InputStream is = getClass().getResourceAsStream("/clinical-guidelines.json")) {
            if (is != null) {
                Gson gson = new Gson();
                Type listType = new TypeToken<List<GuidelineSnippet>>() {}.getType();
                List<GuidelineSnippet> loaded = gson.fromJson(new InputStreamReader(is, StandardCharsets.UTF_8), listType);
                if (loaded != null && !loaded.isEmpty()) {
                    corpus.addAll(loaded);
                    System.out.println("[ClinicalGuidelineRAGService] Successfully loaded " + corpus.size() + " guidelines from clinical-guidelines.json");
                    return;
                }
            }
        } catch (Exception e) {
            System.err.println("[ClinicalGuidelineRAGService] Failed to load from resource file: " + e.getMessage());
        }

        // Fallback in case resource loading encountered an issue
        corpus.add(new GuidelineSnippet(
                "GL-ADA-01",
                "Metformin Dosing & Renal Safety Thresholds (eGFR Cutoffs)",
                "American Diabetes Association (ADA)",
                "2024",
                "ENDOCRINOLOGY_NEPHROLOGY",
                "Guidelines on when Metformin dose must be reduced or discontinued based on eGFR.",
                "ADA Standards of Care: Metformin should be initiated if eGFR >= 45 mL/min/1.73m2. If eGFR declines to 30-44 mL/min/1.73m2, reduce maximum dose to 1000 mg/day with renal monitoring every 3 months. Metformin is strictly CONTRAINDICATED in patients with eGFR < 30 mL/min/1.73m2 due to high risk of life-threatening lactic acidosis; therapy must be discontinued immediately and alternative glycemic regimens instituted.",
                List.of("metformin", "egfr", "kidney", "renal", "lactic acidosis", "diabetes", "contraindicated", "creatinine")
        ));
    }

    @Override
    public List<GuidelineMatch> searchGuidelines(String query, int maxResults, boolean simulateFailure, ToolCallLog log) {
        long startTime = System.currentTimeMillis();
        String timestamp = Instant.now().toString();

        if (log != null) {
            log.setId("TOOL-" + UUID.randomUUID().toString().substring(0, 8));
            log.setToolName("ClinicalGuidelineRAGService");
            log.setCallReason("Retrieve evidence-based guideline recommendations from local knowledge base");
            log.setInputSummary("Query: '" + query + "', maxResults: " + maxResults);
            log.setTimestamp(timestamp);
        }

        if (simulateFailure) {
            long duration = System.currentTimeMillis() - startTime;
            if (log != null) {
                log.setStatus("FAILED");
                log.setDurationMs(duration);
                log.setOutputSummary("Simulated Failure: Guideline Knowledge Base index unavailable (503)");
                log.setErrorDetails("Semantic index retrieval timed out");
            }
            throw new RuntimeException("Simulated Failure: Guideline Knowledge Base index timeout");
        }

        if (query == null || query.isBlank()) {
            return Collections.emptyList();
        }

        List<String> queryTokens = Arrays.stream(query.toLowerCase().split("[^a-zA-Z0-9]+"))
                .filter(s -> s.length() > 2)
                .collect(Collectors.toList());

        List<GuidelineMatch> matches = new ArrayList<>();

        for (GuidelineSnippet s : corpus) {
            double score = 0.0;
            List<String> matchedWords = new ArrayList<>();

            for (String token : queryTokens) {
                boolean matched = false;
                if (s.getKeywords() != null) {
                    for (String kw : s.getKeywords()) {
                        if (kw.equalsIgnoreCase(token) || kw.toLowerCase().contains(token) || token.contains(kw.toLowerCase())) {
                            score += 3.0;
                            matchedWords.add(kw);
                            matched = true;
                            break;
                        }
                    }
                }
                if (!matched && s.getTitle() != null && s.getTitle().toLowerCase().contains(token)) {
                    score += 2.0;
                    matchedWords.add(token);
                } else if (!matched && s.getCitedText() != null && s.getCitedText().toLowerCase().contains(token)) {
                    score += 1.0;
                    matchedWords.add(token);
                }
            }

            if (score > 0) {
                String rationale = "Matched keywords/terms: " + String.join(", ", matchedWords.stream().distinct().limit(4).toList());
                matches.add(new GuidelineMatch(s, score, rationale));
            }
        }

        matches.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));
        List<GuidelineMatch> result = matches.stream().limit(maxResults).collect(Collectors.toList());

        long duration = System.currentTimeMillis() - startTime;
        if (log != null) {
            log.setStatus("SUCCESS");
            log.setDurationMs(duration);
            if (!result.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                sb.append("Found ").append(result.size()).append(" guideline matches: ");
                for (GuidelineMatch m : result) {
                    sb.append("[").append(m.getSnippet().getId()).append(" - ").append(m.getSnippet().getTitle())
                            .append(" (").append(m.getSnippet().getOrganization()).append(", ").append(m.getSnippet().getPublicationYear()).append(")]; ");
                }
                log.setOutputSummary(sb.toString());
            } else {
                log.setOutputSummary("No specific guideline matched for query: " + query);
            }
        }

        return result;
    }

    @Override
    public List<GuidelineSnippet> getAllGuidelines() {
        return Collections.unmodifiableList(corpus);
    }
}
