package me.valkeea.fishyaddons.ui.list;

import java.util.Map;

import com.mojang.blaze3d.platform.InputConstants;

import me.valkeea.fishyaddons.feature.qol.FishyPresets;
import me.valkeea.fishyaddons.feature.qol.input.ChatReplacement;
import me.valkeea.fishyaddons.feature.qol.input.CommandAlias;
import me.valkeea.fishyaddons.feature.qol.input.KeyShortcut;
import me.valkeea.fishyaddons.ui.element.ChoicePopup;
import me.valkeea.fishyaddons.util.Keyboard;
import me.valkeea.fishyaddons.vconfig.config.impl.ShortcutsConfig;
import me.valkeea.fishyaddons.vconfig.ui.manager.ScreenManager;
import me.valkeea.fishyaddons.vconfig.ui.render.RenderUtils;
import me.valkeea.fishyaddons.vconfig.ui.render.VCText;
import me.valkeea.fishyaddons.vconfig.ui.widget.FaButton;
import me.valkeea.fishyaddons.vconfig.ui.widget.FaTextField;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;

public final class TabbedListScreen extends DynamicListScreen {
    public enum Tab { COMMANDS, KEYBINDS, CHAT }
    private Tab currentTab = Tab.COMMANDS;

    private AliasEntryList cmdList;
    private KeybindEntryList keyList;
    private ChatEntryList chatList;

    private static final int HEADERH = 20;

    private static final int TBW = 80;
    private static final int OFFSET = 30;
    private static final int INW = 100;
    private static final int OUTW = 230;
    private static final int BTNW = 60;
    private static final int GAP = 10;
    private static final int TOGGLEW = 40; 
    private static final int LIST_END_OFF = OFFSET * 2;
    private static final int LIST_Y = HEADERH + OFFSET + 40;

    protected int bBtnY;
    private boolean isAdding;

    public TabbedListScreen(Tab tab) {
        super(VCText.header("Aliases, Keybinds, Chat Replacement", null));
        this.currentTab = tab;
    }

    @Override
    public boolean isAdding() {
        return isAdding;
    }

    @Override
    public void setAdding(boolean state) {
        isAdding = state;
    }

    private GenericEntryList<String> getActiveList() {
        return switch (currentTab) {
            case COMMANDS -> cmdList;
            case KEYBINDS -> keyList;
            case CHAT -> chatList;
        };
    }    

    @Override
    protected void init() {
        super.init();

        var settings = createSettings();

        int h = settings.elH();
        int w = TBW;
        int tabBtnY = 40;

        int tabW = w * 3 + h * 2;
        int tx = width / 2 - tabW / 2;
        addRenderableWidget(new FaButton(tx, tabBtnY, w, h,
            text("Commands"),
            btn -> switchTab(Tab.COMMANDS))
        );
        tx += w + h;

        addRenderableWidget(new FaButton(tx, tabBtnY, w, h,
            text("Keybinds"),
            btn -> switchTab(Tab.KEYBINDS))
        );
        tx += w + h;

        addRenderableWidget(new FaButton(tx, tabBtnY, w, h,
            text("Chat"),
            btn -> switchTab(Tab.CHAT))
        );

        var mc = Minecraft.getInstance();
        cmdList = new AliasEntryList(mc, settings, this);
        keyList = new KeybindEntryList(mc, settings, this);
        chatList = new ChatEntryList(mc, settings, this);

        cmdList.setPosition(settings.listX(), settings.listY());
        keyList.setPosition(settings.listX(), settings.listY());        
        chatList.setPosition(settings.listX(), settings.listY());
            
        addRenderableWidget(cmdList);
        addRenderableWidget(keyList);
        addRenderableWidget(chatList);
        
        updateTabVisibility();       

        int x = width / 2 - w;
        bBtnY = settings.listY() + settings.listH() + 2;

        addRenderableWidget(new FaButton(x, bBtnY, w, h,
            styled("Back", 0xFFB0B0B0),
            b -> ScreenManager.openConfigScreen()
        ));

        x += w;
        addRenderableWidget(new FaButton(x, bBtnY, w, h,
            styled("Close", 0xFFB0B0B0),
            b -> this.onClose()
        ));

        x += w;
        addPresetButtons(x, bBtnY, TOGGLEW, h,
            this::getTypeForTab,
            (suffix, override) -> {
                var list = getActiveList();
                loadPresetForCurrentTab(suffix, override);
                list.repopulate();
                list.onRefresh();
            }, this::saveCurrentTabAsPreset
        );

        this.minecraft.execute(getActiveList()::repopulate);
    }

