package me.valkeea.fishyaddons.ui.list;

import java.util.Map;

import me.valkeea.fishyaddons.compat.McApi;
import me.valkeea.fishyaddons.feature.qol.ChatAlert;
import me.valkeea.fishyaddons.feature.qol.FishyPresets;
import me.valkeea.fishyaddons.tool.FishyMode;
import me.valkeea.fishyaddons.ui.screen.AlertEditScreen;
import me.valkeea.fishyaddons.util.text.Enhancer;
import me.valkeea.fishyaddons.vconfig.config.impl.AlertConfig;
import me.valkeea.fishyaddons.vconfig.config.impl.AlertConfig.AlertData;
import me.valkeea.fishyaddons.vconfig.ui.layout.Colors;
import me.valkeea.fishyaddons.vconfig.ui.manager.ScreenManager;
import me.valkeea.fishyaddons.vconfig.ui.render.VCText;
import me.valkeea.fishyaddons.vconfig.ui.widget.FaButton;
import me.valkeea.fishyaddons.vconfig.ui.widget.FaTextField;
import me.valkeea.fishyaddons.vconfig.ui.widget.VCTextField;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;

public class ChatAlerts extends DynamicListScreen {
    private static final String TITLE_TEXT = "Chat Alerts";
    private static final int BTNW = 40;
    private static final int DELBTNW = 20;
    private static final int FIELDW = 450;
    private static final int ELH = 20;
    private static final int OFFSET = 30;
    private static final int ENTRYH = 28;
    private static final int ENTRYW = FIELDW + BTNW * 2 + DELBTNW * 2;
    private static final int LISTY = OFFSET * 2;
    private static final int LISTEND = OFFSET * 2;

    private boolean isAdding;

    private AlertList list;
    private String searchQuery = "";

    public ChatAlerts() {
        super(VCText.header(TITLE_TEXT, null));      
    }

    @Override
    public boolean isAdding() {
        return isAdding;
    }

    @Override
    public void setAdding(boolean state) {
        isAdding = state;
    }   

    @Override
    protected void init() {       
        super.init();
        int searchY = LISTY - ELH;
        int bBtnY = this.height - ELH - OFFSET;

        var settings = createSettings();
        list = new AlertList(minecraft, settings, this);
        
        list.setPosition(settings.listX(), settings.listY());
        addRenderableWidget(list);

        int centerX = this.width / 2;
        var sf = new VCTextField(
            this.font, settings.listX() + settings.offset(),
            searchY, FIELDW + DELBTNW + BTNW * 2, ELH,
            text(searchQuery)
        );

        sf.setMaxLength(100);
        sf.setDrawsCustomBg(false);
        sf.setHint(styled("§oSearch...", Colors.DISABLED_GREY));
        sf.setValue(searchQuery);
        list.filter(searchQuery);
        sf.setResponder(s -> {
            list.setScrollAmount(0);
            list.filter(s);
            searchQuery = s;
        });
        this.addRenderableWidget(sf);

        int x = centerX - BTNW;
        var backButton = new FaButton(
            x, bBtnY, BTNW, ELH,
            text("Back"),
            btn -> ScreenManager.openConfigScreen() 
        );
        this.addRenderableWidget(backButton);

        x += BTNW;
        var closeButton = new FaButton(
            x, bBtnY, BTNW, ELH,
            text("Close"),
            btn -> this.onClose()
        );
        this.addRenderableWidget(closeButton);

        x += BTNW;
        addPresetButtons(x, bBtnY, BTNW, ELH,
            () -> FishyPresets.PresetType.ALERT,
            (suffix, override) -> {
                loadPreset(suffix, override);
                list.repopulate();
                list.onRefresh();
            }, this::savePreset);

        this.minecraft.execute(list::repopulate);        
    }

