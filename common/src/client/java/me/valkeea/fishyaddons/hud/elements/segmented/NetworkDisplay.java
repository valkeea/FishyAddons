package me.valkeea.fishyaddons.hud.elements.segmented;

import java.util.ArrayList;
import java.util.List;

import me.valkeea.fishyaddons.feature.qol.NetworkMetrics;
import me.valkeea.fishyaddons.hud.base.SegmentedTextElement;
import me.valkeea.fishyaddons.util.text.Color;
import me.valkeea.fishyaddons.vconfig.api.BooleanKey;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public class NetworkDisplay extends SegmentedTextElement {
    
    public NetworkDisplay() {
        super(
            BooleanKey.HUD_METRICS_ENABLED,
            "Network Display",
            5, 5,
            12,
            0xFFDB8BF2,
            true,
            false
        );
    }

    @Override
    protected boolean shouldRender() {
        return NetworkMetrics.isOn();
    }

    @Override
    protected Pair[] getContent() {
        if (!shouldRender()) return new Pair[0];

        var mc = Minecraft.getInstance();
        int ping = NetworkMetrics.getPing();
        int fps = mc.getFps();
        
        boolean showPing = NetworkMetrics.shouldDisplay(BooleanKey.METRICS_SHOW_PING);
        boolean showTps = NetworkMetrics.shouldDisplay(BooleanKey.METRICS_SHOW_TPS);
        boolean showFps = NetworkMetrics.shouldDisplay(BooleanKey.METRICS_SHOW_FPS);

        if (!isEditingMode() && !showPing && !showTps && !showFps) return new Pair[0];
        if (!isEditingMode() && showPing && ping < 0) return new Pair[0];

        return buildContent(showPing, showTps, showFps, ping, fps);
    }
    
    private Pair[] buildContent(boolean showPing, boolean showTps, boolean showFps, int ping, int fps) {
        List<Pair> content = new ArrayList<>();
        int labelColor = getCachedState().color;
        int valueColor = Color.brighten(labelColor, 0.7f);
        
        if (showPing || isEditingMode()) {
            addPing(content, ping, labelColor, valueColor, showPing);
        }
        
        if (showTps || isEditingMode()) {
            addTps(content, labelColor, valueColor, showTps);
        }
        
        if (showFps || isEditingMode()) {
            addFps(content, fps, labelColor, valueColor, showFps);
        }
        
        return content.toArray(new Pair[0]);
    }
    
    private void addPing(List<Pair> content, int ping, int labelColor, int valueColor, boolean showPing) {
        var pingLabel = Component.literal("Ping:");
        var pingValue = Component.literal(ping >= 0 ? " " + ping + " ms" : " ?");
        int pingValueColor = showPing ? valueColor : 0x80808080;
        content.add(new Pair(pingLabel, pingValue, labelColor, pingValueColor));
    }
    
    private void addTps(List<Pair> content, int labelColor, int valueColor, boolean showTps) {
        var tpsLabel = Component.literal("TPS:");
        var tpsValue = Component.literal(" " + NetworkMetrics.getTpsString());
        int tpsValueColor = showTps ? valueColor : 0x80808080;
        content.add(new Pair(tpsLabel, tpsValue, labelColor, tpsValueColor));
    }

    private void addFps(List<Pair> content, int fps, int labelColor, int valueColor, boolean showFps) {
        var fpsLabel = Component.literal("FPS:");
        var fpsValue = Component.literal(" " + fps);
        int fpsValueColor = showFps ? valueColor : 0x80808080;
        content.add(new Pair(fpsLabel, fpsValue, labelColor, fpsValueColor));
    }
}
