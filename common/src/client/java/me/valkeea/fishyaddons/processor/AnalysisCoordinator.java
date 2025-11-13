package me.valkeea.fishyaddons.processor;

import java.util.concurrent.ConcurrentHashMap;

import org.jetbrains.annotations.Nullable;

import me.valkeea.fishyaddons.config.FilterConfig;
import me.valkeea.fishyaddons.config.RuleFactory;
import net.minecraft.text.Text;

@SuppressWarnings("squid:S6548")
public class AnalysisCoordinator {
    private AnalysisCoordinator() {}

    private static final AnalysisCoordinator INSTANCE = new AnalysisCoordinator();
    public static AnalysisCoordinator getInstance() { return INSTANCE; }
    
    private final ConcurrentHashMap<String, AnalysisResult> analysisCache = new ConcurrentHashMap<>();
    private static final int MAX_CACHE_SIZE = 600;
    
    public static class AnalysisResult {
        private final MessageAnalysis baseAnalysis;
        private final boolean isSeaCreatureMessage;
        private final String seaCreatureId;
        private final boolean isDoubleHook;
        private final long analysisTimestamp;
        private final String originalMessage;
        
        public AnalysisResult(MessageAnalysis baseAnalysis, boolean isSeaCreatureMessage, 
                            String seaCreatureId, boolean isDoubleHook, String originalMessage) {
            this.baseAnalysis = baseAnalysis;
            this.isSeaCreatureMessage = isSeaCreatureMessage;
            this.seaCreatureId = seaCreatureId;
            this.isDoubleHook = isDoubleHook;
            this.analysisTimestamp = System.currentTimeMillis();
            this.originalMessage = originalMessage;
        }
        
        public MessageAnalysis getBaseAnalysis() { return baseAnalysis; }
        public boolean isSeaCreatureMessage() { return isSeaCreatureMessage; }
        public String getSeaCreatureId() { return seaCreatureId; }
        public boolean isDoubleHook() { return isDoubleHook; }
        public long getAnalysisTimestamp() { return analysisTimestamp; }
        public String getOriginalMessage() { return originalMessage; }
        
        public boolean hasFilterMatches() { return baseAnalysis != null && baseAnalysis.hasFilterMatches(); }
        public boolean hasAlertMatches() { return baseAnalysis != null && baseAnalysis.hasAlertMatches(); }

        public MessageAnalysis.AlertMatch getFirstAlertMatch() {
            if (hasAlertMatches()) {
                return baseAnalysis.getAlertMatches().get(0);
            }
            return null;
        }

        public MessageAnalysis.FilterMatch getFirstFilterMatch() {
            if (hasFilterMatches()) {
                return baseAnalysis.getFilterMatches().get(0);
            }
            return null;
        }
    }
    
    /**
     * Initial message analysis.
     */
    public AnalysisResult analyzeMessage(String originalMessage) {
        if (originalMessage == null || originalMessage.trim().isEmpty()) {
            return new AnalysisResult(MessageAnalysis.EMPTY, false, null, false, originalMessage);
        }
        
        AnalysisResult cached = analysisCache.get(originalMessage);
        if (cached != null && isCacheValid(cached)) {
            return cached;
        }
        
        ensureSeaCreatureRulesAvailable();

        var baseAnalysis = BaseAnalysis.onPacket(originalMessage);
        var scInfo = extractScInfo(baseAnalysis);
        
        AnalysisResult result = new AnalysisResult(
            baseAnalysis,
            scInfo.isSeaCreatureMessage,
            scInfo.creatureId,
            scInfo.isDoubleHook,
            originalMessage
        );
        
        cacheResult(originalMessage, result);
        
        return result;
    }
    
    /**
     * Analyzes a message that may have been modified by other mods.
     * (this allows users to use either original, or messages modified by other mods)
     */
    public AnalysisResult analyzeMessage(String originalMessage, @Nullable Text modifiedText) {

        var baseResult = analyzeMessage(originalMessage);
        
        if (modifiedText == null || modifiedText.getString().equals(originalMessage)) {
            return baseResult;
        }

        var modifiedAnalysis = BaseAnalysis.onRender(originalMessage, modifiedText);

        return new AnalysisResult(
            modifiedAnalysis,
            baseResult.isSeaCreatureMessage,
            baseResult.seaCreatureId,
            baseResult.isDoubleHook,
            originalMessage
        );
    }
    
    private void ensureSeaCreatureRulesAvailable() {
        boolean anyFeatureNeedsRules = FilterConfig.usingScRules();
        
        if (anyFeatureNeedsRules && !FilterConfig.areScRulesLoaded()) {
            FilterConfig.refreshScRules();
        }
    }
    
    private SeaCreatureInfo extractScInfo(MessageAnalysis analysis) {
        if (analysis == null || !analysis.hasFilterMatches()) {
            return new SeaCreatureInfo(false, null, false);
        }
        
        for (var filterMatch : analysis.getFilterMatches()) {
            var ruleName = filterMatch.getRuleName();
            if (isScMessage(ruleName)) {
                var creatureId = ruleName.substring(3);
                var isDoubleHook = isDh();
                return new SeaCreatureInfo(true, creatureId, isDoubleHook);
            }
        }
        
        return new SeaCreatureInfo(false, null, false);
    }

    private boolean isScMessage(String ruleName) {
        return ruleName.startsWith("sc_");
    }
    
    private boolean isDh() {
        return FilterConfig.MessageContext.hasTriggerWithin(RuleFactory.getDhTriggers(), 50);
    }
    
    private static class SeaCreatureInfo {
        final boolean isSeaCreatureMessage;
        final String creatureId;
        final boolean isDoubleHook;
        
        SeaCreatureInfo(boolean isSeaCreatureMessage, String creatureId, boolean isDoubleHook) {
            this.isSeaCreatureMessage = isSeaCreatureMessage;
            this.creatureId = creatureId;
            this.isDoubleHook = isDoubleHook;
        }
    }
    
    private boolean isCacheValid(AnalysisResult cached) {
        return (System.currentTimeMillis() - cached.getAnalysisTimestamp()) < 5000;
    }
    
    private void cacheResult(String message, AnalysisResult result) {
        if (analysisCache.size() >= MAX_CACHE_SIZE) {
            analysisCache.entrySet().removeIf(entry -> 
                (System.currentTimeMillis() - entry.getValue().getAnalysisTimestamp()) > 10000
            );
        }
        analysisCache.put(message, result);
    }
    
    public static void clearCache() {
        getInstance().analysisCache.clear();
    }
}
