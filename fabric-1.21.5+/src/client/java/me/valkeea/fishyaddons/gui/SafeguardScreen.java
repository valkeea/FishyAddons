package me.valkeea.fishyaddons.gui;

import me.valkeea.fishyaddons.safeguard.BlacklistManager.GuiBlacklistEntry;
import me.valkeea.fishyaddons.safeguard.BlacklistManager;
import me.valkeea.fishyaddons.config.ItemConfig;
import me.valkeea.fishyaddons.config.FishyConfig;
import me.valkeea.fishyaddons.util.KeyUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.client.util.InputUtil;

import java.util.HashMap;
import java.util.Map;

public class SafeguardScreen extends Screen {
    private final Map<ButtonWidget, GuiBlacklistEntry> blacklistButtons = new HashMap<>();
    private static final int BTNW = 120;
    private static final int BBTNW = 200;
    private static final int BTNH = 20;

    public SafeguardScreen() {
        super(Text.literal("FA safeguard"));
    }

    @Override
    protected void init() {
        this.clearChildren();
        blacklistButtons.clear();

        int centerX = this.width / 2;
        int y = this.height / 3 - 30;

        // Sell Protection toggle
        addDrawableChild(ButtonWidget.builder(getToggleButtonText(), btn -> {
            ItemConfig.setSellProtectionEnabled(!ItemConfig.isSellProtectionEnabled());
            ItemConfig.saveConfigIfNeeded();
            btn.setMessage(getToggleButtonText());
        }).dimensions(centerX - 240, y, BTNW, BTNH).build());

        // Tooltip toggle
        addDrawableChild(ButtonWidget.builder(getTooltipToggleText(), btn -> {
            ItemConfig.setTooltipEnabled(!ItemConfig.isTooltipEnabled());
            ItemConfig.saveConfigIfNeeded();
            btn.setMessage(getTooltipToggleText());
        }).dimensions(centerX - 120, y, BTNW, BTNH).build());

        // Sound toggle
        addDrawableChild(ButtonWidget.builder(getSoundToggleText(), btn -> {
            ItemConfig.setProtectTriggerEnabled(!ItemConfig.isProtectTriggerEnabled());
            ItemConfig.saveConfigIfNeeded();
            btn.setMessage(getSoundToggleText());
        }).dimensions(centerX, y, BTNW, BTNH).build());

        // Notification toggle
        addDrawableChild(ButtonWidget.builder(getNotiToggleText(), btn -> {
            ItemConfig.setProtectNotiEnabled(!ItemConfig.isProtectNotiEnabled());
            ItemConfig.saveConfigIfNeeded();
            btn.setMessage(getNotiToggleText());
        }).dimensions(centerX + 120, y, BTNW, BTNH).build());

        // Blacklist entries
        int by = this.height / 3 + 30;
        for (GuiBlacklistEntry entry : BlacklistManager.getMergedBlacklist()) {
            String label = "blacklist." + entry.identifiers.get(0).toLowerCase().replace(" ", "_");
            ButtonWidget blacklistButton = addDrawableChild(ButtonWidget.builder(Text.translatable(
                label).styled(s -> s.withColor(entry.enabled ? 0xCCFFCC : 0xFF8080)), btn -> {
                entry.enabled = !entry.enabled;
                BlacklistManager.updateBlacklistEntry(entry.identifiers.get(0), entry.enabled);
                this.setFocused(false);
                btn.setMessage(
                    Text.translatable(label).styled(s -> s.withColor(entry.enabled ? 0xCCFFCC : 0xFF8080))
                );
            }).dimensions(centerX - 100, by, 200, 20).build());
            blacklistButtons.put(blacklistButton, entry);
            by += 24;
        }

        // Lock/Bind Slot Keybind button
        int keyButtonY = by + 10;
        addDrawableChild(new ListeningWidget(
            centerX - 100, keyButtonY, BBTNW, BTNH,
            FishyConfig.getLockKey(),
            FishyConfig::setLockKey,
            keyName -> getLockKeyButtonText()
        ));

        by += 30;
        // Slotlocking Sound toggle
        addDrawableChild(ButtonWidget.builder(getLockTriggerText(), btn -> {
            ItemConfig.setLockTriggerEnabled(!ItemConfig.isLockTriggerEnabled());
            ItemConfig.saveConfigIfNeeded();
            this.setFocused(false);            
            btn.setMessage(getLockTriggerText());
        }).dimensions(centerX - 100, by, BBTNW, BTNH).build());
        by += 30;

        // Back and Close buttons
        addDrawableChild(ButtonWidget.builder(Text.literal("Back"), btn -> 
            MinecraftClient.getInstance().setScreen(new FishyAddonsScreen())
        ).dimensions(centerX - 100, by, 80, BTNH).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Close"), btn -> 
            MinecraftClient.getInstance().setScreen(null)
        ).dimensions(centerX + 20, by, 80, BTNH).build());
    }

    private Text getToggleButtonText() {
        return GuiUtil.onOffLabel("Sell Protection", ItemConfig.isSellProtectionEnabled());
    }

    private Text getTooltipToggleText() {
        return GuiUtil.onOffLabel("Tooltip", ItemConfig.isTooltipEnabled());
    }

    private Text getSoundToggleText() {
        return GuiUtil.onOffLabel("Soundeffect", ItemConfig.isProtectTriggerEnabled());
    }

    private Text getNotiToggleText() {
        return GuiUtil.onOffLabel("Chat Notifications", ItemConfig.isProtectNotiEnabled());
    }

    private Text getLockTriggerText() {
        return GuiUtil.onOffLabel("Slotlocking Soundeffect", ItemConfig.isLockTriggerEnabled());
    }    

    private Text getLockKeyButtonText() {
        String keyName = FishyConfig.getLockKey();
        int keyCode = KeyUtil.getKeyCodeFromString(keyName);
        String display = keyCode != -1 ? InputUtil.fromKeyCode(keyCode, 0).getLocalizedText().getString() : keyName;
        return Text.literal("Lock/Bind Slot Key: ").append(
            Text.literal(display).styled(s -> s.withColor(0x55FFFF))
        );
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, "FA safeguard", this.width / 2, this.height / 3 - 60, 0xFF55FFFF);
        super.render(context, mouseX, mouseY, delta);
    }
}