package me.valkeea.fishyaddons.gui;

import java.util.Arrays;
import java.util.List;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class FishyAddonsScreen extends Screen {
    private static final int BTNW = 200;
    private static final int BTNH = 20;

    private int qolBtnX, qolBtnY, qolBtnW, qolBtnH;
    private int visualBtnX, visualBtnY, visualBtnW, visualBtnH;
    private int fgBtnX, fgBtnY, fgBtnW, fgBtnH;

    private SearchMenu searchMenu;

    public FishyAddonsScreen() {
        super(Text.literal("FishyAddons"));
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        visualBtnX = centerX - 100;
        visualBtnY = centerY - 80;
        visualBtnW = BTNW;
        visualBtnH = BTNH;

        addDrawableChild(new FaButton(
            visualBtnX, visualBtnY, visualBtnW, visualBtnH,
            Text.literal("Visual Settings").styled(style -> style.withColor(0xE2CAE9)),
            btn -> MinecraftClient.getInstance().setScreen(new VisualSettingsScreen())
        ));

        qolBtnX = centerX - 100;
        qolBtnY = centerY - 50;
        qolBtnW = BTNW;
        qolBtnH = BTNH;

        addDrawableChild(new FaButton(
            qolBtnX, qolBtnY, qolBtnW, qolBtnH,
            Text.literal("General Qol").styled(style -> style.withColor(0xE2CAE9)),
            btn -> MinecraftClient.getInstance().setScreen(new QolScreen())
        ));

        addDrawableChild(new FaButton(
            centerX - 100, centerY - 20, BTNW, BTNH,
            Text.literal("Skyblock").styled(style -> style.withColor(0xE2CAE9)),
            btn -> MinecraftClient.getInstance().setScreen(new SbScreen())
        ));
        
        fgBtnX = centerX - 100;
        fgBtnY = centerY + 10;
        fgBtnW = BTNW;
        fgBtnH = BTNH;

        addDrawableChild(new FaButton(
            fgBtnX, fgBtnY, fgBtnW, fgBtnH,
            Text.literal("FA Safeguard").styled(style -> style.withColor(0xE2CAE9)),
            btn -> MinecraftClient.getInstance().setScreen(new SafeguardScreen())
        ));

        addDrawableChild(new FaButton(
            centerX - 100, centerY + 40, BTNW, BTNH,
            Text.literal("New Config (WIP)").styled(style -> style.withColor(0x6DE6B5)),
            btn -> MinecraftClient.getInstance().setScreen(new VCScreen())
        ));

        addDrawableChild(new FaButton(
            centerX - 100, centerY + 70, BTNW, BTNH,
            Text.literal("Close").styled(style -> style.withColor(0xFF808080)),
            btn -> MinecraftClient.getInstance().setScreen(null)
        ));

        List<SearchEntry> featureEntries = SearchList.FEATURES;

        searchMenu = new SearchMenu(
            featureEntries,
            this.width / 2 - 100,
            this.height / 2 - 200,
            200,
            25,
            entry -> {
                if (entry != null && entry.onSelect != null) entry.onSelect.run();
            },
            this
        );
        this.addSelectableChild(searchMenu.getSearchField());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, "FishyAddons", this.width / 2, this.height / 2 - 110, 0xFF55FFFF);

        super.render(context, mouseX, mouseY, delta);

        // Tooltip for General Qol
        if (mouseX >= qolBtnX && mouseX <= qolBtnX + qolBtnW && mouseY >= qolBtnY && 
            mouseY <= qolBtnY + qolBtnH && !searchMenu.isVisible()) {
            List<Text> tooltipLines = Arrays.asList(
                Text.literal("General Qol:"),
                Text.literal("- §8Custom Keybinds"),
                Text.literal("- §8Command Aliases"),
                Text.literal("- §8Modify Chat"),
                Text.literal("- §8Chat Event- based screen"),
                Text.literal("  §8/ chat / sound alerts"),
                Text.literal("- §8Custom F5/Camera Settings"),
                Text.literal("- §8Ping HUD display"),
                Text.literal("- §8Clean Wither Impact"),
                Text.literal("- §8Mute Phantoms"),
                Text.literal("- §8Draw Coordinate Beacons")            

            );
            GuiUtil.fishyTooltip(context, this.textRenderer, tooltipLines, mouseX, mouseY);
        }

        // Tooltip for Visual Settings
        if (mouseX >= visualBtnX && mouseX <= visualBtnX + visualBtnW && mouseY >= visualBtnY && 
            mouseY <= visualBtnY + visualBtnH && !searchMenu.isVisible()) {
            List<Text> tooltipLines = Arrays.asList(
                Text.literal("Visual Settings:"),
                Text.literal("- §8Custom Redstone Particles"),
                Text.literal("- §8Island Texture Toggles (1.8.9 only)"),
                Text.literal("- §8Lava Fog Removal")
            );
            GuiUtil.fishyTooltip(context, this.textRenderer, tooltipLines, mouseX, mouseY);
        }

        // Tooltip for FA Safeguard
        if (mouseX >= fgBtnX && mouseX <= fgBtnX + fgBtnW && mouseY >= fgBtnY && 
            mouseY <= fgBtnY + fgBtnH && !searchMenu.isVisible()) {
            List<Text> tooltipLines = Arrays.asList(
                Text.literal("FishyAddons item safeguard:"),
                Text.literal("- §8Drop/Sell Protection"),
                Text.literal("- §8Slotlocking and -binding"),
                Text.literal("- §8Toggle chat, sound and visual cues")
            );
            GuiUtil.fishyTooltip(context, this.textRenderer, tooltipLines, mouseX, mouseY);
        }

        if (searchMenu != null && searchMenu.isVisible()) {
            context.getMatrices().push();
            context.getMatrices().translate(0, 0, 500);
            searchMenu.render(context, this, mouseX, mouseY, delta);
            context.getMatrices().pop();
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (searchMenu != null && searchMenu.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (searchMenu != null && searchMenu.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (searchMenu != null && searchMenu.mouseScrolled(mouseX, mouseY, verticalAmount)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }
}