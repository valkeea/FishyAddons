package me.valkeea.fishyaddons.gui;

import java.util.Arrays;
import java.util.List;

import me.valkeea.fishyaddons.config.FishyConfig;
import me.valkeea.fishyaddons.config.Key;
import me.valkeea.fishyaddons.handler.CopyChat;
import me.valkeea.fishyaddons.handler.MobAnimations;
import me.valkeea.fishyaddons.handler.SkyblockCleaner;
import me.valkeea.fishyaddons.handler.ItemSearchOverlay;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

public class QolScreen extends Screen {
    private static final int BTNW = 200;
    private static final int BTNH = 20;

    private int cmdBtnX, cmdBtnY, cmdBtnW, cmdBtnH;
    private int keyBtnX, keyBtnY, keyBtnW, keyBtnH;
    private int chatBtnX, chatBtnY, chatBtnW, chatBtnH;
    private int alertBtnX, alertBtnY, alertBtnW, alertBtnH;
    private int invBtnX, invBtnY, invBtnW, invBtnH;
    private int f5BtnX, f5BtnY, f5BtnW, f5BtnH;
    private int hypBtnX, hypBtnY, hypBtnW, hypBtnH;
    private int rndrBtnX, rndrBtnY, rndrBtnW, rndrBtnH;
    private int copyBtnX, copyBtnY, copyBtnW, copyBtnH;
    private CompactSlider opacitySlider;