    protected GenericEntryList.EntryConfig createSettings() {
        int listW = getListW();     
        int listX = (width - listW) / 2;
        
        return new GenericEntryList.EntryConfig(
            listX, LIST_Y, listW, getListH(), OFFSET,
            20, 24, INW, OUTW, BTNW, GAP, true
        );
    }

    protected int getListH() {
        return height - LIST_END_OFF - LIST_Y;
    }

    private int getListW() {
        return INW + OUTW + BTNW * 2 + GAP * 6 + TOGGLEW + OFFSET * 2;
    }

    @Override
    public void extractContent(GuiGraphicsExtractor gge, int mX, int mY, float delta) {
        super.extractContent(gge, mX, mY, delta);
        gge.centeredText(font, getTitle(), width / 2, HEADERH, 0xFFFFFFFF);
        renderPresets(gge, mX, mY);
    }

    private void switchTab(Tab tab) {
        this.currentTab = tab;
        setAdding(false);
        setOverlay(null);
        this.setFocused(null);        
        updateTabVisibility();
        this.minecraft.execute(getActiveList()::repopulate);
    }

    private void updateTabVisibility() {
        if (cmdList != null) cmdList.visible = (currentTab == Tab.COMMANDS);
        if (keyList != null) keyList.visible = (currentTab == Tab.KEYBINDS);
        if (chatList != null) chatList.visible = (currentTab == Tab.CHAT);
    }

    // A bit hacky but realistically these wont change
    private FishyPresets.PresetType getTypeForTab() {
        return FishyPresets.PresetType.valueOf(currentTab.name());
    }

    private void loadPresetForCurrentTab(String suffix, boolean override) {
        var type = getTypeForTab();
        var map = FishyPresets.loadStringPreset(type, suffix);
        if (map == null) return;

        var list = getActiveList();
        if (override) list.getConfig().clear();
        list.getConfig().putAll(map);
        ShortcutsConfig.save();

        if (override) list.setScrollAmount(0);
        getActiveList().repopulate();
    }

    private void saveCurrentTabAsPreset(String suffix) {
        FishyPresets.saveStringPreset(getTypeForTab(), suffix, getActiveList().getConfig());
    }

    protected class GenericEntry extends GenericEntryList.Entry<String> {
        private FaTextField output;

        public GenericEntry(String input, GenericEntryList<String> list, boolean isNew) {
            super(list, input, isNew);
        }

        final void initialize(String input, String output) {
            createInputWidget(input);
            createOutputField(output);
            createButtons();
            setSize(list.getRowWidth(), list.settings.entryH());
        }

        AbstractWidget outputWidget() {
            return widgets.get(1);
        }

        @Override
        protected String getOutput() {
            return output.get();
        }

        @Override
        protected void updateOutput(String out) {
            this.output.update(out);
        }

        void createInputWidget(String input) {
            var in = new FaTextField(minecraft.font, 0, 0, list.settings.inW(), list.settings.elH(), Component.empty());
            in.setMaxLength(256);
            in.set(input);
            in.setBordered(false);
            if (input.length() > 31) in.setTooltip(getTooltipLines(input));
            if (isNew) in.setFocused(true);
            setInputWidget(in);
        }        

        void createOutputField(String output) {
            var out = new FaTextField(
                list.mc.font,
                0, 0, list.settings.outW(), list.settings.elH(),
                text("Command")
            );
            out.setMaxLength(256);
            out.set(output);
            out.setBordered(false);
            if (output.length() > 31) out.setTooltip(getTooltipLines(output));
            this.output = out;
            widgets.add(out);
        }

        void createButtons() {
            var save = new FaButton(
                0, 0, list.settings.btnW(), list.settings.elH(),
                styled("Save", 0xFF808080),
                b ->  list.saveFocused(this)
            );
            widgets.add(save);

            var del = new FaButton(
                0, 0, list.settings.btnW(), list.settings.elH(),
                styled("Delete", 0xFF808080),
                b -> delete()
            );
            widgets.add(del);
            
            boolean toggled = list.isEntryToggled(getInput());
            var toggle = new FaButton.Toggle(
                0, 0, 40, list.settings.elH(),
                b -> toggle(),
                toggled
            ) {};
            widgets.add(toggle);            
        }

        Tooltip getTooltipLines(String text) {
            int lineLength = 64;
            var t = Component.empty();
            for (int i = 0; i < text.length(); i += lineLength) {
                t = t.append(text(text.substring(i, Math.min(i + lineLength, text.length()))));
            }
            return Tooltip.create(t);
        }

