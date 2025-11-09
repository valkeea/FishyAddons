package me.valkeea.fishyaddons.hud.elements.segmented;

import java.util.ArrayList;
import java.util.List;

import me.valkeea.fishyaddons.config.Key;
import me.valkeea.fishyaddons.feature.qol.NetworkMetrics;
import me.valkeea.fishyaddons.hud.base.SegmentedTextElement;
import me.valkeea.fishyaddons.util.text.Color;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

public class NetworkDisplay extends SegmentedTextElement {
    
    public NetworkDisplay() {
        super(
            Key.HUD_PING_ENABLED,
            "Network Display",
            5, 5,
            12,
            0xEECAEC,
            false,
            true
        );
    }

    @Override
    protected boolean shouldRender() {
        return NetworkMetrics.isOn();
    }

    @Override
    protected Component[] getComponents() {
        if (!shouldRender()) return new Component[0];

        var mc = MinecraftClient.getInstance();
        int ping = NetworkMetrics.getPing();
        int fps = mc.getCurrentFps();
        
        boolean showPing = NetworkMetrics.shouldDisplay(Key.HUD_PING_SHOW_PING);
        boolean showTps = NetworkMetrics.shouldDisplay(Key.HUD_PING_SHOW_TPS);
        boolean showFps = NetworkMetrics.shouldDisplay(Key.HUD_PING_SHOW_FPS);

        if (!isEditingMode() && !showPing && !showTps && !showFps) return new Component[0];
        if (!isEditingMode() && showPing && ping < 0) return new Component[0];

        return buildComponents(showPing, showTps, showFps, ping, fps);
    }
    
    private Component[] buildComponents(boolean showPing, boolean showTps, boolean showFps, int ping, int fps) {
        List<Component> components = new ArrayList<>();
        int labelColor = getCachedState().color;
        int valueColor = Color.brighten(labelColor, 0.7f);
        
        if (showPing || isEditingMode()) {
            addPing(components, ping, labelColor, valueColor, showPing);
        }
        
        if (showTps || isEditingMode()) {
            addTps(components, labelColor, valueColor, showTps);
        }
        
        if (showFps || isEditingMode()) {
            addFps(components, fps, labelColor, valueColor, showFps);
        }
        
        return components.toArray(new Component[0]);
    }
    
    private void addPing(List<Component> components, int ping, int labelColor, int valueColor, boolean showPing) {
        Text pingLabel = Text.literal("Ping:");
        Text pingValue = Text.literal(ping >= 0 ? " " + ping + " ms" : " ?");
        int pingValueColor = showPing ? valueColor : 0x808080;
        components.add(new Component(pingLabel, pingValue, labelColor, pingValueColor));
    }
    
    private void addTps(List<Component> components, int labelColor, int valueColor, boolean showTps) {
        Text tpsLabel = Text.literal("TPS:");
        Text tpsValue = Text.literal(" " + NetworkMetrics.getTpsString());
        int tpsValueColor = showTps ? valueColor : 0x808080;
        components.add(new Component(tpsLabel, tpsValue, labelColor, tpsValueColor));
    }

    private void addFps(List<Component> components, int fps, int labelColor, int valueColor, boolean showFps) {
        Text fpsLabel = Text.literal("FPS:");
        Text fpsValue = Text.literal(" " + fps);
        int fpsValueColor = showFps ? valueColor : 0x808080;
        components.add(new Component(fpsLabel, fpsValue, labelColor, fpsValueColor));
    }
}
