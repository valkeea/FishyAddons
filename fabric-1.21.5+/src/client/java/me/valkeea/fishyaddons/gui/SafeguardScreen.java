package me.valkeea.fishyaddons.gui;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import me.valkeea.fishyaddons.config.FishyConfig;
import me.valkeea.fishyaddons.config.ItemConfig;
import me.valkeea.fishyaddons.safeguard.BlacklistManager;
import me.valkeea.fishyaddons.safeguard.BlacklistManager.GuiBlacklistEntry;
import me.valkeea.fishyaddons.util.KeyUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;

public class SafeguardScreen extends Screen {
    private final Map<ButtonWidget, GuiBlacklistEntry> blacklistButtons = new HashMap<>();
    private static final int BTNW = 120;
    private static final int BBTNW = 200;
    private static final int BTNH = 20;

    private int keyBtnX, keyBtnY, keyBtnW, keyBtnH; 

    public SafeguardScreen() {
        super(Text.literal("FA safeguard"));
    }

    @Override
    protected void init() {
        this.clearChildren();
        blacklistButtons.clear();

        int centerX = this.width / 2;
        int y = this.height / 3 - 30;

        addDrawableChild(new FaButton(
            centerX - 240, y, BTNW, BTNH,
            getToggleButtonText(),
            btn -> {
                ItemConfig.setSellProtectionEnabled(!ItemConfig.isSellProtectionEnabled());
                ItemConfig.saveConfigIfNeeded();
                btn.setMessage(getToggleButtonText());
            }
        ));

        addDrawableChild(new FaButton(
            centerX - 120, y, BTNW, BTNH,
            getTooltipToggleText(),
            btn -> {
                ItemConfig.setTooltipEnabled(!ItemConfig.isTooltipEnabled());
                ItemConfig.saveConfigIfNeeded();
                btn.setMessage(getTooltipToggleText());
            }
        ));

        addDrawableChild(new FaButton(
            centerX, y, BTNW, BTNH,
            getSoundToggleText(),
            btn -> {
                ItemConfig.setProtectTriggerEnabled(!ItemConfig.isProtectTriggerEnabled());
                ItemConfig.saveConfigIfNeeded();
                btn.setMessage(getSoundToggleText());
            }
        ));

        addDrawableChild(new FaButton(
            centerX + 120, y, BTNW, BTNH,
            getNotiToggleText(),
            btn -> {
                ItemConfig.setProtectNotiEnabled(!ItemConfig.isProtectNotiEnabled());
                ItemConfig.saveConfigIfNeeded();
                btn.setMessage(getNotiToggleText());
            }
        ));

        int by = this.height / 3 + 30;
        for (GuiBlacklistEntry entry : BlacklistManager.getMergedBlacklist()) {
            String label = "blacklist." + entry.identifiers.get(0).toLowerCase().replace(" ", "_");
            FaButton blacklistButton = new FaButton(
                centerX - 100, by, 200, 20,
                Text.translatable(label).styled(s -> s.withColor(entry.isEnabled() ? 0xCCFFCC : 0xFF8080)),
                btn -> {
                    entry.setEnabled(!entry.isEnabled());
                    BlacklistManager.updateBlacklistEntry(entry.identifiers.get(0), entry.isEnabled());
                    this.setFocused(false);
                    btn.setMessage(
                        Text.translatable(label).styled(s -> s.withColor(entry.isEnabled() ? 0xCCFFCC : 0xFF8080))
                    );
                }
            );
            addDrawableChild(blacklistButton);
            blacklistButtons.put(blacklistButton, entry);
            by += 24;
        }

        keyBtnX = centerX - 100;
        keyBtnY = by + 10;
        keyBtnW = BBTNW;
        keyBtnH = BTNH;

        addDrawableChild(new ListeningWidget(
            keyBtnX, keyBtnY, keyBtnW, keyBtnH,
            FishyConfig.getLockKey(),
            FishyConfig::setLockKey,
            keyName -> getLockKeyButtonText()
        ));
        by += 30;

        addDrawableChild(new FaButton(
            centerX - 100, by, BBTNW, BTNH,
            getLockTriggerText(),
            btn -> {
                ItemConfig.setLockTriggerEnabled(!ItemConfig.isLockTriggerEnabled());
                ItemConfig.saveConfigIfNeeded();
                this.setFocused(false);            
                btn.setMessage(getLockTriggerText());
            }
        ));
        by += 30;

        addDrawableChild(new FaButton(
            centerX - 100, by, 80, BTNH,
            Text.literal("Back"),
            btn -> MinecraftClient.getInstance().setScreen(new FishyAddonsScreen())
        ));

        addDrawableChild(new FaButton(
            centerX + 20, by, 80, BTNH,
            Text.literal("Close"),
            btn -> MinecraftClient.getInstance().setScreen(null)
        ));
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

        if (mouseX >= keyBtnX && mouseX <= keyBtnX + keyBtnW && mouseY >= keyBtnY && mouseY <= keyBtnY + keyBtnH) {
            List<Text> tooltipLines = Arrays.asList(
                Text.literal("Slotlocking/Binding:"),
                Text.literal("- §8Press the key to lock slots,"),
                Text.literal("  §8drag while the key is pressed to bind slots"),
                Text.literal("- §8left click to unbind")
            );
            GuiUtil.fishyTooltip(context, this.textRenderer, tooltipLines, mouseX, mouseY);
        }
    }
}