package me.valkeea.fishyaddons.processor;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import me.valkeea.fishyaddons.config.FilterConfig.Rule;
import me.valkeea.fishyaddons.config.FishyConfig.AlertData;

/**
 * Contains the results of analyzing a chat message for both filter and alert patterns.
 */
public class MessageAnalysis {
    private final String originalMessage;
    private final String strippedMessage;
    private final List<FilterMatch> filterMatches;
    private final List<AlertMatch> alertMatches;
    private final long analysisTimeNs;
    private final boolean hasAnyMatches;
    
    public MessageAnalysis(String originalMessage, String strippedMessage, 
                          List<FilterMatch> filterMatches, List<AlertMatch> alertMatches, 
                          long analysisTimeNs) {
        this.originalMessage = originalMessage;
        this.strippedMessage = strippedMessage;
        this.filterMatches = filterMatches != null ? filterMatches : Collections.emptyList();
        this.alertMatches = alertMatches != null ? alertMatches : Collections.emptyList();
        this.analysisTimeNs = analysisTimeNs;
        this.hasAnyMatches = !this.filterMatches.isEmpty() || !this.alertMatches.isEmpty();
    }
    
    public String getOriginalMessage() { return originalMessage; }
    public String getStrippedMessage() { return strippedMessage; }
    public List<FilterMatch> getFilterMatches() { return filterMatches; }
    public List<AlertMatch> getAlertMatches() { return alertMatches; }
    public long getAnalysisTimeNs() { return analysisTimeNs; }
    public boolean hasAnyMatches() { return hasAnyMatches; }
    public boolean hasFilterMatches() { return !filterMatches.isEmpty(); }
    public boolean hasAlertMatches() { return !alertMatches.isEmpty(); }
    
    /**
     * Represents a filter rule match with its configuration and match details
     */
    public static class FilterMatch {
        private final String ruleName;
        private final Rule rule;
        private final String matchedText;
        private final int matchStart;
        private final int matchEnd;
        private final Map<String, String> captureGroups;
        
        public FilterMatch(String ruleName, Rule rule, String matchedText, 
                          int matchStart, int matchEnd, Map<String, String> captureGroups) {
            this.ruleName = ruleName;
            this.rule = rule;
            this.matchedText = matchedText;
            this.matchStart = matchStart;
            this.matchEnd = matchEnd;
            this.captureGroups = captureGroups != null ? captureGroups : Collections.emptyMap();
        }
        
        public String getRuleName() { return ruleName; }
        public Rule getRule() { return rule; }
        public String getMatchedText() { return matchedText; }
        public int getMatchStart() { return matchStart; }
        public int getMatchEnd() { return matchEnd; }
        public Map<String, String> getCaptureGroups() { return captureGroups; }
    }
    
    /**
     * Represents an alert match with its configuration
     */
    public static class AlertMatch {
        private final String alertKey;
        private final AlertData alertData;
        private final String matchedText;
        private final int matchStart;
        private final int matchEnd;
        
        public AlertMatch(String alertKey, AlertData alertData, String matchedText, 
                         int matchStart, int matchEnd) {
            this.alertKey = alertKey;
            this.alertData = alertData;
            this.matchedText = matchedText;
            this.matchStart = matchStart;
            this.matchEnd = matchEnd;
        }
        
        public String getAlertKey() { return alertKey; }
        public AlertData getAlertData() { return alertData; }
        public String getMatchedText() { return matchedText; }
        public int getMatchStart() { return matchStart; }
        public int getMatchEnd() { return matchEnd; }
    }
    
    /**
     * Empty analysis for when no patterns are configured or message should be skipped
     */
    public static final MessageAnalysis EMPTY = new MessageAnalysis("", "", 
        Collections.emptyList(), Collections.emptyList(), 0L);
    
    @Override
    public String toString() {
        return String.format("MessageAnalysis{filters=%d, alerts=%d, time=%.2fμs}", 
            filterMatches.size(), alertMatches.size(), analysisTimeNs / 1000.0);
    }
}
