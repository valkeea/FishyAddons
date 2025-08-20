package me.valkeea.fishyaddons.gui;

import java.util.List;

import me.valkeea.fishyaddons.config.FishyConfig;
import me.valkeea.fishyaddons.handler.ChatAlert;
import me.valkeea.fishyaddons.util.SoundUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

public class AlertEditScreen extends Screen {
    private final Screen parent;
    private final FishyConfig.AlertData initialData;
    private static final String COLOR = "Color";
    private String alertKey;

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
        this.alertColor = this.initialData.getColor();
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2 - 10;

        int x = centerX - 350;
        int y = centerY - 80;
        int w = 300;
        int h = 20;

        TextFieldWidget keyField = new FaTextField(this.textRenderer, x, y, w, h, Text.literal("Key"));
        keyField.setText(alertKey != null ? alertKey : "");
        this.addDrawableChild(keyField);

        msgField = new FaTextField(this.textRenderer, x, y + 40, w, h, Text.literal("Chat message"));
        msgField.setText(initialData.getMsg() != null ? initialData.getMsg() : "");
        this.addDrawableChild(msgField);

        alertTextField = new FaTextField(this.textRenderer, x, y + 80, w, h, Text.literal("On-screen Alert"));
        alertTextField.setText(initialData.getOnscreen() != null ? initialData.getOnscreen() : "");
        this.addDrawableChild(alertTextField);

        addDrawableChild(new FaButton(x + w, centerY, 50, h, 
            Text.literal(COLOR).styled(style -> style.withColor(alertColor)),
            btn -> { 
                float[] rgb = ColorWheel.intToRGB(alertColor);
                this.client.setScreen(new ColorWheel(this, rgb, selected -> {
                    alertColor = ColorWheel.rgbToInt(selected);
                    btn.setMessage(Text.literal(COLOR).styled(s -> s.withColor(alertColor)));
                    this.client.setScreen(this);
                }));
            }
        ));

        soundIdField = new FaTextField(this.textRenderer, x, y + 120, w, h, Text.literal("SoundEvent ID"));
        soundIdField.setText(initialData.getSoundId() != null ? initialData.getSoundId() : "");
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
            soundId -> SoundUtil.playDynamicSound(soundId, 1.0F, 1.0F),
            this,
            soundIdField
        );

        // Update searchMenu filter when soundIdField changes
        soundIdField.setChangedListener(
            query -> searchMenu.setVisible(soundIdField.isFocused() && !query.isEmpty())
        );

        searchMenu.setVisible(!soundIdField.getText().isEmpty());

        volumeField = new FaTextField(this.textRenderer, x + width, sy, 50, 20, Text.literal("Volume"));
        volumeField.setText(String.valueOf(initialData.getVolume()));
        this.addDrawableChild(volumeField);

        this.addDrawableChild(new FaButton(x + w / 2 + 80, soundIdField.getY() + 80, 80, 20,
            Text.literal("HUD").styled(style -> style.withColor(0xE2CAE9)),
            btn -> MinecraftClient.getInstance().setScreen(new HudEditScreen("Title HUD"))
        ));        

        this.addDrawableChild(done(x + w / 2, keyField));
    }

    private FaButton done(int x, TextFieldWidget keyField) {
        return new FaButton(x, soundIdField.getY() + 80, 80, 20,
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
            } catch (NumberFormatException ignored) {
                System.err.println("Invalid volume input: " + volumeField.getText());
            }
            String newKey = keyField.getText().trim();
            if (newKey.isEmpty()) {
                return;
            }
            FishyConfig.AlertData newData = new FishyConfig.AlertData(
                msgField.getText(),
                alertTextField.getText(),
                alertColor,
                soundIdField.getText(),
                volume,
                true
            );
            if (!newKey.equals(alertKey)) {
                FishyConfig.removeChatAlert(alertKey);
            }
            FishyConfig.setChatAlert(newKey, newData);
            alertKey = newKey;
            ChatAlert.refresh();
            this.client.setScreen(parent);
        });
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        renderGuideText(context);
        super.render(context, mouseX, mouseY, delta);

        // Only show dropdown if field is focused and has text
        if (searchMenu != null) {
            searchMenu.setVisible(soundIdField.isFocused() && !soundIdField.getText().isEmpty());
            if (searchMenu.isVisible()) {
                searchMenu.render(context, this, mouseX, mouseY, delta);
            }
        }

        int centerX = this.width / 2;
        int centerY = this.height / 2 - 10;
        int x = centerX - 350;
        int y = centerY - 90; 
        int w = 300;       

        context.drawText(this.textRenderer, "Detected String", x, y, 0xFF808080, false);
        context.drawText(this.textRenderer, "Auto Chat", x, y + 40, 0xFF808080, false);
        context.drawText(this.textRenderer, "On-screen Title", x, y + 80, 0xFF808080, false);
        context.drawText(this.textRenderer, COLOR, x + w + 10, y + 80, 0xFF808080, false);
        context.drawText(this.textRenderer, "SoundEvent ID", x, y + 120, 0xFF808080, false);
        context.drawText(this.textRenderer, "Volume", x + w + 10, y + 120, 0xFF808080, false);
    }

    public void renderGuideText(DrawContext context) {       
        int x = this.width / 2 + 50;
        int y = this.height / 2 - 175;
        int lineHeight = 15;

        Text title = Text.literal("FishyAddons Custom Alerts").formatted(Formatting.BOLD, Formatting.AQUA);

        context.getMatrices().push();
        context.getMatrices().translate(0,0, 300);
        context.drawTextWithShadow(this.textRenderer, title, x, y, 0xFFFFFF);            
        context.getMatrices().pop();

        y += lineHeight * 2;
                
        String[] instructions = {
            " The First field is required to successfully create an alert!",
            " Leaving other fields empty will disable those functions.",
            "",
            "- Detected String -",
            " • §7Matched anywhere in the message",
            " • §8Example: alert set to 'hel', the alert will also trigger on 'hello'",
            "",
            "- Auto Chat -",
            " • §7Optional chat message sent with the alert",
            "",
            "- On-screen Title -",
            " • §7Text appears for 2 seconds",
            " • §7Position, color and size can be customized in /fa hud",
            "",
            "- SoundEvent -",
            " • §7Plays a Minecraft SoundEvent when",
            "   the alert is triggered",
            " • §7You can preview sounds by right-clicking",
            "   them in the dropdown",
            " • §7Volume is affected by internal settings",
            "   §3Note: FA provides 3 custom events.",
            "   §3You can replace with a resource pack",
            "   §3by placing fishyaddons_1.ogg, (1-3)",
            "   §3in assets/fishyaddons/sounds/custom.",
            " • §7Alerts can also be loaded from JSON"           
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

            context.getMatrices().push();
            context.getMatrices().translate(0,0, 300);      
            Text text = Text.literal(instruction).formatted(format);
            context.drawTextWithShadow(this.textRenderer, text, x, y, 0xFFFFFF);
            context.getMatrices().pop();
            y += lineHeight;
        }              
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (soundIdField.isFocused() && (
                msgField.isMouseOver(mouseX, mouseY) || 
                alertTextField.isMouseOver(mouseX, mouseY) ||
                volumeField.isMouseOver(mouseX, mouseY))) {
            soundIdField.setFocused(false);
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