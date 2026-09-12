package com.mediscan.mediscan.agent.tool;

import com.mediscan.mediscan.agent.model.ToolCallLog;

import java.util.List;

/**
 * Tool interface for retrieving grounded clinical guideline snippets from local knowledge base.
 */
public interface ClinicalGuidelineRAGService {

    List<GuidelineMatch> searchGuidelines(String query, int maxResults, boolean simulateFailure, ToolCallLog log);

    default List<GuidelineMatch> search(String query) {
        return searchGuidelines(query, 3, false, null);
    }

    List<GuidelineSnippet> getAllGuidelines();

    class GuidelineSnippet {
        private String id;
        private String title;
        private String organization;
        private String publicationYear;
        private String clinicalDomain;
        private String summary;
        private String citedText;
        private List<String> keywords;

        public GuidelineSnippet() {
        }

        public GuidelineSnippet(String id, String title, String organization, String publicationYear,
                                String clinicalDomain, String summary, String citedText, List<String> keywords) {
            this.id = id;
            this.title = title;
            this.organization = organization;
            this.publicationYear = publicationYear;
            this.clinicalDomain = clinicalDomain;
            this.summary = summary;
            this.citedText = citedText;
            this.keywords = keywords;
        }

        public String getId() {
            return id;
        }

        public String getTitle() {
            return title;
        }

        public String getOrganization() {
            return organization;
        }

        public String getPublicationYear() {
            return publicationYear;
        }

        public String getClinicalDomain() {
            return clinicalDomain;
        }

        public String getSummary() {
            return summary;
        }

        public String getCitedText() {
            return citedText;
        }

        public List<String> getKeywords() {
            return keywords;
        }
    }

    class GuidelineMatch {
        private GuidelineSnippet snippet;
        private double score;
        private String matchedRationale;

        public GuidelineMatch() {
        }

        public GuidelineMatch(GuidelineSnippet snippet, double score, String matchedRationale) {
            this.snippet = snippet;
            this.score = score;
            this.matchedRationale = matchedRationale;
        }

        public GuidelineSnippet getSnippet() {
            return snippet;
        }

        public double getScore() {
            return score;
        }

        public String getMatchedRationale() {
            return matchedRationale;
        }
    }
}
