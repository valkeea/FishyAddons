package me.valkeea.fishyaddons.ui.list;

import java.util.List;

import me.valkeea.fishyaddons.config.FishyConfig;
import me.valkeea.fishyaddons.feature.qol.ChatAlert;
import me.valkeea.fishyaddons.tool.PlaySound;
import me.valkeea.fishyaddons.ui.ColorWheel;
import me.valkeea.fishyaddons.ui.HudEditScreen;
import me.valkeea.fishyaddons.ui.VCOverlay;
import me.valkeea.fishyaddons.ui.VCPopup;
import me.valkeea.fishyaddons.ui.VCText;
import me.valkeea.fishyaddons.ui.widget.FaButton;
import me.valkeea.fishyaddons.ui.widget.VCSlider;
import me.valkeea.fishyaddons.ui.widget.VCTextField;
import me.valkeea.fishyaddons.ui.widget.dropdown.SoundSearchMenu;
import me.valkeea.fishyaddons.ui.widget.dropdown.TextFormatMenu;
import me.valkeea.fishyaddons.util.text.Enhancer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.registry.Registries;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

public class AlertEditScreen extends Screen {
    private final Screen parent;
    private final FishyConfig.AlertData initialData;
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
        stateKey = keyField.getText();
        stateMsg = msgField.getText();
        stateOnscreen = alertTextField.getText();
        stateSoundId = soundIdField.getText();
        stateVolume = volumeSlider != null ? volumeSlider.getValue() : null;
    }

    public AlertEditScreen(String key, FishyConfig.AlertData data, Screen parent) {
        super(Text.literal("Edit Alert"));
        this.parent = parent;
        this.alertKey = key;
        this.initialData = data != null ? data : new FishyConfig.AlertData("", "", 0xFFFFFFFF, "", 1.0F, true, false);
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

        keyField = new VCTextField(this.textRenderer, x, y, w, h, Text.literal("Key"));
        keyField.setMaxLength(100);
        keyField.setText(prefer(stateKey, alertKey));
        this.addDrawableChild(keyField);

        addDrawableChild(new FaButton(x + w, y, 80, h, 
            Text.literal(initialData.isStartsWith() ? "Starts With" : "Anywhere").styled(style -> 
                style.withColor(initialData.isStartsWith() ? alertColor : 0xFF808080)
            ), btn -> { 
                boolean newMode= !initialData.isStartsWith();
                btn.setMessage(Text.literal(newMode ? "Starts With" : "Anywhere")
                    .styled(s -> s.withColor(newMode ? alertColor : 0xFF808080))
                );
                initialData.setStartsWith(newMode);
                alertStartsWith = newMode;
                storeState();
                this.client.setScreen(this);
            }
        ));

        msgField = new VCTextField(this.textRenderer, x, y + 40, w, h, Text.literal("Chat message"));
        msgField.setText(prefer(stateMsg, initialData.getMsg()));
        msgField.setSectionSymbol(false);
        this.addDrawableChild(msgField);

        alertTextField = new VCTextField(this.textRenderer, x, y + 80, w, h, Text.literal("On-screen Alert"));
        alertTextField.setText(prefer(stateOnscreen, initialData.getOnscreen()));
        this.addDrawableChild(alertTextField);

        addDrawableChild(new FaButton(x + w, centerY, 50, h, 
            Text.literal(COLOR).styled(style -> style.withColor(alertColor)),
            btn -> { 
                storeState();
                this.client.setScreen(new ColorWheel(this, alertColor, selected -> {
                    alertColor = selected;
                    btn.setMessage(Text.literal(COLOR).styled(s -> s.withColor(alertColor)));
                    ChatAlert.refresh();
                    this.client.setScreen(this);
                }));
            }
        ));

        soundIdField = new VCTextField(this.textRenderer, x, y + 120, w, h, Text.literal("SoundEvent ID"));
        soundIdField.setText(prefer(stateSoundId, initialData.getSoundId()));
        this.addDrawableChild(soundIdField);

        List<String> soundIds = Registries.SOUND_EVENT.stream()
            .map(Registries.SOUND_EVENT::getId)
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
                soundIdField.setText(soundId);
                searchMenu.setVisible(false);
            },
            soundId -> PlaySound.dynamic(soundId, volumeSlider != null ? volumeSlider.getValue() : 1.0F, 1.0F),
            this,
            soundIdField
        );

        soundIdField.setChangedListener(
            query -> searchMenu.setVisible(soundIdField.isFocused() && !query.isEmpty())
        );

        searchMenu.setVisible(!soundIdField.getText().isEmpty());

        float initialVolume;
        if (stateVolume != null) {
            initialVolume = stateVolume;
        } else {
            initialVolume = initialData.getVolume();
        }
        
        volumeSlider = new VCSlider(x + width, sy, initialVolume, 0.0f, 10.0f, "%.1f", value -> {});
        volumeSlider.setUIScale(1.0f);

        formatMenu = new TextFormatMenu(
            this.width / 2 + 50, 30, w,
            this::insertAtCaret,
            1.0f
        );
        formatMenu.setMaxEntries((this.height - 50) / h);     

        this.addDrawableChild(new FaButton(x + w / 2 + 80, soundIdField.getY() + 80, 80, 20,
            Text.literal("HUD").styled(style -> style.withColor(0xFFE2CAE9)),
            btn -> {
                MinecraftClient.getInstance().setScreen(new HudEditScreen("Title HUD"));
                save();
                ChatAlert.refresh();
            }
        ));

        this.addDrawableChild(new FaButton(x + w / 2, soundIdField.getY() + 80, 80, 20,
            Text.literal("Save").styled(style -> style.withColor(0xFFE2CAE9)),
            btn -> {
            save();
            ChatAlert.refresh();
            this.client.setScreen(parent);
        }));

        this.addDrawableChild(new FaButton(x + w / 2 - 80, soundIdField.getY() + 80, 80, 20,
            Text.literal("Cancel").styled(style -> style.withColor(0xFFE2CAE9)),
            btn -> this.client.setScreen(parent)
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
        String currentText = field.getText();
        int caretPos = field.getCursor();    
        String newText = currentText.substring(0, caretPos) + format + currentText.substring(caretPos);
        field.setText(newText);
        field.setCursor(caretPos + format.length(), false);
        field.setFocused(true);
    }    

    private void save() {
        float volume = volumeSlider != null ? volumeSlider.getValue() : 1.0f;
            String newKey = keyField.getText().trim();
        if (newKey.isEmpty()) {
            warn();
            return;
        }
        FishyConfig.AlertData newData = new FishyConfig.AlertData(
            msgField.getText(),
            alertTextField.getText(),
            alertColor,
            soundIdField.getText(),
            volume,
            true,
            alertStartsWith
        );

        if (!newKey.equals(alertKey)) {
            FishyConfig.removeChatAlert(alertKey);
        }
        FishyConfig.setChatAlert(newKey, newData);
    }

	public void warn() {
        MinecraftClient cl = MinecraftClient.getInstance();        
        VCPopup popup = new VCPopup(
            Text.literal("Empty field detected! Would you like to restore it?"),
            "No", () -> cl.setScreen(parent),
            "Yes", () -> keyField.setText(alertKey),
            1.0f
            );
        cl.setScreen(new VCOverlay(cl.currentScreen, popup));
	}    

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        renderGuideText(context);

        if (searchMenu != null) {
            searchMenu.setVisible(soundIdField.isFocused() && !soundIdField.getText().isEmpty());
            if (searchMenu.isVisible()) {
                searchMenu.render(context, this, mouseX, mouseY, delta);
            }
        }

        int centerX = this.width / 2;
        int centerY = this.height / 2 - 10;
        int x = centerX - 380;
        int y = centerY - 90;
        int w = 300;
        
        checkTooltip(context, mouseX, mouseY);

        context.drawText(this.textRenderer, "Detected String", x, y, 0xFF808080, false);
        context.drawText(this.textRenderer, "Location", x + w + 10, y, 0xFF808080, false);
        context.drawText(this.textRenderer, "Auto Chat", x, y + 40, 0xFF808080, false);
        context.drawText(this.textRenderer, "On-screen Title", x, y + 80, 0xFF808080, false);
        context.drawText(this.textRenderer, COLOR, x + w + 10, y + 80, 0xFF808080, false);
        context.drawText(this.textRenderer, "SoundEvent ID", x, y + 120, 0xFF808080, false);
        
        if (volumeSlider != null) {
            String volumeLabel = "Volume (" + volumeSlider.getPercentageText() + ")";
            context.drawText(this.textRenderer, volumeLabel, x + w + 10, y + 120, 0xFF808080, false);
            volumeSlider.render(context, mouseX, mouseY);
        }

        if (formatMenu != null && formatMenu.isVisible()) {
            context.getMatrices().push();
            context.getMatrices().translate(0, 0, 300);
            formatMenu.render(context, this, mouseX, mouseY);
            context.getMatrices().pop();
        }        
    }

    private void checkTooltip(DrawContext context, int mouseX, int mouseY) {
        if (alertTextField != null && alertTextField.isMouseOver(mouseX, mouseY)) {

            String previewText = alertTextField.getText().trim();
            if (!previewText.isEmpty()) {

                try {
                    Text formattedPreview = Enhancer.parseFormattedText(previewText);
                    int tooltipWidth = Math.min(400, this.textRenderer.getWidth(formattedPreview) + 20);
                    int tooltipHeight = alertTextField.getHeight();
                    int tooltipX = alertTextField.getX();
                    int tooltipY = alertTextField.getY() + tooltipHeight + 5;

                    if (tooltipX + tooltipWidth > this.width) {
                        tooltipX = mouseX - tooltipWidth - 10;
                    }
                    if (tooltipY < 0) {
                        tooltipY = mouseY + 20;
                    }

                    context.getMatrices().push();
                    context.getMatrices().translate(0, 0, 300);
                    context.fill(tooltipX, tooltipY, 
                               tooltipX + tooltipWidth + 6, tooltipY + tooltipHeight + 4, 
                               0xFF171717);
                    
                    context.drawText(this.textRenderer, formattedPreview, 
                                   tooltipX + 3, tooltipY + tooltipHeight / 3, 0xFFFFFFFF, true);
                    context.getMatrices().pop();
                                   
                } catch (Exception e) {
                    System.err.println("[FishyAddons] Error rendering tooltip: " + e.getMessage());
                    e.printStackTrace();
                    renderFallback(mouseX, mouseY, context);
                }
            }
        }
    }

    private void renderFallback(int mouseX, int mouseY, DrawContext context) {
        String errorText = "Error rendering preview";
        int tooltipWidth = Math.min(400, this.textRenderer.getWidth(errorText) + 20);
        int tooltipHeight = 20;
        int tooltipX = mouseX + 10;
        int tooltipY = mouseY - tooltipHeight - 10;
        if (tooltipX + tooltipWidth > this.width) {
            tooltipX = mouseX - tooltipWidth - 10;
        }
        if (tooltipY < 0) {
            tooltipY = mouseY + 20;
        }
                    
        context.getMatrices().push();
        context.getMatrices().translate(0, 0, 300);
        context.fill(tooltipX - 5, tooltipY - 5, 
                    tooltipX + tooltipWidth + 5, tooltipY + tooltipHeight + 5, 
                    0xFF171717);
        context.drawText(this.textRenderer, Text.literal(errorText), 
                    tooltipX + 5, tooltipY + 10, 0xFFFF8080, true);
        context.getMatrices().pop();
    }    

    public void renderGuideText(DrawContext context) {       
        int x = this.width / 2 + 30;
        int y = 30;
        int lineHeight = 15;

        Text title = VCText.header("FishyAddons Custom Alerts", Style.EMPTY.withBold(true));
        context.drawTextWithShadow(this.textRenderer, title, x, y, 0xFFFFFFFF);            

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

            Formatting format = Formatting.GRAY;
            if (instruction.startsWith(" •") || instruction.matches("\\d+\\..*")) {
                format = Formatting.AQUA;
            } else if (instruction.contains("-")) {
                format = Formatting.DARK_AQUA;
            } else if (instruction.startsWith(" The")) {
                format = Formatting.DARK_GRAY;
            }
    
            Text text = Text.literal(instruction).formatted(format);
            context.drawTextWithShadow(this.textRenderer, text, x, y, 0xFFFFFFFF);
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
    
    private boolean handleMenu(double mouseX, double mouseY) {
        if (formatMenu != null && formatMenu.isVisible()) {
            if (formatMenu.mouseClicked(mouseX, mouseY)) {
                return true;
            }
            formatMenu.setVisible(false);
            return false;
        }
        return false;
    }    

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (handleMenu(mouseX, mouseY)) {
            return true;
        }

        if (volumeSlider != null && volumeSlider.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }

        List<VCTextField> fields = List.of(msgField, alertTextField, soundIdField, keyField);
        boolean clickedField = false;
        for (VCTextField field : fields) {
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

        if (searchMenu != null && searchMenu.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (searchMenu != null && soundIdField.isFocused() && searchMenu.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }

        if (keyCode == 292) {
            toggleFormatMenu(true);
            return true;
        }        

        return super.keyPressed(keyCode, scanCode, modifiers);
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
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (volumeSlider != null && volumeSlider.mouseDragged(mouseX, button)) {
            return true;
        }
        
        if (formatMenu != null && formatMenu.isVisible() && formatMenu.mouseDragged(mouseY)) {
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (volumeSlider != null && volumeSlider.mouseReleased(button)) {
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