    public QolScreen() {
        super(Text.literal("General Qol"));
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        cmdBtnX = centerX - 300;
        cmdBtnY = centerY - 120;
        cmdBtnW = BTNW - 50;
        cmdBtnH = BTNH;

        addDrawableChild(new FaButton(
            cmdBtnX, cmdBtnY, cmdBtnW, cmdBtnH,
            Text.literal("Custom Commands").styled(style -> style.withColor(0xE2CAE9)),
            btn -> MinecraftClient.getInstance().setScreen(new TabbedListScreen(MinecraftClient.getInstance().currentScreen, TabbedListScreen.Tab.COMMANDS))
        ));

        keyBtnX = centerX - 150;
        keyBtnY = cmdBtnY;
        keyBtnW = BTNW - 50;
        keyBtnH = BTNH;

        addDrawableChild(new FaButton(
            keyBtnX, keyBtnY, keyBtnW, keyBtnH,
            Text.literal("Custom Keybinds").styled(style -> style.withColor(0xE2CAE9)),
            btn -> MinecraftClient.getInstance().setScreen(new TabbedListScreen(MinecraftClient.getInstance().currentScreen, TabbedListScreen.Tab.KEYBINDS))
        ));

        chatBtnX = centerX;
        chatBtnY = cmdBtnY;
        chatBtnW = BTNW - 50;
        chatBtnH = BTNH;

        addDrawableChild(new FaButton(
            chatBtnX, chatBtnY, chatBtnW, chatBtnH,
            Text.literal("Modify Chat").styled(style -> style.withColor(0xE2CAE9)),
            btn -> MinecraftClient.getInstance().setScreen(new TabbedListScreen(MinecraftClient.getInstance().currentScreen, TabbedListScreen.Tab.CHAT))
        ));

        alertBtnX = centerX + 150; 
        alertBtnY = cmdBtnY;
        alertBtnW = BTNW - 50;
        alertBtnH = BTNH;

        addDrawableChild(new FaButton(
            alertBtnX, alertBtnY, alertBtnW, alertBtnH,
            Text.literal("Chat Alerts").styled(style -> style.withColor(0xE2CAE9)),
            btn -> MinecraftClient.getInstance().setScreen(new TabbedListScreen(MinecraftClient.getInstance().currentScreen, TabbedListScreen.Tab.ALERT))
        ));

        rndrBtnX = centerX - 100;
        rndrBtnY = centerY - 70;
        rndrBtnW = BTNW;
        rndrBtnH = BTNH;

        addDrawableChild(new FaButton(
            rndrBtnX, rndrBtnY, rndrBtnW, rndrBtnH,
            getRndrText(),
            btn -> {
                FishyConfig.toggle(Key.RENDER_COORDS, false);
                btn.setMessage(getRndrText());
            }
        ));

        addDrawableChild(new FaButton(
            centerX + 100, rndrBtnY, 60, BTNH,
            getColorButtonText(),
            btn -> {
                MinecraftClient.getInstance().setScreen(new ColorWheel(this,
                    ColorWheel.intToRGB(FishyConfig.getInt("renderCoordsColor")),
                    rgb -> {
                        FishyConfig.setInt("renderCoordsColor", ColorWheel.rgbToInt(rgb));
                        btn.setMessage(getColorButtonText());
                    }
                ));
            }
        ));

        addDrawableChild(new FaButton(
            centerX - 100, centerY - 40, BTNW, BTNH,
            getPingHudText(),
            btn -> {
                FishyConfig.toggle(Key.HUD_PING_ENABLED, false);
                btn.setMessage(getPingHudText());
            }
        ));

        addDrawableChild(new FaButton(
            centerX + 100, centerY - 40, 60, BTNH,
            Text.literal("Edit Hud").setStyle(Style.EMPTY.withColor(0xFF808080)),
            btn -> MinecraftClient.getInstance().setScreen(new HudEditScreen())
        ));

        copyBtnX = centerX - 100;
        copyBtnY = centerY - 10;
        copyBtnW = BTNW;
        copyBtnH = BTNH;

        addDrawableChild(new FaButton(
            copyBtnX, copyBtnY, copyBtnW, copyBtnH,
            getCopyText(),
            btn -> {
                FishyConfig.toggle(Key.COPY_CHAT, true);
                btn.setMessage(getCopyText());
                CopyChat.refresh();
            }
        ));
        
        addDrawableChild(new FaButton(
            copyBtnX + BTNW, copyBtnY, 60, BTNH,
            getCopyNotiText(),
            btn -> {
                FishyConfig.toggle(Key.COPY_NOTI, true);
                btn.setMessage(getCopyNotiText());
                CopyChat.refresh();
            }
        ));    
        
        invBtnX = centerX - 100;
        invBtnY = centerY + 20;
        invBtnW = 150;
        invBtnH = BTNH;
        
        addDrawableChild(new FaButton(
            invBtnX, invBtnY, invBtnW, invBtnH,
            getInvSearchText(),
            btn -> {
                ItemSearchOverlay overlay = ItemSearchOverlay.getInstance();
                boolean newState = !overlay.isEnabled();
                overlay.setEnabled(newState);
                btn.setMessage(getInvSearchText());
            }
        ));
        
        float currentOpacity = ItemSearchOverlay.getInstance().getOpacity();
        opacitySlider = new CompactSlider(invBtnX + invBtnW + 10, invBtnY + 4, currentOpacity, opacity -> 
            ItemSearchOverlay.getInstance().setOpacity(opacity)
        );

        f5BtnX = centerX - 100;
        f5BtnY = centerY + 50;
        f5BtnW = BTNW;  
        f5BtnH = BTNH;

        addDrawableChild(new FaButton(
            f5BtnX, f5BtnY, f5BtnW, f5BtnH,
            getSkipPerspectiveText(),
            btn -> {
                FishyConfig.toggle(Key.SKIP_F5, false);
                btn.setMessage(getSkipPerspectiveText());
            }
        ));

        hypBtnX = centerX - 100;
        hypBtnY = centerY + 80;
        hypBtnW = BTNW;
        hypBtnH = BTNH;

        addDrawableChild(new FaButton(
            hypBtnX, hypBtnY, hypBtnW, hypBtnH,
            getHypText(),
            btn -> {
                FishyConfig.toggle(Key.CLEAN_HYPE, false);
                btn.setMessage(getHypText());
                SkyblockCleaner.refresh();
            }
        ));        

        addDrawableChild(new FaButton(
            centerX - 100, centerY + 100, BTNW, BTNH,
            getDeathText(),
            btn -> {
                FishyConfig.toggle("deathAni", false);
                MobAnimations.refresh();
                btn.setMessage(getDeathText());
            }
        ));

        addDrawableChild(new FaButton(
            centerX - 100, centerY + 120, BTNW, BTNH,
            getFireText(),
            btn -> {
                FishyConfig.toggle("fireAni", false);
                MobAnimations.refresh();
                btn.setMessage(getFireText());
            }
        ));

        addDrawableChild(new FaButton(
            centerX - 100, centerY + 190, 80, BTNH,
            Text.literal("Back").setStyle(Style.EMPTY.withColor(0xFF808080)),
            btn -> MinecraftClient.getInstance().setScreen(new FishyAddonsScreen())
        ));

        addDrawableChild(new FaButton(
            centerX + 20, centerY + 190, 80, BTNH,
            Text.literal("Close").setStyle(Style.EMPTY.withColor(0xFF808080)),
            btn -> MinecraftClient.getInstance().setScreen(null)
        ));
    }