    private GenericEntryList.EntryConfig createSettings() {
        int listW = ENTRYW + OFFSET * 3;     
        int listX = (width - listW) / 2;
        int listH = height - LISTEND - LISTY;
        
        return new GenericEntryList.EntryConfig(
            listX, LISTY, listW, listH, OFFSET,
            ELH, ENTRYH, FIELDW, 0, BTNW, 0, true
        );
    }    

    @Override
    public void extractContent(GuiGraphicsExtractor gge, int mX, int mY, float delta) {
        super.extractContent(gge, mX, mY, delta);
        if (list.isEmpty()) extractInstructions(gge);
        else gge.centeredText(this.font, getTitle(), this.width / 2, 15, 0xFF55FFFF);
        renderPresets(gge, mX, mY);
    }

    private void extractInstructions(GuiGraphicsExtractor gge) {
        int y = this.height / 2;
        int x = this.width / 2;
        int lineH = 15;

        y += lineH * 2;
        String[] instructions = {
            TITLE_TEXT,
            " -§7Add strings you want to be alerted on.",
            " -§7After adding, click 'Edit' to configure optional title, chat msg and sound alarms.",
            " -§7You can share configs by dropping files in fishyaddons/preset.",
            " -§dIf you add a chat filter, the original message should be used as a trigger!",
        };
        for (String i : instructions) {
            var line = text(i);
            gge.centeredText(this.font, line.getString(),
                x, y, 0xFF55FFFF);
            y += lineH;
        }
    }

    private void loadPreset(String suffix, boolean override) {
        var map = FishyPresets.loadAlertPreset(suffix);
        if (map == null) return;

        if (override) list.getConfig().clear();
        list.getConfig().putAll(map);
        AlertConfig.setAlerts(map);

        if (override) list.setScrollAmount(0);
        list.repopulate();
    }     

    private void savePreset(String suffix) {
        FishyPresets.saveAlertPreset(suffix, AlertConfig.getChatAlerts());
    }

    protected class AlertEntry extends GenericEntryList.Entry<AlertData> {
        private AlertData data;

        public AlertEntry(String input, AlertData output, GenericEntryList<AlertData> list, boolean isNew) {
            super(list, input, isNew);
            this.data = output;
            createInputWidget(input);
            createButtons();
            setSize(list.getRowWidth(), list.settings.entryH());
        }

        void createInputWidget(String input) {
            var in = new FaTextField(minecraft.font, 0, 0, list.settings.inW(), list.settings.elH(), Component.empty());
            in.setMaxLength(256);
            in.set(input);
            if (input.length() > 90) in.setTooltip(getTooltipLines(input));
            if (isNew) in.setFocused(true);
            setInputWidget(in);
        }

        @Override
        public AlertData getOutput() {
            return this.data;
        }

        @Override
        protected void updateOutput(AlertData output) {
            this.data = output;
        }

        void createButtons() {
            var edit = new FaButton.Trigger(
                0, 0, list.settings.btnW(), list.settings.elH(),
                list.styled("Edit", FishyMode.getThemeColor()),
                b -> {
                    McApi.setScreen(new AlertEditScreen(getInput(), getOutput()));
                }
            ) {};
            edit.setTooltip(getDataTooltip());
            widgets.add(edit);

            var save = new FaButton(
                0, 0, list.settings.btnW(), list.settings.elH(),
                list.styled("Save", Colors.DISABLED_GREY),
                b ->  list.saveFocused(this)
            );
            widgets.add(save);

            var del = new FaButton(
                0, 0, list.settings.btnW() / 2, list.settings.elH(),
                list.styled("🗑", Colors.DISABLED_GREY),
                b -> delete()
            );
            del.setTooltip(Tooltip.create(styled("Delete Alert", Colors.DISABLED_GREY)));
            widgets.add(del);
            
            boolean toggled = data.isToggled();
            var toggle = new FaButton.Toggle(
                0, 0, list.settings.btnW() / 2, list.settings.elH(),
                b -> {
                    if (isNew) data.setToggled(!data.isToggled());
                    else toggle();
                },
                toggled
            ) {};
            widgets.add(toggle);
        }