        @Override 
        public void extractContent(GuiGraphicsExtractor gge, int mX, int mY, boolean hovered, float delta) {
            this.setHovered(hovered);
            int x = getX();
            int y = getY();

            renderInputWidget(gge, x, y, mX, mY, delta);
            renderOutputWidget(gge, x, y, mX, mY, delta);
            renderButtons(gge, x, y, mX, mY, delta);
        }

        void renderInputWidget(GuiGraphicsExtractor gge, int x, int y, int mX, int mY, float delta) {
            if (inputWidget() instanceof FaTextField) RenderUtils.border(gge, x, y, list.settings.inW(), list.settings.elH(), 0xFF555555);
            inputWidget().setPosition(x, y);
            inputWidget().extractRenderState(gge, mX, mY, delta);
        }

        void renderOutputWidget(GuiGraphicsExtractor gge, int x, int y, int mX, int mY, float delta) {
            x = x + list.settings.inW() + list.settings.gap();
            RenderUtils.border(gge, x, y, list.settings.outW(), list.settings.elH(), 0xFF555555);
            outputWidget().setPosition(x, y);
            outputWidget().extractRenderState(gge, mX, mY, delta);
        }

        void renderButtons(GuiGraphicsExtractor gge, int x, int y, int mX, int mY, float delta) {
            x = x + list.settings.inW() + list.settings.outW() + list.settings.gap() * 2;
            int btnGap = 65;
            var saveBtn = widgets.get(2);
            saveBtn.setPosition(x, y);
            saveBtn.extractRenderState(gge, mX, mY, delta);

            x += btnGap;
            
            var delBtn = widgets.get(3);
            delBtn.setPosition(x, y);
            delBtn.extractRenderState(gge, mX, mY, delta);

            x += btnGap;
            var toggleBtn = widgets.get(4);
            toggleBtn.setPosition(x, y);
            toggleBtn.extractRenderState(gge, mX, mY, delta);
        }  

        @Override
        public boolean keyPressed(KeyEvent input) {
            if (saveOnEnter(input, outputWidget()) || unfocusOnEscape(input)) return true;
            var w = findFirst(wg -> wg.isFocused());
            if (input.key() == InputConstants.KEY_TAB) return handleTabKey(w);
            return w != null && w.keyPressed(input);
        }

        boolean handleTabKey(GuiEventListener w) {
            if (w == inputWidget()) return swap(inputWidget(), outputWidget());
            else if (w == outputWidget()) return swap(outputWidget(), inputWidget());
            return false;
        }

        boolean swap(GuiEventListener a, GuiEventListener b) {
            if (a != null) a.setFocused(false);
            if (b != null) b.setFocused(true);
            return true;
        }
    }

    private class KeybindEntry extends GenericEntry {
        KeybindEntry(String in, KeybindEntryList list, boolean isNew) {
            super(in, list, isNew);
        }

        @Override
        protected void createInputWidget(String in) {
            var w = new FaButton.Keybind(
                0, 0, list.settings.inW(), list.settings.elH(), in, b -> {}
            ) {
                private final KeybindEntry entry = KeybindEntry.this;

                protected boolean onMouseClick(int btn) {
                    if (isListening()) {
                        if (entry.isNew) list.invalidPopup(entry);
                        else setListening(false);
                        return true;
                    } else {
                        setListening(true);
                        return true;
                    }
                }

                protected boolean newMouseKey(int btn) {
                    var newKey = Keyboard.mouseButtonToBinding(btn);
                    if (newKey == null && list.screen != null) {
                        ChoicePopup.set(
                            "Unsupported mouse button.",
                            text("OK"), () -> {
                                list.screen.setOverlay(null);
                                setListening(false);
                        }, text(""), () -> {}, null, list.screen
                        );
                        return true;
                    }
                    onChange(newKey);
                    return true;
                }

                protected void onChange(String newKey) {
                    set(newKey);
                    if (!entry.isNew) list.saveFocused(entry);
                    else outputWidget().setFocused(true);
                    setListening(false);
                }

                @Override
                public void setListening(boolean listening) {
                    super.setListening(listening);
                    entry.setInterceptsInput(listening);                    
                }

                @Override
                public boolean isHovered() {
                    return this.isListening() || super.isHovered();
                }
            };
            if (isNew) w.setListening(true);
            setInputWidget(w);
        }

        @Override
        protected void renderInputWidget(GuiGraphicsExtractor gge, int x, int y, int mX, int mY, float delta) {
            inputWidget().setPosition(x, y);
            inputWidget().extractRenderState(gge, mX, mY, delta);
        }        

    }    

    private final class AliasEntryList extends GenericEntryList<String> {
        public AliasEntryList(Minecraft mc, GenericEntryList.EntryConfig settings, DynamicListScreen screen) {
            super(mc, settings, screen);
        }