    private Text getRndrText() {
        return GuiUtil.onOffLabel("Highlight Coordinates", FishyConfig.getState(Key.RENDER_COORDS, false));
    }  
    
    private Text getPingHudText() {
        return GuiUtil.onOffLabel("Ping Display", FishyConfig.getState(Key.HUD_PING_ENABLED, false));
    }

    private Text getSkipPerspectiveText() {
        return GuiUtil.onOffLabel("Skip Front Perspective", FishyConfig.getState(Key.SKIP_F5, false));
    }

    private Text getHypText() {
        return GuiUtil.onOffLabel("Clean Wither Impact", FishyConfig.getState(Key.CLEAN_HYPE, false));
    }

    private Text getCopyText() {
        return GuiUtil.onOffLabel("Copy Chat", FishyConfig.getState(Key.COPY_CHAT, true));
    } 
    
    private Text getCopyNotiText() {
        return GuiUtil.onOffLabel("Noti", FishyConfig.getState(Key.COPY_NOTI, true));
    }

    private Text getDeathText() {
        return GuiUtil.onOffLabel("Skip Mob Death Animation", FishyConfig.getState("deathAni", false));
    }

    private Text getFireText() {
        return GuiUtil.onOffLabel("Skip Mob Fire Animation", FishyConfig.getState("fireAni", false));
    }

    private static Text getInvSearchText() {
        return GuiUtil.onOffLabel("Inventory Search", FishyConfig.getState(Key.INV_SEARCH, false));
    }

    private static Text getColorButtonText() {
        float[] rgb = ColorWheel.intToRGB(FishyConfig.getInt("renderCoordsColor"));
        int r = (int)(rgb[0] * 255);
        int g = (int)(rgb[1] * 255);
        int b = (int)(rgb[2] * 255);
        int color = (0xFF << 24) | (r << 16) | (g << 8) | b;
        return Text.literal("Color").styled(style -> style.withColor(color));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, "General Qol", this.width / 2, this.height / 2 - 150, 0xFF55FFFF);
        
        super.render(context, mouseX, mouseY, delta);

        if (opacitySlider != null) {
            opacitySlider.render(context, mouseX, mouseY);
            String percentageText = opacitySlider.getPercentageText();
            context.drawText(this.textRenderer, percentageText, 
                opacitySlider.getX() + CompactSlider.getWidth() + 5, 
                opacitySlider.getY() + 2, 0xFFFFFFFF, false);
        }    

        if (mouseX >= cmdBtnX && mouseX <= cmdBtnX + cmdBtnW && mouseY >= cmdBtnY && mouseY <= cmdBtnY + cmdBtnH) {
            List<Text> tooltipLines = Arrays.asList(
                Text.literal("Command Aliases:"),
                Text.literal("- §8Create custom aliases"),
                Text.literal("  §8for existing commands"),
                Text.literal("- §8/fa cmd add | on | off")
            );
            GuiUtil.fishyTooltip(context, this.textRenderer, tooltipLines, mouseX, mouseY);
        }

        if (mouseX >= keyBtnX && mouseX <= keyBtnX + keyBtnW && mouseY >= keyBtnY && mouseY <= keyBtnY + keyBtnH) {
            List<Text> tooltipLines = Arrays.asList(
                Text.literal("Custom Keybinds:"),
                Text.literal("- §8Create custom keybinds"),
                Text.literal("  §8for existing commands"),
                Text.literal("- §8/fa key add | on | off")
            );
            GuiUtil.fishyTooltip(context, this.textRenderer, tooltipLines, mouseX, mouseY);
        }

        if (mouseX >= chatBtnX && mouseX <= chatBtnX + chatBtnW && mouseY >= chatBtnY && mouseY <= chatBtnY + chatBtnH) {
            List<Text> tooltipLines = Arrays.asList(
                Text.literal("Modify Chat:"),
                Text.literal("- §8Customize chat messages"),
                Text.literal("  §8and formatting"),
                Text.literal("- §8/fa chat add | on | off")
            );
            GuiUtil.fishyTooltip(context, this.textRenderer, tooltipLines, mouseX, mouseY);
        }

