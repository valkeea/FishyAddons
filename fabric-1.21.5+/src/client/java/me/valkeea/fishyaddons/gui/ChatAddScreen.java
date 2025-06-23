package me.valkeea.fishyaddons.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import me.valkeea.fishyaddons.config.FishyConfig;
import me.valkeea.fishyaddons.util.FishyNotis;

import java.util.List;

public class ChatAddScreen extends Screen {
    private TextFieldWidget inputField, outputField;
    private ButtonWidget saveButton, backButton;

    private final Screen parent;

    public ChatAddScreen(Screen parent) {
        super(Text.literal("Add Chat Replacement"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        inputField = new TextFieldWidget(this.textRenderer, centerX - 100, centerY - 70, 200, 20, Text.literal("Typed Text"));
        inputField.setMaxLength(256);
        inputField.setFocused(true);
        this.addDrawableChild(inputField);

        outputField = new TextFieldWidget(this.textRenderer, centerX - 100, centerY - 30, 200, 20, Text.literal("Replaced Text"));
        outputField.setMaxLength(256);
        this.addDrawableChild(outputField);

        saveButton = ButtonWidget.builder(Text.literal("Save"), b -> saveChat())
                .dimensions(centerX - 50, centerY + 10, 100, 20)
                .build();
        backButton = ButtonWidget.builder(Text.literal("Back"), b -> this.client.setScreen(parent))
                .dimensions(centerX - 50, centerY + 40, 100, 20)
                .build();

        this.addDrawableChild(saveButton);
        this.addDrawableChild(backButton);
    }

    private void saveChat() {
        String input = inputField.getText();
        String output = outputField.getText();

        if (!input.isEmpty() && !output.isEmpty()) {
            try {
                FishyConfig.setChatReplacement(input, output);
                FishyNotis.send(Text.literal("Chat Replacement Added: ").append(input)
                .append(" -> ").append(output.trim()).styled(s -> s.withColor(Formatting.GRAY)));
                this.client.setScreen(parent);
            } catch (Exception e) {
                FishyNotis.send(Text.literal("Error adding chat replacement: " + e.getMessage())
                        .styled(s -> s.withColor(Formatting.RED)));
                e.printStackTrace();
            }
        } else {
            FishyNotis.send("Please fill in both fields.");
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (inputField.keyPressed(keyCode, scanCode, modifiers)) return true;
        if (outputField.keyPressed(keyCode, scanCode, modifiers)) return true;
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (inputField.charTyped(chr, modifiers)) return true;
        if (outputField.charTyped(chr, modifiers)) return true;
        return super.charTyped(chr, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (inputField.mouseClicked(mouseX, mouseY, button)) {
            inputField.setFocused(true);
            outputField.setFocused(false);
            return true;
        }
        if (outputField.mouseClicked(mouseX, mouseY, button)) {
            outputField.setFocused(true);
            inputField.setFocused(false);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(context, mouseX, mouseY, partialTicks);
        super.render(context, mouseX, mouseY, partialTicks);

        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, this.height / 2 - 90, 0xFF55FFFF);
        context.drawTextWithShadow(this.textRenderer, Text.literal("Typed Text:"), this.width / 2 - 100, this.height / 2 - 80, 0xFF808080);
        context.drawTextWithShadow(this.textRenderer, Text.literal("Replaced Text:"), this.width / 2 - 100, this.height / 2 - 40, 0xFF808080);
        inputField.render(context, mouseX, mouseY, partialTicks);
        outputField.render(context, mouseX, mouseY, partialTicks);

        if (mouseX >= outputField.getX() && mouseX < outputField.getX() + outputField.getWidth() &&
            mouseY >= outputField.getY() && mouseY < outputField.getY() + outputField.getHeight()) {
            GuiUtil.fishyTooltip(context, MinecraftClient.getInstance().textRenderer,List.of(
                Text.literal("Note:"),
                Text.literal("§8Text without / will be sent in the currently "),
                Text.literal("§8toggled chat as is.")
            ), mouseX, mouseY);
        }
    }
}

