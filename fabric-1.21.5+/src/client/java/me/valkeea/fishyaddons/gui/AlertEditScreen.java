package me.valkeea.fishyaddons.gui;

import java.util.List;

import me.valkeea.fishyaddons.config.FishyConfig;
import me.valkeea.fishyaddons.handler.ChatAlert;
import me.valkeea.fishyaddons.util.SoundUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class AlertEditScreen extends Screen {
    private final Screen parent;
    private final String alertKey;
    private final FishyConfig.AlertData initialData;

    private SoundSearchMenu searchMenu;
    private TextFieldWidget msgField;
    private TextFieldWidget alertTextField;
    private TextFieldWidget soundIdField;
    private TextFieldWidget volumeField;
    private int alertColor = 0xFFFFFFFF;

    public AlertEditScreen(String key, FishyConfig.AlertData data, Screen parent) {
        super(Text.literal("Edit Alert"));
        this.parent = parent;
        this.alertKey = key;
        this.initialData = data != null ? data : new FishyConfig.AlertData("", "", 0xFFFFFFFF, "", 1.0F, true);
        this.alertColor = this.initialData.color;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2 - 10;

        msgField = new FaTextField(this.textRenderer, centerX - 300, centerY, 100, 20, Text.literal("Chat message"));
        msgField.setText(initialData.msg != null ? initialData.msg : "");
        this.addDrawableChild(msgField);

        alertTextField = new FaTextField(this.textRenderer, centerX - 190, centerY, 120, 20, Text.literal("On-screen Alert"));
        alertTextField.setText(initialData.onscreen != null ? initialData.onscreen : "");
        this.addDrawableChild(alertTextField);        

        addDrawableChild(new FaButton(centerX - 70, centerY, 50, 20, 
            Text.literal("Color").styled(style -> style.withColor(alertColor)),
            btn -> { 
                float[] rgb = ColorWheel.intToRGB(alertColor);
                this.client.setScreen(new ColorWheel(this, rgb, selected -> {
                    alertColor = ColorWheel.rgbToInt(selected);
                    btn.setMessage(Text.literal("Color").styled(s -> s.withColor(alertColor)));
                    this.client.setScreen(this);
                }));
            }
        ));

        soundIdField = new FaTextField(this.textRenderer, centerX - 10, centerY, 200, 20, Text.literal("SoundEvent ID"));
        soundIdField.setText(initialData.soundId != null ? initialData.soundId : "");
        this.addDrawableChild(soundIdField);

        List<String> soundIds = Registries.SOUND_EVENT.stream()
            .map(Registries.SOUND_EVENT::getId)
            .filter(java.util.Objects::nonNull)
            .map(Identifier::toString)
            .sorted()
            .toList();

        int x = soundIdField.getX();
        int y = soundIdField.getY();
        int width = soundIdField.getWidth() + 50;
        int entryHeight = 18;

        searchMenu = new SoundSearchMenu(
            soundIds,
            x, y, width, entryHeight,
            soundId -> {
                soundIdField.setText(soundId);
                searchMenu.setVisible(false);
            },
            soundId -> {
                SoundUtil.playDynamicSound(soundId, 1.0F, 1.0F);
            },
            this,
            soundIdField
        );

        // Update searchMenu filter when soundIdField changes
        soundIdField.setChangedListener(query -> {
            searchMenu.setVisible(soundIdField.isFocused() && !query.isEmpty());
        });

        soundIdField.setFocused(true);
        searchMenu.setVisible(!soundIdField.getText().isEmpty());

        volumeField = new FaTextField(this.textRenderer, centerX + 190, centerY, 50, 20, Text.literal("Volume"));
        volumeField.setText(String.valueOf(initialData.volume));
        this.addDrawableChild(volumeField); 
        
        this.addDrawableChild(new FaButton(centerX - 60, soundIdField.getY() + 80, 80, 20,
            Text.literal("Done").styled(style -> style.withColor(0xE2CAE9)),
            btn -> {
                float volume = 1.0F;
                try {
                    String input = volumeField.getText().trim();
                    if (!input.isEmpty()) {
                        volume = Float.parseFloat(input);

                        if (volume < 0.0F) volume = 0.0F;
                        if (volume > 5.0F) volume = 5.0F;
                    }
                } catch (NumberFormatException ignored) {}
                FishyConfig.AlertData newData = new FishyConfig.AlertData(
                    msgField.getText(),
                    alertTextField.getText(),
                    alertColor,
                    soundIdField.getText(),
                    volume,
                    true
                );
                FishyConfig.setChatAlert(alertKey, newData);
                ChatAlert.refresh();
                this.client.setScreen(parent);
            }));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);

        // Only show dropdown if field is focused and has text
        if (searchMenu != null) {
            searchMenu.setVisible(soundIdField.isFocused() && !soundIdField.getText().isEmpty());
            if (searchMenu.isVisible()) {
                searchMenu.render(context, this, mouseX, mouseY, delta);
            }
        }

        int centerX = this.width / 2;
        int y = this.height / 2 - 10;

        context.drawText(this.textRenderer, "Chat Message", centerX - 300, y - 12, 0xFF808080, false);
        context.drawText(this.textRenderer, "On-screen Title", centerX - 190, y - 12, 0xFF808080, false);        
        context.drawText(this.textRenderer, "Color", centerX - 70, y - 12, 0xFF808080, false);
        context.drawText(this.textRenderer, "SoundEvent ID", centerX - 10, y - 12, 0xFF808080, false);
        context.drawText(this.textRenderer, "Volume", centerX + 190, y - 12, 0xFF808080, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // If another field is clicked, unfocus soundIdField
        if (soundIdField.isFocused()) {
            if (msgField.isMouseOver(mouseX, mouseY) || 
                alertTextField.isMouseOver(mouseX, mouseY) ||
                volumeField.isMouseOver(mouseX, mouseY)) {
                soundIdField.setFocused(false);
            }
        }
        if (searchMenu != null && searchMenu.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Only forward to searchMenu if soundIdField is focused
        if (searchMenu != null && soundIdField.isFocused() && searchMenu.keyPressed(keyCode, scanCode, modifiers)) {
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

    @Override
    public boolean shouldPause() {
        return false;
    }
}