        Tooltip getTooltipLines(String text) {
            int lineLength = 100;
            var t = Component.empty();
            for (int i = 0; i < text.length(); i += lineLength) {
                t = t.append(text(text.substring(i, Math.min(i + lineLength, text.length()))));
            }
            return Tooltip.create(t);
        }

        Tooltip getDataTooltip() {
            var title = Enhancer.hasFormattingCodes(data.getOnscreen())
                ? Enhancer.parseFormattedText(data.getOnscreen())
                : styled(data.getOnscreen(), data.getColor());

            var msg = (data.getMsg().isEmpty() ? "" : "\n§7Message:§8 " + data.getMsg()) + "\n";
            var styledTitle = Component.literal("§7Title: ").append(title);
            var soundId = (data.getSoundId().isEmpty() ? "" : "\n§7Sound ID:§8 " + data.getSoundId());

            var tt = Component.literal(msg).append(styledTitle).append(text(soundId
                + "\n§7Volume:§8 " + data.getVolume()
                + "\n§7Toggled:§8 " + data.isToggled()
                + (data.isStartsWith() ? "\n Exact Match" : "\nMatched Anywhere")));

            return Tooltip.create(tt);
        }

        @Override 
        public void extractContent(GuiGraphicsExtractor gge, int mX, int mY, boolean hovered, float delta) {
            this.setHovered(hovered);
            int x = getX();
            int y = getY();

            inputWidget().setPosition(x, y);
            x = x + list.settings.inW();
            int btnGap = list.settings.btnW();
            var editBtn = widgets.get(1);
            editBtn.setPosition(x, y);

            x += btnGap;
            var saveBtn = widgets.get(2);
            saveBtn.setPosition(x, y);

            x += btnGap;
            var delBtn = widgets.get(3);
            delBtn.setPosition(x, y);

            x += btnGap / 2;
            var toggleBtn = widgets.get(4);
            toggleBtn.setPosition(x, y);

            widgets.forEach(wg -> wg.extractRenderState(gge, mX, mY, delta));
        }

        @Override
        public boolean keyPressed(KeyEvent input) {
            if (saveOnEnter(input, inputWidget()) || unfocusOnEscape(input)) return true;
            var focused = findFirst(AbstractWidget::isFocused);
            return focused != null && focused.keyPressed(input);
        }
    }

    private final class AlertList extends GenericEntryList<AlertData> {
        public AlertList(Minecraft mc, GenericEntryList.EntryConfig settings, DynamicListScreen screen) {
            super(mc, settings, screen);
        }

        @Override public Map<String, AlertData> getConfig() { return AlertConfig.getChatAlerts();}
        @Override public boolean isEntryToggled(String key) { return AlertConfig.isChatAlertToggled(key);}
        @Override public void toggleEntry(String key, boolean toggled) { AlertConfig.toggleChatAlert(key, toggled);}
        @Override public void setEntry(String key, AlertData v) { AlertConfig.setChatAlert(key, v);}
        @Override public void removeEntry(String key) { AlertConfig.removeChatAlert(key);}
        @Override public void onRefresh() { ChatAlert.refresh(); }

        @Override
        protected Entry<AlertData> createNewEntry(String in, AlertData out, boolean isNew) {
            return new AlertEntry(in, out, this, isNew);
        }    

        @Override
        public boolean isValidEntry(String key, AlertData v) { return !key.isBlank();}

        @Override
        public String[] getGuideText() {
            return new String[] {""};
        }

        @Override
        public String getInvalidMsg() {
            return "Detected string cannot be empty.";
        }

        @Override public String getAddButtonText() { return "+ New Alert";}
        @Override public AlertData getDefaultOutput() { return new AlertData();}
    }
}
