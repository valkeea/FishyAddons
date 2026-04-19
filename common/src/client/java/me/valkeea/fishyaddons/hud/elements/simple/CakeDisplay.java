package me.valkeea.fishyaddons.hud.elements.simple;

import java.util.Map;

import me.valkeea.fishyaddons.feature.skyblock.timer.CakeTimer;
import me.valkeea.fishyaddons.hud.base.SimpleHudElement;
import me.valkeea.fishyaddons.tool.FishyMode;
import me.valkeea.fishyaddons.util.SpriteUtil;
import me.valkeea.fishyaddons.vconfig.api.Config;
import me.valkeea.fishyaddons.vconfig.api.BooleanKey;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class CakeDisplay extends SimpleHudElement {

    public CakeDisplay() {
        super(
            BooleanKey.HUD_CENTURY_CAKE_ENABLED,
            "Century Cake Timer",
            50, 5,
            12,
            0xFFFFFFFF,
            true,
            false
        );
    }

    @Override
    protected boolean shouldRender() {
        return Config.get(BooleanKey.HUD_CENTURY_CAKE_ENABLED);
    }
    
    @Override
    protected Text getText() {
        
        String displayText;
        String symbolText = "";

        var timer = CakeTimer.getInstance();

        Map<String, Long> activeCakes = timer.getActiveCakes();
        if (activeCakes.isEmpty()) displayText = "Expired";

        else {
            String nextCake = timer.getNextExpiringCake();
            symbolText = nextCake != null ? timer.symbol(nextCake) : "";
            long timeLeft = timer.getTimeUntilNextExpiry();
            displayText = CakeTimer.formatTimeLeft(timeLeft);
        }
   
        return Text.empty()
            .append(Text.literal(symbolText).styled(s -> s.withColor(0xFF808080)))
            .append(Text.literal(displayText).styled(s2 -> s2.withColor(getCachedState().color)));

    }

    @Override
    protected Identifier getIcon() {
        return SpriteUtil.createModSprite("gui/" + FishyMode.themeName() + "/cake");
    }
}
