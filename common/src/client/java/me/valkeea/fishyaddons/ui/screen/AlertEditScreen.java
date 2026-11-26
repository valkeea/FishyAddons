package me.valkeea.fishyaddons.ui.screen;

import java.util.List;

import com.mojang.blaze3d.platform.InputConstants;

import me.valkeea.fishyaddons.compat.McApi;
import me.valkeea.fishyaddons.feature.qol.ChatAlert;
import me.valkeea.fishyaddons.tool.PlaySound;
import me.valkeea.fishyaddons.ui.element.SoundSearchMenu;
import me.valkeea.fishyaddons.ui.element.TextFormatMenu;
import me.valkeea.fishyaddons.util.text.Enhancer;
import me.valkeea.fishyaddons.vconfig.api.BooleanKey;
import me.valkeea.fishyaddons.vconfig.config.impl.AlertConfig;
import me.valkeea.fishyaddons.vconfig.config.impl.AlertConfig.AlertData;
import me.valkeea.fishyaddons.vconfig.ui.manager.ScreenManager;
import me.valkeea.fishyaddons.vconfig.ui.render.VCText;
import me.valkeea.fishyaddons.vconfig.ui.screen.AdjustedScreen;
import me.valkeea.fishyaddons.vconfig.ui.screen.ColorWheel;
import me.valkeea.fishyaddons.vconfig.ui.screen.HudEditScreen;
import me.valkeea.fishyaddons.vconfig.ui.widget.FaButton;
import me.valkeea.fishyaddons.vconfig.ui.widget.VCPopup;
import me.valkeea.fishyaddons.vconfig.ui.widget.VCSlider;
import me.valkeea.fishyaddons.vconfig.ui.widget.VCTextField;
import me.valkeea.fishyaddons.vconfig.ui.widget.VCVisuals;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;

public class AlertEditScreen extends AdjustedScreen {
    private final Screen parent;
    private final AlertData initialData;
    private static final String COLOR = "Color";
    private final String alertKey;

    private SoundSearchMenu searchMenu;
    private TextFormatMenu formatMenu;
    private VCTextField msgField;
    private VCTextField alertTextField;
    private VCTextField soundIdField;
    private VCSlider volumeSlider;
    private VCTextField keyField;
    private VCTextField lastFocusedField = null; 

    private int alertColor = 0xFF6DE6B5;

    private boolean alertStartsWith;
    private String stateKey = null;
    private String stateMsg = null;
    private String stateOnscreen = null;
    private String stateSoundId = null;
    private Float stateVolume = null; 
    
    private static String prefer(String... values) {
        for (String v : values) if (v != null) return v;
        return "";
    }    

    private void storeState() {
        stateKey = keyField.getValue();
        stateMsg = msgField.getValue();
        stateOnscreen = alertTextField.getValue();
        stateSoundId = soundIdField.getValue();
        stateVolume = volumeSlider != null ? (float) volumeSlider.getValue() : null;
    }

    public AlertEditScreen(String key, AlertData data) {
        super(VCText.header(
            "FishyAddons Custom Alerts",
            Style.EMPTY.withBold(true))
        );
        this.parent = ScreenManager.getConfigOrCurrent();
        this.alertKey = key;
        this.initialData = data != null ? data : new AlertData("", "", 0xFFFFFFFF, "", 1.0F, true, false);
        this.alertColor = this.initialData.getColor();
        this.alertStartsWith = this.initialData.isStartsWith();
    }