        @Override public Map<String, String> getConfig() { return ShortcutsConfig.getAliases();}
        @Override public boolean isEntryToggled(String key) { return ShortcutsConfig.isAliasToggled(key);}
        @Override public void removeEntry(String key) { ShortcutsConfig.removeAlias(key);}
        @Override public void toggleEntry(String key, boolean toggled) { ShortcutsConfig.toggleAlias(key, toggled);}
        @Override public void setEntry(String key, String v) { ShortcutsConfig.setAlias(key, v);}
        @Override public void onRefresh() { CommandAlias.refresh();}

        @Override
        protected Entry<String> createNewEntry(String in, String out, boolean isNew) {
            var entry = new GenericEntry(in, this, isNew);
            entry.initialize(in, out);
            return entry;
        }

        @Override
        public boolean isValidEntry(String key, String v) {
            return key.startsWith("/") && v.startsWith("/") &&
                !key.replaceFirst("/", "").isBlank() && !v.replaceFirst("/","").isBlank();
        }    

        @Override
        public String[] getGuideText() {
            return new String[] {"Alias", "Executed Command"};
        }

        @Override
        public String getInvalidMsg() {
            return "Both alias and command must\nstart with '/' and cannot be empty.\n ";
        }    

        @Override public String getAddButtonText() { return "+ Add Command";}
        @Override public String getDefaultInput() { return "/";}
        @Override public String getDefaultOutput() { return "/";}
    }

    private final class KeybindEntryList extends GenericEntryList<String> {
        public KeybindEntryList(Minecraft mc, GenericEntryList.EntryConfig settings, DynamicListScreen screen) {
            super(mc, settings, screen);
        }
        
        @Override
        protected GenericEntry createNewEntry(String in, String out, boolean isNew) {
            var entry = new KeybindEntry(in, this, isNew);
            entry.initialize(in, out);
            return entry;
        }

        @Override public Map<String, String> getConfig() { return ShortcutsConfig.getKeybinds();}
        @Override public boolean isEntryToggled(String key) { return ShortcutsConfig.isKeybindToggled(key);}
        @Override public void setEntry(String key, String v) { ShortcutsConfig.setKeybind(key, v);}
        @Override public void removeEntry(String key) { ShortcutsConfig.removeKeybind(key);}
        @Override public void toggleEntry(String key, boolean toggled) { ShortcutsConfig.toggleKeybind(key, toggled);}
        @Override public void onRefresh() { KeyShortcut.refresh();}

        @Override
        public boolean isValidEntry(String key, String v) {
            return !v.replaceFirst("/", "").isBlank() && !invalidKey(key);
        }

        @Override
        public String[] getGuideText() {
            return new String[] { "Keybind", "Executed Command" };
        }

        @Override
        public String getInvalidMsg() {
            return "Neither key nor command can be empty.";
        }

        @Override public String getAddButtonText() { return "+ Add Keybind";}
        @Override public String getDefaultInput() { return "NONE";}
        @Override public String getDefaultOutput() { return "/";}

        boolean invalidKey(String key) {
            return Keyboard.isInvalidKey(key) && !key.equalsIgnoreCase(FaButton.Keybind.PROMPT);
        }        
    }

    private final class ChatEntryList extends GenericEntryList<String> {
        public ChatEntryList(Minecraft mc, GenericEntryList.EntryConfig settings, DynamicListScreen screen) {
            super(mc, settings, screen);
        }

        @Override public Map<String, String> getConfig() { return ShortcutsConfig.getChat();}
        @Override public boolean isEntryToggled(String key) { return ShortcutsConfig.isChatToggled(key);}
        @Override public void toggleEntry(String key, boolean toggled) { ShortcutsConfig.toggleChat(key, toggled);}
        @Override public void setEntry(String key, String v) { ShortcutsConfig.setChat(key, v);}
        @Override public void removeEntry(String key) { ShortcutsConfig.removeChat(key);}
        @Override public void onRefresh() { ChatReplacement.refresh();}

        @Override
        protected Entry<String> createNewEntry(String in, String out, boolean isNew) {
            var entry = new GenericEntry(in, this, isNew);
            entry.initialize(in, out);
            return entry;
        }    

        @Override
        public boolean isValidEntry(String key, String v) { return !key.isBlank();}

        @Override
        public String[] getGuideText() {
            return new String[] {"Detected String", "Replaced in chat with:"};
        }

        @Override
        public String getInvalidMsg() {
            return "Detected string cannot be empty.";
        }

        @Override public String getAddButtonText() { return "+ New Replacement";}
        @Override public String getDefaultOutput() { return "";}
    }
}
