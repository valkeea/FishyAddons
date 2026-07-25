package me.valkeea.fishyaddons.ui.screen;

import java.util.List;

import org.lwjgl.glfw.GLFW;

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

public class AlertEditScreen extends Screen {
    private final Screen parent;
    private final AlertData initialData;
    private static final String COLOR = "Color";
    private String alertKey;

    private SoundSearchMenu searchMenu;
    private TextFormatMenu formatMenu;
    private VCTextField msgField;
    private VCTextField alertTextField;
    private VCTextField soundIdField;
    private VCSlider volumeSlider;
    private VCTextField keyField;
    private int alertColor = 0xFF6DE6B5;
    private boolean alertStartsWith = false;
    private VCTextField lastFocusedField = null; 

    private String stateKey = null;
    private String stateMsg = null;
    private String stateOnscreen = null;
    private String stateSoundId = null;
    private Float stateVolume = null; 
    
    private static String prefer(String... values) {
        for (String v : values) {
            if (v != null) return v;
        }
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
        super(Component.literal("Edit Alert"));
        this.parent = ScreenManager.getConfigOrCurrent();
        this.alertKey = key;
        this.initialData = data != null ? data : new AlertData("", "", 0xFFFFFFFF, "", 1.0F, true, false);
        this.alertColor = this.initialData.getColor();
    }

    @Override
    protected void init() {
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
        volumeSlider.setUIScale(1.0f);

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
            focusedField.setFocused(true);
        }
        