    @Override
    protected void init() {
        super.init();
        int centerX = this.width / 2;
        int centerY = this.height / 2 - 10;

        int x = centerX - 380;
        int y = centerY - 80;
        int w = 300;
        int h = 20;

        keyField = new VCTextField(this.font, x, y, w, h, Component.literal("Key"));
        keyField.setMaxLength(100);
        keyField.setValue(prefer(stateKey, alertKey));
        this.addRenderableWidget(keyField);

        addRenderableWidget(new FaButton(x + w, y, 80, h, 
            Component.literal(initialData.isStartsWith() ? "Starts With" : "Anywhere").withStyle(style -> 
                style.withColor(initialData.isStartsWith() ? alertColor : 0xFF808080)
            ), btn -> { 
                boolean newMode= !initialData.isStartsWith();
                btn.setMessage(Component.literal(newMode ? "Starts With" : "Anywhere")
                    .withStyle(s -> s.withColor(newMode ? alertColor : 0xFF808080))
                );
                initialData.setStartsWith(newMode);
                alertStartsWith = newMode;
                storeState();
                McApi.setScreen(this);
            }
        ));

        msgField = new VCTextField(this.font, x, y + 40, w, h, Component.literal("Chat message"));
        msgField.setValue(prefer(stateMsg, initialData.getMsg()));
        msgField.setSectionSymbol(false);
        this.addRenderableWidget(msgField);

        alertTextField = new VCTextField(this.font, x, y + 80, w, h, Component.literal("On-screen Alert"));
        alertTextField.setValue(prefer(stateOnscreen, initialData.getOnscreen()));
        this.addRenderableWidget(alertTextField);

        addRenderableWidget(new FaButton(x + w, centerY, 50, h, 
            Component.literal(COLOR).withStyle(style -> style.withColor(alertColor)),
            btn -> { 
                storeState();
                McApi.setScreen(new ColorWheel(this, alertColor, selected -> {
                    alertColor = selected;
                    btn.setMessage(Component.literal(COLOR).withStyle(s -> s.withColor(alertColor)));
                    McApi.setScreen(this);
                }));
            }
        ));

        soundIdField = new VCTextField(this.font, x, y + 120, w, h, Component.literal("SoundEvent ID"));
        soundIdField.setValue(prefer(stateSoundId, initialData.getSoundId()));
        this.addRenderableWidget(soundIdField);

        List<String> soundIds = BuiltInRegistries.SOUND_EVENT.stream()
            .map(BuiltInRegistries.SOUND_EVENT::getKey)
            .filter(java.util.Objects::nonNull)
            .map(Identifier::toString)
            .sorted()
            .toList();

        int sx = soundIdField.getX();
        int sy = soundIdField.getY();
        int width = soundIdField.getWidth();
        int entryHeight = 18;

        searchMenu = new SoundSearchMenu(
            soundIds,
            sx, sy, width, entryHeight,
            soundId -> {
                soundIdField.setValue(soundId);
                searchMenu.setVisible(false);
            },
            soundId -> PlaySound.dynamic(soundId, volumeSlider != null ? (float) volumeSlider.getValue() : 1.0F, 1.0F, false),
            this,
            soundIdField
        );

        soundIdField.setResponder(
            query -> searchMenu.setVisible(soundIdField.isFocused() && !query.isEmpty())
        );

        searchMenu.setVisible(!soundIdField.getValue().isEmpty());

        float initialVolume;
        if (stateVolume != null) {
            initialVolume = stateVolume;
        } else {
            initialVolume = initialData.getVolume();
        }
        
        volumeSlider = new VCSlider(x + width, sy, initialVolume, 0.0f, 1.0f, "%.0f%%", value -> {});
        formatMenu = new TextFormatMenu(
            this.width / 2 + 50, 30, w,
            this::insertAtCaret,
            1.0f
        );
        formatMenu.setMaxEntries((this.height - 50) / h);     

        this.addRenderableWidget(new FaButton(x + w / 2 + 80, soundIdField.getY() + 80, 80, 20,
            Component.literal("HUD").withStyle(style -> style.withColor(0xFFE2CAE9)),
            btn -> {
                McApi.setScreen(new HudEditScreen(BooleanKey.HUD_TITLE_ENABLED, this));
                save();
            }
        ));

        this.addRenderableWidget(new FaButton(x + w / 2, soundIdField.getY() + 80, 80, 20,
            Component.literal("Save").withStyle(style -> style.withColor(0xFFE2CAE9)),
            btn -> {
            save();
            onClose();
        }));

        this.addRenderableWidget(new FaButton(x + w / 2 - 80, soundIdField.getY() + 80, 80, 20,
            Component.literal("Cancel").withStyle(style -> style.withColor(0xFFE2CAE9)),
            btn -> onClose()
        ));
    }

    private void insertAtCaret(String format) {
        VCTextField focusedField = null;
        if (msgField.isFocused()) {
            focusedField = msgField;
        } else if (alertTextField.isFocused()) {
            focusedField = alertTextField;
        }
        
        if (focusedField == null && lastFocusedField != null) {
            focusedField = lastFocusedField;
            this.setFocused(focusedField);
        }
        
        if (focusedField != null) apply(focusedField, format);
        formatMenu.setVisible(false);
    }

    private void apply(VCTextField field, String format) {
        String currentText = field.getValue();
        int caretPos = field.getCursorPosition();    
        String newText = currentText.substring(0, caretPos) + format + currentText.substring(caretPos);
        field.setValue(newText);
        field.moveCursorTo(caretPos + format.length(), false);
        this.setFocused(field);
    }    

