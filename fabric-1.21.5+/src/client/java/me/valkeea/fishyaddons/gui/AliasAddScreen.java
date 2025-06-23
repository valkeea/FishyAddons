package me.valkeea.fishyaddons.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import me.valkeea.fishyaddons.config.FishyConfig;
import me.valkeea.fishyaddons.util.FishyNotis;

import java.util.List;

public class AliasAddScreen extends Screen {
    private TextFieldWidget input, commandField;
    private ButtonWidget saveButton, backButton;

    private final Screen parent;

    public AliasAddScreen(Screen parent) {
        super(Text.literal("Add Command Alias"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        input = new TextFieldWidget(this.textRenderer, centerX - 100, centerY - 70, 200, 20, Text.literal("Alias"));
        input.setMaxLength(256);
        input.setFocused(true);
        this.addDrawableChild(input);

        commandField = new TextFieldWidget(this.textRenderer, centerX - 100, centerY - 30, 200, 20, Text.literal("Command"));
        commandField.setMaxLength(256);
        this.addDrawableChild(commandField);

        saveButton = ButtonWidget.builder(Text.literal("Save"), b -> saveCommandAlias())
                .dimensions(centerX - 50, centerY + 10, 100, 20)
                .build();
        backButton = ButtonWidget.builder(Text.literal("Back"), b -> this.client.setScreen(parent))
                .dimensions(centerX - 50, centerY + 40, 100, 20)
                .build();

        this.addDrawableChild(saveButton);
        this.addDrawableChild(backButton);
    }

    private void saveCommandAlias() {
        String alias = input.getText();
        String command = commandField.getText();

        if (!alias.isEmpty() && !command.isEmpty()) {
            try {
                FishyConfig.setCommandAlias(alias, command);
                FishyNotis.send(Text.literal("Chat replacement added: ").append(alias)
                .append(" -> ").append(command.trim()).styled(s -> s.withColor(Formatting.GRAY)));
                this.client.setScreen(parent);
            } catch (Exception e) {
                FishyNotis.send(Text.literal("Error adding command alias: " + e.getMessage())
                        .styled(s -> s.withColor(Formatting.RED)));
                e.printStackTrace();
            }
        } else {
            FishyNotis.send(Text.literal("Please enter a valid alias and command.")
                    .styled(s -> s.withColor(Formatting.YELLOW)));
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (input.keyPressed(keyCode, scanCode, modifiers)) return true;
        if (commandField.keyPressed(keyCode, scanCode, modifiers)) return true;
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (input.charTyped(chr, modifiers)) return true;
        if (commandField.charTyped(chr, modifiers)) return true;
        return super.charTyped(chr, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (input.mouseClicked(mouseX, mouseY, button)) {
            input.setFocused(true);
            commandField.setFocused(false);
            return true;
        }
        if (commandField.mouseClicked(mouseX, mouseY, button)) {
            commandField.setFocused(true);
            input.setFocused(false);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(context, mouseX, mouseY, partialTicks);
        super.render(context, mouseX, mouseY, partialTicks);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, this.height / 2 - 90, 0xFF55FFFF);
        context.drawTextWithShadow(this.textRenderer, Text.literal("Alias:"), this.width / 2 - 100, this.height / 2 - 80, 0xFF808080);
        context.drawTextWithShadow(this.textRenderer, Text.literal("Command:"), this.width / 2 - 100, this.height / 2 - 40, 0xFF808080);
        input.render(context, mouseX, mouseY, partialTicks);
        commandField.render(context, mouseX, mouseY, partialTicks);

        // Tooltip
        if (mouseX >= commandField.getX() && mouseX < commandField.getX() + commandField.getWidth() &&
            mouseY >= commandField.getY() && mouseY < commandField.getY() + commandField.getHeight()) {
            GuiUtil.fishyTooltip(context, this.textRenderer, List.of(
                Text.literal("Note:"),
                Text.literal("§8Text without / will be sent in the currently "),
                Text.literal("§8toggled chat as is.")
            ), mouseX, mouseY);
        }
    }
}

