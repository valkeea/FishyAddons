package me.valkeea.fishyaddons.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.List;

public class TabbedListScreen extends Screen {
    public enum Tab { COMMANDS, KEYBINDS, CHAT }
    private Tab currentTab = Tab.COMMANDS;
    private AliasEntryList commandEntryList;
    private KeybindEntryList keybindEntryList;
    private ChatEntryList chatEntryList;
    private final Screen parent;

    protected boolean addingNewEntry = false;
    protected FishyPopup fishyPopup = null;

    public TabbedListScreen(Screen parent, Tab tab) {
        super(Text.literal("Aliases, Keybinds & Chat Replacement List"));
        this.parent = parent;
        this.currentTab = tab;
    }

    @Override
    protected void init() {
        int listWidth = 700;
        int listHeight = height - 120;
        int listY = 70;

        // Tab buttons
        addDrawableChild(ButtonWidget.builder(Text.literal("Alias"), b -> switchTab(Tab.COMMANDS))
            .dimensions(width / 2 - 160, 40, 80, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Keybind"), b -> switchTab(Tab.KEYBINDS))
            .dimensions(width / 2 - 55, 40, 80, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Chat"), b -> switchTab(Tab.CHAT))
            .dimensions(width / 2 + 50, 40, 80, 20).build());            

        // Lists
        commandEntryList = new AliasEntryList(MinecraftClient.getInstance(), listWidth, listHeight, listY, 24, this);
        keybindEntryList = new KeybindEntryList(MinecraftClient.getInstance(), listWidth, listHeight, listY, 24, this);
        chatEntryList = new ChatEntryList(MinecraftClient.getInstance(), listWidth, listHeight, listY, 24, this);

        commandEntryList.setPosition((width - listWidth) / 2, listY);
        keybindEntryList.setPosition((width - listWidth) / 2, listY);        
        chatEntryList.setPosition((width - listWidth) / 2, listY);

        commandEntryList.refreshWithAdd();
        keybindEntryList.refreshWithAdd();
        chatEntryList.refreshWithAdd();
            
        addDrawableChild(commandEntryList);
        addDrawableChild(keybindEntryList);
        addDrawableChild(chatEntryList);
        
        updateTabVisibility();

        // Bottom buttons
        addDrawableChild(ButtonWidget.builder(Text.literal("Back"), b -> this.client.setScreen(new QolScreen()))
            .dimensions(width / 2 - 120, height - 30, 80, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Close"), b -> this.client.setScreen(null))
            .dimensions(width / 2 - 10, height - 30, 80, 20).build());

        // Refresh the entry list to ensure it's up-to-date
        this.client.execute(this::refreshEntryList);
    }

    private void switchTab(Tab tab) {
        this.currentTab = tab;
        if (fishyPopup != null) {
            fishyPopup = null; // Close any open popup when switching tabs
        }
        updateTabVisibility();
        this.client.execute(this::refreshEntryList);
        // Reset focused element to avoid issues when switching tabs
        this.setFocused(null);
    }

    private void updateTabVisibility() {
        if (commandEntryList != null) commandEntryList.visible = (currentTab == Tab.COMMANDS);
        if (keybindEntryList != null) keybindEntryList.visible = (currentTab == Tab.KEYBINDS);
        if (chatEntryList != null) chatEntryList.visible = (currentTab == Tab.CHAT);        
    }

    protected void refreshEntryList() {
        switch (currentTab) {
            case COMMANDS -> commandEntryList.refreshWithAdd();
            case KEYBINDS -> keybindEntryList.refreshWithAdd();
            case CHAT -> chatEntryList.refreshWithAdd();
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, width / 2, 20, 0xFF55FFFF);
        super.render(context, mouseX, mouseY, delta);

        // Only show tooltips if popup is NOT open
        if (fishyPopup == null) {
            if (currentTab == Tab.COMMANDS) {
                for (var entry : commandEntryList.children()) {
                    if (entry instanceof GenericEntryList.GenericEntry ge && ge.pendingTooltip != null) {
                        GuiUtil.fishyTooltip(context, this.textRenderer, ge.tooltipLines != null ? ge.tooltipLines : List.of(Text.literal(ge.pendingTooltip)), ge.tooltipX, ge.tooltipY);
                        break; // Only show one tooltip
                    }
                }
            } else if (currentTab == Tab.KEYBINDS) {
                for (var entry : keybindEntryList.children()) {
                    if (entry instanceof GenericEntryList.GenericEntry ge && ge.pendingTooltip != null) {
                        GuiUtil.fishyTooltip(context, this.textRenderer, ge.tooltipLines != null ? ge.tooltipLines : List.of(Text.literal(ge.pendingTooltip)), ge.tooltipX, ge.tooltipY);
                        break; // Only show one tooltip
                    }
                }
            } else if (currentTab == Tab.CHAT) {
                for (var entry : chatEntryList.children()) {
                    if (entry instanceof GenericEntryList.GenericEntry ge && ge.pendingTooltip != null) {
                        GuiUtil.fishyTooltip(context, this.textRenderer, ge.tooltipLines != null ? ge.tooltipLines : List.of(Text.literal(ge.pendingTooltip)), ge.tooltipX, ge.tooltipY);
                        break; // Only show one tooltip
                    }
                }
            }
        }

        // Render popup if it exists
        if (fishyPopup != null) {
            this.renderBackground(context, mouseX, mouseY, delta);
            fishyPopup.render(context, this.textRenderer, mouseX, mouseY, delta);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (fishyPopup != null) {
            if (fishyPopup.mouseClicked(mouseX, mouseY, button)) return true;
            return false; // Block clicks to the rest of the UI
        }        
        if (currentTab == Tab.COMMANDS) {
            if (commandEntryList.getHoveredCommandEntry() instanceof GenericEntryList.GenericEntry entry) {
                if (entry.inputWidget instanceof TextFieldWidget field) {
                    if (field.mouseClicked(mouseX, mouseY, button)) {
                        field.setFocused(true);
                        this.setFocused(field);
                        return true;
                    }
                } else if (entry.inputWidget instanceof ButtonWidget btn) {
                    if (btn.mouseClicked(mouseX, mouseY, button)) {
                        btn.setFocused(true);
                        this.setFocused(btn);
                        return true;
                    }
                }
                if (entry.outputField.mouseClicked(mouseX, mouseY, button)) {
                    entry.outputField.setFocused(true);
                    this.setFocused(entry.outputField);
                    return true;
                }
                if (entry.saveButton.mouseClicked(mouseX, mouseY, button)) return true;
                if (entry.deleteButton.mouseClicked(mouseX, mouseY, button)) return true;
                if (entry.toggleButton.mouseClicked(mouseX, mouseY, button)) return true;
            }
        } else if (currentTab == Tab.KEYBINDS) {
            if (keybindEntryList.getHoveredKeybindEntry() instanceof GenericEntryList.GenericEntry entry) {
                if (entry.inputWidget instanceof TextFieldWidget field) {
                    if (field.mouseClicked(mouseX, mouseY, button)) {
                        field.setFocused(true);
                        this.setFocused(field);
                        return true;
                    }
                } else if (entry.inputWidget instanceof ButtonWidget btn) {
                    if (btn.mouseClicked(mouseX, mouseY, button)) {
                        btn.setFocused(true);
                        this.setFocused(btn);
                        return true;
                    }
                }
                if (entry.outputField.mouseClicked(mouseX, mouseY, button)) {
                    entry.outputField.setFocused(true);
                    this.setFocused(entry.outputField);
                    return true;
                }
                if (entry.saveButton.mouseClicked(mouseX, mouseY, button)) return true;
                if (entry.deleteButton.mouseClicked(mouseX, mouseY, button)) return true;
                if (entry.toggleButton.mouseClicked(mouseX, mouseY, button)) return true;
            }
        } else if (currentTab == Tab.CHAT) {
            if (chatEntryList.getHoveredChatEntry() instanceof GenericEntryList.GenericEntry entry) {
                if (entry.inputWidget instanceof TextFieldWidget field) {
                    if (field.mouseClicked(mouseX, mouseY, button)) {
                        field.setFocused(true);
                        this.setFocused(field);
                        return true;
                    }
                } else if (entry.inputWidget instanceof ButtonWidget btn) {
                    if (btn.mouseClicked(mouseX, mouseY, button)) {
                        btn.setFocused(true);
                        this.setFocused(btn);
                        return true;
                    }
                }
                if (entry.outputField.mouseClicked(mouseX, mouseY, button)) {
                    entry.outputField.setFocused(true);
                    this.setFocused(entry.outputField);
                    return true;
                }
                if (entry.saveButton.mouseClicked(mouseX, mouseY, button)) return true;
                if (entry.deleteButton.mouseClicked(mouseX, mouseY, button)) return true;
                if (entry.toggleButton.mouseClicked(mouseX, mouseY, button)) return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (fishyPopup != null) {
            // handle popup keys if needed
            return false;
        }
        if (currentTab == Tab.COMMANDS) {
            if (commandEntryList.getFocused() instanceof GenericEntryList.GenericEntry entry) {
                if (entry.outputField.isFocused() && entry.outputField.keyPressed(keyCode, scanCode, modifiers)) return true;
                if (entry.inputWidget instanceof TextFieldWidget field && field.isFocused() && field.keyPressed(keyCode, scanCode, modifiers)) return true;
                if (entry.inputWidget instanceof ButtonWidget btn && btn.isFocused() && btn.keyPressed(keyCode, scanCode, modifiers)) return true;
            }
        } else if (currentTab == Tab.KEYBINDS) {
            if (keybindEntryList.getFocused() instanceof GenericEntryList.GenericEntry entry) {
                if (entry.outputField.isFocused() && entry.outputField.keyPressed(keyCode, scanCode, modifiers)) return true;
                if (entry.inputWidget instanceof TextFieldWidget field && field.isFocused() && field.keyPressed(keyCode, scanCode, modifiers)) return true;
                if (entry.inputWidget instanceof ButtonWidget btn && btn.isFocused() && btn.keyPressed(keyCode, scanCode, modifiers)) return true;
            }
        } else if (currentTab == Tab.CHAT) {
            if (chatEntryList.getFocused() instanceof GenericEntryList.GenericEntry entry) {
                if (entry.outputField.isFocused() && entry.outputField.keyPressed(keyCode, scanCode, modifiers)) return true;
                if (entry.inputWidget instanceof TextFieldWidget field && field.isFocused() && field.keyPressed(keyCode, scanCode, modifiers)) return true;
                if (entry.inputWidget instanceof ButtonWidget btn && btn.isFocused() && btn.keyPressed(keyCode, scanCode, modifiers)) return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (fishyPopup != null) {
            // handle popup chars if needed
            return false;
        }
        if (currentTab == Tab.COMMANDS) {
            if (commandEntryList.getFocused() instanceof GenericEntryList.GenericEntry entry) {
                if (entry.outputField.isFocused() && entry.outputField.charTyped(chr, modifiers)) return true;
                if (entry.inputWidget instanceof TextFieldWidget field && field.isFocused() && field.charTyped(chr, modifiers)) return true;
            }
        } else if (currentTab == Tab.KEYBINDS) {
            if (keybindEntryList.getFocused() instanceof GenericEntryList.GenericEntry entry) {
                if (entry.outputField.isFocused() && entry.outputField.charTyped(chr, modifiers)) return true;
                if (entry.inputWidget instanceof TextFieldWidget field && field.isFocused() && field.charTyped(chr, modifiers)) return true;
            }
        } else if (currentTab == Tab.CHAT) {
            if (chatEntryList.getFocused() instanceof GenericEntryList.GenericEntry entry) {
                if (entry.outputField.isFocused() && entry.outputField.charTyped(chr, modifiers)) return true;
                if (entry.inputWidget instanceof TextFieldWidget field && field.isFocused() && field.charTyped(chr, modifiers)) return true;
            }
        }
        return super.charTyped(chr, modifiers);
    }

    public void showFishyPopup(Text title, Text continueButtonText, Runnable onContinue, Text discardButtonText, Runnable onDiscard) {
        this.fishyPopup = new FishyPopup(title, continueButtonText, onContinue, discardButtonText, onDiscard);
        this.fishyPopup.init(this.width, this.height);
    }
}