    private void save() {
        float volume = volumeSlider != null ? (float) volumeSlider.getValue() : 1.0f;
            String newKey = keyField.getValue().trim();
        if (newKey.isEmpty()) {
            warn();
            return;
        }

        var newData = new AlertData(
            msgField.getValue(),
            alertTextField.getValue(),
            alertColor,
            soundIdField.getValue(),
            volume,
            true,
            alertStartsWith
        );

        if (!newKey.equals(alertKey)) {
            AlertConfig.removeChatAlert(alertKey);
        }

        AlertConfig.setChatAlert(newKey, newData);
        ChatAlert.refresh();
    }

	public void warn() {      
        var popup = new VCPopup(
            Component.literal("Empty field detected! Would you like to restore it?"),
            "No", this::onClose,
            "Yes", () -> keyField.setValue(alertKey),
            1.0f
            );
        McApi.setScreen(new Overlay(McApi.screen(), popup));
	}    

    @Override
    public void extractContent(GuiGraphicsExtractor gg, int mX, int mY, float delta) {
        super.extractContent(gg, mX, mY, delta);
        gg.blurBeforeThisStratum();
        renderGuideText(gg);

        if (searchMenu != null) {
            searchMenu.setVisible(soundIdField.isFocused() && !soundIdField.getValue().isEmpty());
            if (searchMenu.isVisible()) {
                searchMenu.render(gg, this, mX, mY, delta);
            }
        }

        int centerX = this.width / 2;
        int centerY = this.height / 2 - 10;
        int x = centerX - 380;
        int y = centerY - 90;
        int w = 300;

        gg.text(this.font, "Detected String", x, y, 0xFF808080, false);
        gg.text(this.font, "Location", x + w + 10, y, 0xFF808080, false);
        gg.text(this.font, "Auto Chat", x, y + 40, 0xFF808080, false);
        gg.text(this.font, "On-screen Title", x, y + 80, 0xFF808080, false);
        gg.text(this.font, COLOR, x + w + 10, y + 80, 0xFF808080, false);
        gg.text(this.font, "SoundEvent ID", x, y + 120, 0xFF808080, false);
        
        if (volumeSlider != null) {
            String volumeLabel = "Volume (" + volumeSlider.getPercentageLabel() + ")";
            gg.text(this.font, volumeLabel, x + w + 10, y + 120, 0xFF808080, false);
            volumeSlider.render(gg, this.font, mX, mY, VCVisuals.getThemeColor());
        }

        if (formatMenu != null && formatMenu.isVisible()) {
            gg.nextStratum();
            gg.pose().pushMatrix();
            gg.pose().translate(0, 0);
            formatMenu.render(gg, this, mX, mY);
            gg.pose().popMatrix();
        }

        checkTooltip(gg, mX, mY);        
    }

    private void checkTooltip(GuiGraphicsExtractor gg, int mX, int mY) {
        if (alertTextField != null && alertTextField.isMouseOver(mX, mY)) {

            String previewText = alertTextField.getValue().trim();
            if (!previewText.isEmpty()) {

                try {
                    var tt = Enhancer.parseFormattedText(previewText);
                    int ttW = Math.min(400, this.font.width(tt) + 20);
                    int ttH = alertTextField.getHeight();
                    int ttX = alertTextField.getX();
                    int ttY = alertTextField.getY() + ttH + 5;

                    if (ttX + ttW > this.width) {
                        ttX = mX - ttW - 10;
                    }
                    if (ttY < 0) {
                        ttY = mY + 20;
                    }

                    gg.nextStratum();
                    gg.fill(ttX, ttY, ttX + ttW + 6, ttY + ttH + 4, 0xFF171717);
                    gg.text(this.font, tt, ttX + 3, ttY + ttH / 3, 0xFFFFFFFF, true);
                                   
                } catch (Exception e) {
                    System.err.println("[FishyAddons] Error rendering tooltip: " + e.getMessage());
                    renderFallback(mX, mY, gg);
                }
            }
        }
    }

    private void renderFallback(int mX, int mY, GuiGraphicsExtractor gg) {
        String errorText = "Error rendering preview";
        int ttW = Math.min(400, this.font.width(errorText) + 20);
        int ttH = 20;
        int ttX = mX + 10;
        int ttY = mY - ttH - 10;
        if (ttX + ttW > this.width) ttX = mX - ttW - 10;
        if (ttY < 0) ttY = mY + 20;
                    
        gg.fill(ttX - 5, ttY - 5, ttX + ttW + 5, ttY + ttH + 5,  0xFF171717);
        gg.text(this.font, Component.literal(errorText), 
                    ttX + 5, ttY + 10, 0xFFFF8080, true);
    }    