        if (focusedField != null) {
            apply(focusedField, format);
        }
        formatMenu.setVisible(false);
    }

    private void apply(VCTextField field, String format) {
        String currentText = field.getValue();
        int caretPos = field.getCursorPosition();    
        String newText = currentText.substring(0, caretPos) + format + currentText.substring(caretPos);
        field.setValue(newText);
        field.moveCursorTo(caretPos + format.length(), false);
        field.setFocused(true);
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
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractRenderState(context, mouseX, mouseY, delta);
        renderGuideText(context);

        if (searchMenu != null) {
            searchMenu.setVisible(soundIdField.isFocused() && !soundIdField.getValue().isEmpty());
            if (searchMenu.isVisible()) {
                searchMenu.render(context, this, mouseX, mouseY, delta);
            }
        }

        int centerX = this.width / 2;
        int centerY = this.height / 2 - 10;
        int x = centerX - 380;
        int y = centerY - 90;
        int w = 300;

        context.text(this.font, "Detected String", x, y, 0xFF808080, false);
        context.text(this.font, "Location", x + w + 10, y, 0xFF808080, false);
        context.text(this.font, "Auto Chat", x, y + 40, 0xFF808080, false);
        context.text(this.font, "On-screen Title", x, y + 80, 0xFF808080, false);
        context.text(this.font, COLOR, x + w + 10, y + 80, 0xFF808080, false);
        context.text(this.font, "SoundEvent ID", x, y + 120, 0xFF808080, false);
        
        if (volumeSlider != null) {
            String volumeLabel = "Volume (" + volumeSlider.getPercentageLabel() + ")";
            context.text(this.font, volumeLabel, x + w + 10, y + 120, 0xFF808080, false);
            volumeSlider.render(context, this.font, mouseX, mouseY, VCVisuals.getThemeColor());
        }

        if (formatMenu != null && formatMenu.isVisible()) {
            context.nextStratum();
            context.pose().pushMatrix();
            context.pose().translate(0, 0);
            formatMenu.render(context, this, mouseX, mouseY);
            context.pose().popMatrix();
        }

        checkTooltip(context, mouseX, mouseY);        
    }

    private void checkTooltip(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        if (alertTextField != null && alertTextField.isMouseOver(mouseX, mouseY)) {

            String previewText = alertTextField.getValue().trim();
            if (!previewText.isEmpty()) {

                try {
                    Component formattedPreview = Enhancer.parseFormattedText(previewText);
                    int tooltipWidth = Math.min(400, this.font.width(formattedPreview) + 20);
                    int tooltipHeight = alertTextField.getHeight();
                    int tooltipX = alertTextField.getX();
                    int tooltipY = alertTextField.getY() + tooltipHeight + 5;

                    if (tooltipX + tooltipWidth > this.width) {
                        tooltipX = mouseX - tooltipWidth - 10;
                    }
                    if (tooltipY < 0) {
                        tooltipY = mouseY + 20;
                    }

                    context.nextStratum();
                    context.fill(tooltipX, tooltipY, 
                               tooltipX + tooltipWidth + 6, tooltipY + tooltipHeight + 4, 
                               0xFF171717);
                    
                    context.text(this.font, formattedPreview, 
                                   tooltipX + 3, tooltipY + tooltipHeight / 3, 0xFFFFFFFF, true);
                                   
                } catch (Exception e) {
                    System.err.println("[FishyAddons] Error rendering tooltip: " + e.getMessage());
                    renderFallback(mouseX, mouseY, context);
                }
            }
        }
    }

    private void renderFallback(int mouseX, int mouseY, GuiGraphicsExtractor context) {
        String errorText = "Error rendering preview";
        int tooltipWidth = Math.min(400, this.font.width(errorText) + 20);
        int tooltipHeight = 20;
        int tooltipX = mouseX + 10;
        int tooltipY = mouseY - tooltipHeight - 10;
        if (tooltipX + tooltipWidth > this.width) {
            tooltipX = mouseX - tooltipWidth - 10;
        }
        if (tooltipY < 0) {
            tooltipY = mouseY + 20;
        }
                    
        context.fill(tooltipX - 5, tooltipY - 5, 
                    tooltipX + tooltipWidth + 5, tooltipY + tooltipHeight + 5, 
                    0xFF171717);
        context.text(this.font, Component.literal(errorText), 
                    tooltipX + 5, tooltipY + 10, 0xFFFF8080, true);
    }    

    public void renderGuideText(GuiGraphicsExtractor context) {       
        int x = this.width / 2 + 30;
        int y = 30;
        int lineHeight = 15;

        Component title = VCText.header("FishyAddons Custom Alerts", Style.EMPTY.withBold(true));
        context.text(this.font, title, x, y, 0xFFFFFFFF);            

        y += lineHeight * 2;
                
        String[] instructions = {
            " The First field is required to successfully create an alert!",
            " Leaving other fields empty will disable those functions.",
            "",
            "- Detected String -",
            " • §7Matched anywhere in the message",
            "   §7or, only from start if 'Starts With' is selected",
            " • §7With Chat Filter enabled, use the §doriginal message",
            "   as a trigger to not have to update alerts if you change your config",
            "",
            "- Auto Chat -",
            " • §7Sends a pchat message if you are currently in a party",
            " • §b<pos> §7can be used to insert your current coordinates!",
            " • §7 Commands (start with '/') will be sent even if not in a party",
            "",
            "- On-screen Title -",
            " • §7Lasts for 2 seconds, position and size can be customized in /fa hud",
            " • §7You can choose a color or use mod / legacy formatting codes",
            "",
            "- SoundEvent -",
            " • §7Triggers a Minecraft SoundEvent when the alert is triggered",
            " • §7You can preview sounds by right-clicking",
            "   them in the dropdown",
            " • §7Volume is affected by internal settings",
            "   §3Note: §8FA provides 3 custom events with placeholder sounds.",
            "   §3Guide to replace them: §8/fa sc sounds",
            "",
            " • §7Alerts can be loaded from JSON in the main list UI if you want to share!"
        };

        for (String instruction : instructions) {
            if (instruction.isEmpty()) {
                y += lineHeight / 2;
                continue;
            }

            var format = ChatFormatting.GRAY;
            if (instruction.startsWith(" •") || instruction.matches("\\d+\\..*")) {
                format = ChatFormatting.AQUA;
            } else if (instruction.contains("-")) {
                format = ChatFormatting.DARK_AQUA;
            } else if (instruction.startsWith(" The")) {
                format = ChatFormatting.DARK_GRAY;
            }
    
            var text = Component.literal(instruction).withStyle(format);
            context.text(this.font, text, x, y, 0xFFFFFFFF);
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
    
    private boolean handleMenu(MouseButtonEvent click) {
        if (formatMenu != null && formatMenu.isVisible()) {
            if (formatMenu.mouseClicked(click)) {
                return true;
            }
            formatMenu.setVisible(false);
            return false;
        }
        return false;
    }    

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {

        double mouseX = click.x();
        double mouseY = click.y();

        if (handleMenu(click)) {
            return true;
        }

        // Handle volume slider clicks
        if (volumeSlider != null && volumeSlider.mouseClicked(click)) {
            return true;
        }

        boolean clickedField = false;
        for (var field : List.of(msgField, alertTextField, soundIdField, keyField)) {
            if (field.isMouseOver(mouseX, mouseY)) {
                lastFocusedField = field;
                clickedField = true;
                toggleFormatMenu(true);
                break;
            }
        }

        if (!clickedField) {
            toggleFormatMenu(false);
        }

        if (searchMenu != null && searchMenu.mouseClicked(click)) {
            return true;
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean keyPressed(KeyEvent input) {
        if (searchMenu != null && soundIdField.isFocused() && searchMenu.keyPressed(input)) {
            return true;
        }

        if (input.key() == GLFW.GLFW_KEY_F3) {
            toggleFormatMenu(true);
            return true;
        }        

        return super.keyPressed(input);
    }     

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (searchMenu != null && searchMenu.mouseScrolled(verticalAmount)) {
            return true;
        }
        if (formatMenu != null && formatMenu.isVisible()) {
            return formatMenu.mouseScrolled(verticalAmount);
        }        
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent click, double offsetX, double offsetY) {
        if (volumeSlider != null && volumeSlider.mouseDragged(click)) {
            return true;
        }

        if (formatMenu != null && formatMenu.isVisible() && formatMenu.mouseDragged(click)) {
            return true;
        }
        return super.mouseDragged(click, offsetX, offsetY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent click) {
        if (volumeSlider != null && volumeSlider.mouseReleased(click)) {
            return true;
        }
        return super.mouseReleased(click);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        McApi.setScreen(parent);
    }
}
