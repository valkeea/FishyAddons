package me.valkeea.fishyaddons.processor.handlers;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import me.valkeea.fishyaddons.config.FishyConfig;
import me.valkeea.fishyaddons.config.Key;
import me.valkeea.fishyaddons.feature.skyblock.timer.EffectRegistry;
import me.valkeea.fishyaddons.feature.skyblock.timer.EffectTimers;
import me.valkeea.fishyaddons.processor.ChatHandler;
import me.valkeea.fishyaddons.processor.ChatHandlerResult;
import me.valkeea.fishyaddons.processor.ChatMessageContext;
import net.minecraft.util.Identifier;

public class TimerHandler implements ChatHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger("FishyAddons/EffectTimerChatHandler");

    private static final Pattern CONSUMED_PATTERN = Pattern.compile(
        "(?i)you consumed an?\s+(.+?)\s+and gained.*?for\s+(\\d+)\s*([smhd])",
        Pattern.DOTALL);

    private static final long ROSEWATER_DURATION_MS = 2L * 60L * 60L * 1000L;
    private static final long GUMMY_DURATION_MS = 1L * 60L * 60L * 1000L;

    @Override
    public int getPriority() { return 50; }

    @Override
    public String getHandlerName() { return "EffectTimers"; }

    @Override
    public boolean shouldHandle(ChatMessageContext context) {
        if (!context.isSkyblockMessage()) return false;        
        String s = context.getLowerCleanString();
        return s.contains("you ");
    }

    @Override
    public ChatHandlerResult handle(ChatMessageContext context) {

        try {

            String msg = context.getCleanString();

            if (msg.equals("You ate a Re-heated Gummy Polar Bear!")) {
                var config = EffectRegistry.get("re-heated gummy polar bear");
                if (config != null) {
                    EffectTimers.getInstance().stackCooldown(config.displayName, GUMMY_DURATION_MS, config.texture, config.pauseOffline);
                    return ChatHandlerResult.STOP;
                }
            }

            if (msg.equals("You consumed a Filled Rosewater Flask which grew your Greenhouse crops by 4 Growth Stages!")) {
                var config = EffectRegistry.get("filled rosewater flask");
                if (config != null) {
                    EffectTimers.getInstance().addCooldown(config.displayName, ROSEWATER_DURATION_MS, config.texture, config.pauseOffline);
                    return ChatHandlerResult.STOP;
                }
            }

            Matcher m = CONSUMED_PATTERN.matcher(msg);
            if (!m.find()) return ChatHandlerResult.CONTINUE;

            String itemName = m.group(1).trim();
            long durationMs = parseDuration(m.group(2), m.group(3));

            var config = EffectRegistry.get(itemName);
            if (config != null) {
                EffectTimers.getInstance().addCooldown(config.displayName, durationMs, config.texture, config.pauseOffline);
                return ChatHandlerResult.STOP;
            }

        } catch (Exception e) {
            LOGGER.debug("Failed to parse effect duration: {}", e.getMessage());
        }

        return ChatHandlerResult.CONTINUE;
    }

    private static long parseDuration(String number, String unit) {
        long n;
        try { n = Long.parseLong(number); } catch (Exception e) { return 0L; }
        char u = unit == null || unit.isEmpty() ? 's' : Character.toLowerCase(unit.charAt(0));
        return switch (u) {
            case 'd' -> n * 24L * 60L * 60L * 1000L;
            case 'h' -> n * 60L * 60L * 1000L;
            case 'm' -> n * 60L * 1000L;
            default -> n * 1000L;
        };
    }

    @Override
    public boolean isEnabled() {
        return FishyConfig.getState(Key.HUD_EFFECTS_ENABLED, false);
    }
}