    public void renderGuideText(GuiGraphicsExtractor gge) {       
        int x = this.width / 2 + 30;
        int y = 30;
        int lineHeight = 15;

        gge.text(this.font, getTitle(), x, y, 0xFFFFFFFF);            

        y += lineHeight * 2;  
        String[] instructions = {
            " The First field is required to successfully create an alert!",
            " Leaving other fields empty will disable those functions.",
            "",
            "- Detected String -",
            " • §7Matched anywhere in the message",
            "   §7or, only from start if 'Starts With' is selected",
            " • §7With Chat Filter enabled, always use the",
            "   §dunfiltered message§7 as a trigger",
            "",
            "- Auto Chat -",
            " • §7Sends a pchat message if you are currently in a party",
            " • §b<pos> §7can be used to insert your current coordinates!",
            " • §7 Commands (start with '/') will be sent even if not in a party",
            "",
            "- On-screen Title -",
            " • §7Lasts for 2 seconds, position and size can be customized in /fa hud",
            " • §7You can choose a color or use mod / vanilla formatting codes",
            "",
            "- SoundEvent -",
            " • §7Triggers a Minecraft SoundEvent when the alert is triggered",
            " • §7You can preview sounds by right-clicking",
            "   them in the dropdown",
            " • §7Volume is affected by internal settings",
            "   §3Note: §8FA provides 3 custom events with placeholder sounds.",
            "   §3Guide to replace them: §8/fa sc sounds",
            ""
        };

        for (var line : instructions) {
            if (line.isEmpty()) {
                y += lineHeight / 2;
                continue;
            }

            var format = ChatFormatting.GRAY;
            if (line.startsWith(" •") || line.matches("\\d+\\..*")) {
                format = ChatFormatting.AQUA;
            } else if (line.contains("-")) {
                format = ChatFormatting.DARK_AQUA;
            } else if (line.startsWith(" The")) {
                format = ChatFormatting.DARK_GRAY;
            }
    
            var text = Component.literal(line).withStyle(format);
            gge.text(this.font, text, x, y, 0xFFFFFFFF);
            y += lineHeight;
        }              
    }

    private void toggleFormatMenu(boolean show) {
        if (formatMenu != null) {
            boolean shouldShow = !formatMenu.isVisible() &&
            (lastFocusedField == msgField || lastFocusedField == alertTextField);
            formatMenu.setVisible(shouldShow && show);
        }
    } 

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        var adjusted = adjustClick(click);
        double mX = adjusted.x();
        double mY = adjusted.y();

        if (handleMenu(adjusted)) return true;
        if (volumeSlider != null && volumeSlider.mouseClicked(adjusted)) {
            return true;
        }

        boolean clickedField = false;
        for (var field : List.of(msgField, alertTextField, soundIdField, keyField)) {
            if (field.isMouseOver(mX, mY)) {
                lastFocusedField = field;
                clickedField = true;
                toggleFormatMenu(true);
                break;
            }
        }

        if (!clickedField) toggleFormatMenu(false);
        if (searchMenu != null && searchMenu.mouseClicked(adjusted)) {
            return true;
        }
        boolean r = super.mouseClicked(click, doubled);
        if (!r && !formatMenu.isVisible()) this.setFocused(null);
        return r;
    }

    private boolean handleMenu(MouseButtonEvent adjusted) {
        if (formatMenu != null && formatMenu.isVisible()) {
            if (formatMenu.mouseClicked(adjusted)) {
                return true;
            }
            formatMenu.setVisible(false);
            return false;
        }
        return false;
    }       

    @Override
    public boolean keyPressed(KeyEvent input) {
        if (searchMenu != null && soundIdField.isFocused() && searchMenu.keyPressed(input)) {
            return true;
        }
        if (input.key() == InputConstants.KEY_F3) {
            toggleFormatMenu(true);
            return true;
        }
        return super.keyPressed(input);
    }     

    @Override
    public boolean mouseScrolled(double mX, double mY, double h, double v) {
        if (searchMenu != null && searchMenu.mouseScrolled(v)) {
            return true;
        }
        if (formatMenu != null && formatMenu.isVisible()) {
            return formatMenu.mouseScrolled(v);
        }        
        return super.mouseScrolled(mX, mY, h, v);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent click, double h, double v) {
        var adjusted = adjustClick(click);
        if (volumeSlider != null && volumeSlider.mouseDragged(adjusted)) return true;
        if (formatMenu != null && formatMenu.isVisible() && formatMenu.mouseDragged(adjusted)) {
            return true;
        }
        return super.mouseDragged(click, h, v);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent click) {
        if (volumeSlider != null && volumeSlider.mouseReleased(adjustClick(click))) {
            return true;
        }
        return super.mouseReleased(click);
    }

    @Override
    public void onClose() {
        McApi.setScreen(parent);
    }
}