        if (mouseX >= alertBtnX && mouseX <= alertBtnX + alertBtnW && mouseY >= alertBtnY && mouseY <= alertBtnY + alertBtnH) {
            List<Text> tooltipLines = Arrays.asList(
                Text.literal("Chat Alerts:"),
                Text.literal("- §8Set alerts for specific"),
                Text.literal("  §8keywords in chat"),
                Text.literal("- §8/fa alert add | on | off")
            );
            GuiUtil.fishyTooltip(context, this.textRenderer, tooltipLines, mouseX, mouseY);
        }

        if (mouseX >= invBtnX && mouseX <= invBtnX + invBtnW && mouseY >= invBtnY && mouseY <= invBtnY + invBtnH) {
            List<Text> tooltipLines = Arrays.asList(
                Text.literal("Inventory Search:"),
                Text.literal("- §8Search your inventory"),
                Text.literal("  §8for items by name/lore"),
                Text.literal("- §8Right click the textfield"),
                Text.literal("  §8to toggle search mode")
            );
            GuiUtil.fishyTooltip(context, this.textRenderer, tooltipLines, mouseX, mouseY);
        }

        // Add tooltip for opacity slider
        if (opacitySlider != null && opacitySlider.isMouseOver(mouseX, mouseY)) {
            List<Text> tooltipLines = Arrays.asList(
                Text.literal("Search Overlay Opacity:"),
                Text.literal("- §8Adjust the darkness of"),
                Text.literal("  §8the search overlay"),
                Text.literal("- §8" + opacitySlider.getPercentageText() + " opacity")
            );
            GuiUtil.fishyTooltip(context, this.textRenderer, tooltipLines, mouseX, mouseY);
        }

        if (mouseX >= f5BtnX && mouseX <= f5BtnX + f5BtnW && mouseY >= f5BtnY && mouseY <= f5BtnY + f5BtnH) {
            List<Text> tooltipLines = Arrays.asList(
                Text.literal("Custom F5/Camera:"),
                Text.literal("- §8Skip front perspective"),
                Text.literal("  §8/fa camera on | off")
            );
            GuiUtil.fishyTooltip(context, this.textRenderer, tooltipLines, mouseX, mouseY);
        }

        if (mouseX >= hypBtnX && mouseX <= hypBtnX + hypBtnW && mouseY >= hypBtnY && mouseY <= hypBtnY + hypBtnH) {
            List<Text> tooltipLines = Arrays.asList(
                Text.literal("Wither Impact:"),
                Text.literal("- §8Remove Explosion particles"),
                Text.literal("  §8and sound effects")
            );
            GuiUtil.fishyTooltip(context, this.textRenderer, tooltipLines, mouseX, mouseY);
        }

        if (mouseX >= rndrBtnX && mouseX <= rndrBtnX + rndrBtnW && mouseY >= rndrBtnY && mouseY <= rndrBtnY + rndrBtnH) {
            List<Text> tooltipLines = Arrays.asList(
                Text.literal("Render a Beacon for /fa coords:"),
                Text.literal("- §8Supports other mods if formatted"),
                Text.literal("  §8as 'x:%, y:%, z:%'"),
                Text.literal("- §8Disappears after 1min"),
                Text.literal("- §8/fa coords [title] to include additional text")
            );
            GuiUtil.fishyTooltip(context, this.textRenderer, tooltipLines, mouseX, mouseY);
        }

        if (mouseX >= copyBtnX && mouseX <= copyBtnX + copyBtnW && mouseY >= copyBtnY && mouseY <= copyBtnY + copyBtnH) {
            List<Text> tooltipLines = Arrays.asList(
                Text.literal("Copy Chat:"),
                Text.literal("- §8Right-click to copy chat messages"),
                Text.literal("  §8to clipboard"),
                Text.literal("- §8Hold shift to copy just the hovered line")
            );
            GuiUtil.fishyTooltip(context, this.textRenderer, tooltipLines, mouseX, mouseY);
        }
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (opacitySlider != null && opacitySlider.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (opacitySlider != null && opacitySlider.mouseReleased(button)) {
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }
    
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (opacitySlider != null && opacitySlider.mouseDragged(mouseX, button)) {
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }
}