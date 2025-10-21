package me.valkeea.fishyaddons.processor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import me.valkeea.fishyaddons.config.FilterConfig;
import me.valkeea.fishyaddons.config.FilterConfig.Rule;
import me.valkeea.fishyaddons.config.FishyConfig;
import me.valkeea.fishyaddons.config.FishyConfig.AlertData;
import me.valkeea.fishyaddons.processor.MessageAnalysis.AlertMatch;
import me.valkeea.fishyaddons.processor.MessageAnalysis.FilterMatch;
import me.valkeea.fishyaddons.util.text.TextUtils;

/**
 * Single-pass message analysis for chat filter and alerts
 */
public class SharedMessageDetector {
    private SharedMessageDetector() {}
    
    private static final Map<String, MessageAnalysis> resultCache = new ConcurrentHashMap<>();
    
    private static volatile long lastFilterConfigVersion = -1;
    private static volatile long lastAlertConfigVersion = -1;
    
    private static final CopyOnWriteArrayList<Map.Entry<String, Rule>> cachedFilterRules = new CopyOnWriteArrayList<>();
    private static final Map<String, AlertData> cachedAlertData = new ConcurrentHashMap<>();
    
    private static final int MAX_CACHE_SIZE = 1000;
    
    public static MessageAnalysis analyzeMessage(String originalMessage) {
        if (originalMessage == null || originalMessage.isEmpty()) {
            return MessageAnalysis.EMPTY;
        }
        
        MessageAnalysis cached = resultCache.get(originalMessage);
        if (cached != null) {
            return cached;
        }
        
        long startTime = System.nanoTime();
        
        refreshConfigurationCache();
        
        String strippedMessage = TextUtils.stripColor(originalMessage);
        List<FilterMatch> filterMatches = analyzeFilterPatterns(originalMessage);
        List<AlertMatch> alertMatches = analyzeAlertPatterns(strippedMessage);

        long endTime = System.nanoTime();
        long analysisTime = endTime - startTime;
        
        MessageAnalysis result = new MessageAnalysis(originalMessage, strippedMessage, 
            filterMatches, alertMatches, analysisTime);
        
        cacheResult(originalMessage, result);
        
        return result;
    }
    
    private static List<FilterMatch> analyzeFilterPatterns(String originalMessage) {
        List<FilterMatch> matches = new ArrayList<>();
        
        for (Map.Entry<String, Rule> entry : cachedFilterRules) {
            String ruleName = entry.getKey();
            Rule rule = entry.getValue();
            
            if (rule.isEnabled()) {
                try {
                    FilterMatch match = processFilterRule(ruleName, rule, originalMessage);
                    if (match != null) {
                        matches.add(match);
                        
                        if (rule.getReplacement() != null && !rule.getReplacement().isEmpty()) {
                            break;
                        }
                    }
                } catch (Exception e) {
                    System.err.println("[SharedMessageDetector] Error processing filter rule '" + 
                        ruleName + "': " + e.getMessage());
                }
            }
        }
        
        return matches;
    }
    
    private static FilterMatch processFilterRule(String ruleName, Rule rule, String originalMessage) {
        return processStringMatch(ruleName, rule, originalMessage, rule.requireFullMatch());
    }
    
    private static FilterMatch processStringMatch(String ruleName, Rule rule, String originalMessage, boolean requireFullMatch) {
        if (requireFullMatch) {
            if (originalMessage.equals(rule.getSearchText())) {
                return new FilterMatch(
                    ruleName,
                    rule,
                    rule.getSearchText(),
                    0,
                    originalMessage.length(),
                    Collections.emptyMap()
                );
            }
        } else {
            int index = originalMessage.indexOf(rule.getSearchText());
            if (index != -1) {
                return new FilterMatch(
                    ruleName,
                    rule,
                    rule.getSearchText(),
                    index,
                    index + rule.getSearchText().length(),
                    Collections.emptyMap()
                );
            }
        }
        return null;
    }
    
    private static List<AlertMatch> analyzeAlertPatterns(String strippedMessage) {
        List<AlertMatch> matches = new ArrayList<>();
        
        for (Map.Entry<String, AlertData> entry : cachedAlertData.entrySet()) {
            String alertKey = entry.getKey();
            AlertData alertData = entry.getValue();
            
            if (alertData.isToggled()) {
                try {
                    int matchIndex = strippedMessage.indexOf(alertKey);
                    if (alertData.isStartsWith() && matchIndex != 0) {
                        matchIndex = -1;
                    }
                    
                    if (matchIndex != -1) {
                        AlertMatch match = new AlertMatch(
                            alertKey,
                            alertData,
                            alertKey,
                            matchIndex,
                            matchIndex + alertKey.length()
                        );
                        
                        matches.add(match);
                        return matches;
                    }
                } catch (Exception e) {
                    System.err.println("[SharedMessageDetector] Error processing alert '" + 
                        alertKey + "': " + e.getMessage());
                }
            }
        }
        
        return matches;
    }
    
    private static void refreshConfigurationCache() {
        Map<String, Rule> allRules = FilterConfig.getAllRules();
        List<Map.Entry<String, Rule>> enabledRules = new ArrayList<>();
        
        for (Map.Entry<String, Rule> entry : allRules.entrySet()) {
            if (entry.getValue().isEnabled()) {
                enabledRules.add(entry);
            }
        }
        enabledRules.sort((a, b) -> Integer.compare(b.getValue().getPriority(), a.getValue().getPriority()));
        
        long currentFilterVersion = enabledRules.hashCode();
        
        if (currentFilterVersion != lastFilterConfigVersion) {
            
            cachedFilterRules.clear();
            cachedFilterRules.addAll(enabledRules);
            lastFilterConfigVersion = currentFilterVersion;
            resultCache.clear();
        }
        
        Map<String, AlertData> currentAlerts = FishyConfig.getChatAlerts();
        long currentAlertVersion = currentAlerts.hashCode();
        if (currentAlertVersion != lastAlertConfigVersion) {
            cachedAlertData.clear();
            cachedAlertData.putAll(currentAlerts);
            lastAlertConfigVersion = currentAlertVersion;
            resultCache.clear();
        }
    }
    
    private static void cacheResult(String message, MessageAnalysis result) {
        if (resultCache.size() >= MAX_CACHE_SIZE) {
            resultCache.clear();
        }
        resultCache.put(message, result);
    }
    
    public static void clearCaches() {
        resultCache.clear();
        lastFilterConfigVersion = -1;
        lastAlertConfigVersion = -1;
    }
}
