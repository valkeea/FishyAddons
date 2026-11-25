package me.valkeea.fishyaddons.ui.list;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.jetbrains.annotations.Nullable;

import com.mojang.blaze3d.platform.InputConstants;

import me.valkeea.fishyaddons.tool.FishyMode;
import me.valkeea.fishyaddons.ui.element.ChoicePopup;
import me.valkeea.fishyaddons.vconfig.ui.layout.Colors;
import me.valkeea.fishyaddons.vconfig.ui.render.RenderUtils;
import me.valkeea.fishyaddons.vconfig.ui.widget.FaButton;
import me.valkeea.fishyaddons.vconfig.ui.widget.ValueHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractContainerWidget;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

/**
 * An editable list keyed by string input, with values supplied by the owning config.
 * The owner should call {@link #repopulate()} after construction, once subclass state is initialized.
 * Entry implementations register a string-valued input widget and provide their output and layout.
 */
public abstract class GenericEntryList<T> extends AbstractContainerWidget {
    private final Map<String, Entry<T>> entryHolder = new LinkedHashMap<>();
    private final List<Entry<T>> children = new ArrayList<>();

    protected final DynamicListScreen screen;     
    protected final Minecraft mc;

    public static record EntryConfig(int listX, int listY, int listW, int listH, int offset,
        int elH, int entryH, int inW, int outW, int btnW, int gap, boolean scaled
    ) {}

    protected EntryConfig settings;

    private int rowLeft;
    private int rowWidth;

    private Entry<T> hovered;
    private String filterQuery = "";

    protected GenericEntryList(Minecraft mc, EntryConfig settings, DynamicListScreen screen) {
        super(settings.listX, settings.listY, settings.listW, settings.listH, CommonComponents.EMPTY,
            new ScrollbarSettings(null, null, null, 3, 32, 24 / 2, true)
        );
        this.mc = mc;
        this.screen = screen;        
        this.settings = settings;
        init();
    }

    private void init() {
        this.rowWidth = settings.listW - settings.offset * 2;
        this.rowLeft = this.getX() + settings.offset;
    }

    public void setSettings(EntryConfig settings) {
        this.settings = settings;
        setPosition(settings.listX, settings.listY);
        setSize(settings.listW, settings.listH);
        init();
        entryHolder.clear();
        repopulate();
    }

    public EntryConfig getSettings() {
        return this.settings;
    }
    
    @Override
    public final List<Entry<T>> children() {
        return Collections.unmodifiableList(this.children.stream().filter(Entry::isVisible).toList());
    }

    protected abstract Map<String, T> getConfig();
    protected abstract boolean isEntryToggled(String key);
    protected abstract void setEntry(String key, T v);
    protected abstract void removeEntry(String key);
    protected abstract void toggleEntry(String key, boolean toggled);
    protected abstract void onRefresh();

    protected abstract String[] getGuideText();
    protected abstract String getAddButtonText();
    protected abstract String getInvalidMsg();
    protected String getDefaultInput() { return "";}
    protected abstract T getDefaultOutput();    

    protected abstract boolean isValidEntry(String key, T v);

    private static final String DISCARD = "Discard Entry";
    private static final String CONTINUE = "Continue Editing";

    public int getRowWidth() {
        return this.rowWidth;
    }

    private int getFirstEntryY() {
        return this.getY() + 2;
    }

    private int getNextY() {
        int y = this.getFirstEntryY() - (int)this.scrollAmount();
        y += children.size() * settings.entryH;
        return y;
    }
    
    @Override
    protected int contentHeight() {
        return children().size() * settings.entryH + 4;
    }

    // -- Controls --

    @Override
    public int maxScrollAmount() {    
        return Math.max(0, children().size() * settings.entryH + 2 - settings.listH);
    }

    private void scrollToLast() {
        if (children().isEmpty()) return;
        Entry<T> e = children().get(children().size() - 1);
        int topDelta = e.getY() - this.getY() - 2;
        if (topDelta < 0) this.scroll(topDelta);

        int bottomDelta = this.getBottom() - e.getY() - e.getHeight() - 2;
        if (bottomDelta < 0) this.scroll(-bottomDelta);
    }

    private void scroll(int amount) {
        this.setScrollAmount(this.scrollAmount() + amount);
    }

    @Override
    public void setScrollAmount(double scrollAmount) {
        super.setScrollAmount(scrollAmount);
        this.reposition();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        for (Entry<T> e : this.children()) if (e.interceptsInput()) return e.mouseClicked(click, doubled);
        var r = super.mouseClicked(click, doubled);
        if (!r) this.setFocused(null);
        return r;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent click) {
        if (this.maxScrollAmount() == 0) return super.mouseReleased(click);
        if (isOverScrollbar(click.x(), click.y())) {
            int knobH = this.scrollerHeight();
            int knobY = this.scrollBarY();
            if (!(click.y() >= knobY && click.y() <= knobY + knobH)) {
                this.setScrollAmount(getClickScroll(click.y()));
            }
        }
        return super.mouseReleased(click);
    }

    private int getClickScroll(double mY) {
        int scrollAreaH = settings.listH;
        int clickY = (int)mY;
        int relativeScroll = clickY - this.getY();
        return Math.floorDiv(relativeScroll * this.maxScrollAmount(), scrollAreaH);
    }
    
    // -- Render --

    @Override
    public void extractWidgetRenderState(GuiGraphicsExtractor gge, int mX, int mY, float a) {
        int l = this.getX() - 1;
        int r = this.getRight() - settings.offset;
        this.hovered = this.isMouseOver(mX, mY) ? this.getEntryAt(mX, mY) : null;
        gge.blurBeforeThisStratum();
        this.extractBg(gge, l, r);
        this.extractText(gge);
        gge.enableScissor(l, this.getY(), r, this.getBottom());
        this.extractEntries(gge, mX, mY, a);
        gge.disableScissor();
        this.extractBorders(gge, l, r);
        this.extractScrollbar(gge, mX, mY);
    }

    protected void extractBorders(GuiGraphicsExtractor gge, int l, int r) {
        gge.horizontalLine(l, r, this.getY() - 2, Colors.TRANSPARENT);
        gge.horizontalLine(l, r, this.getBottom() + 1, Colors.TRANSPARENT);
        gge.horizontalLine(l, r, this.getY() - 1, Colors.SCROLLBAR_TRACK);
        gge.horizontalLine(l, r, this.getBottom(), Colors.SCROLLBAR_TRACK);
    }

    protected void extractBg(GuiGraphicsExtractor gge, int l, int r) {
        gge.fill(l, this.getY(), r,
            this.getBottom(), Colors.SUB_ENTRY_BG);        
        RenderUtils.frame(gge, l, this.getY() - settings.elH - 1,
            r - l, settings.elH, Colors.WHITE, Colors.NONE);
    }

    protected void extractText(GuiGraphicsExtractor gge) {      
        var lines = getGuideText();
        int y = this.getY() - 15;
        for (int i = 0; i < lines.length; i++) {
            gge.text(mc.font, text(lines[i]), rowLeft + i * (settings.inW + settings.gap), y, Colors.WHITE, false);
        }
    }

    protected void extractEntries(GuiGraphicsExtractor gge, int mX, int mY, float a) {
        for (Entry<T> e : this.children()) {
            if (e.getY() + e.getHeight() >= this.getY() && e.getY() <= this.getBottom()) {
                e.extractContent(gge, mX, mY, Objects.equals(this.hovered, e), a);
            }
        }
    }

    @Override
	protected void extractScrollbar(GuiGraphicsExtractor gge, int mX, int mY) {
		int knobX = this.scrollBarX();
		int knobH = this.scrollerHeight();
		int knobY = this.scrollBarY();
        int color = FishyMode.getThemeColor();

		if (this.scrollable()) {
            gge.fill(knobX - 1, this.getY() - 1, knobX + this.scrollbarWidth() + 1,
                this.getBottom() + 1, Colors.SCROLLBAR_TRACK);
			gge.fill(knobX, knobY, knobX + this.scrollbarWidth(), knobY + knobH, color);
            gge.fill(knobX + 1, knobY + knobH, knobX + this.scrollbarWidth(),
                knobY + knobH + 1, 0xFF000000);            
		}
	}

    // -- Entry Management --

    protected abstract Entry<T> createNewEntry(String in, T out, boolean isNew);    

    protected void addEntry(Entry<T> e) {
        e.setWidth(rowWidth);
        int y = this.getNextY();
        e.setPosition(rowLeft, y);
        this.children.add(e);
    }

    protected void removeEntry(final Entry<T> e) {
        boolean removed = this.children.remove(e);
        if (removed) {
            this.reposition();
            if (e == this.hovered) this.hovered = null;
        }
    }

    protected void clearEntries() {
        this.children.clear();
        this.hovered = null;
    }

    public boolean isEmpty() {
        return this.children.size() <= 1;
    }

    public final void repopulate() {
        var config = getConfig();
        entryHolder.keySet().removeIf(e -> !config.containsKey(e));

        for (var e : config.entrySet()) {
            var existing = entryHolder.get(e.getKey());
            if (existing instanceof Entry<T> old) old.updateData(e.getKey(), e.getValue());
            else entryHolder.put(e.getKey(), createNewEntry(e.getKey(), e.getValue(), false));
        }

        this.clearEntries();
        entryHolder.values().forEach(this::addEntry);

        Entry<T> draft = null;
        if (screen.isAdding()) {
            draft = createNewEntry(getDefaultInput(), getDefaultOutput(), true);
            this.addEntry(draft);
            this.setFocused(draft);
        }
        this.addEntry(new AddEntry(this));
        applyFilter();
        setScrollAmount(scrollAmount());
        if (draft != null && draft.isVisible()) scrollToLast();
    }

    private void reposition() {
        int y = this.getFirstEntryY() - (int)this.scrollAmount();
        for (var e : this.children()) {
            e.setPosition(rowLeft, y);
            y += settings.entryH;
            e.setWidth(rowWidth);
        }
    }

    public void filter(String query) {
        Objects.requireNonNull(query, "query");
        filterQuery = query.toLowerCase(Locale.ROOT).trim();
        applyFilter();
        setScrollAmount(scrollAmount());
    }    

    private void applyFilter() {
        for (var e : children) {
            e.setVisible(e.getInput().toLowerCase(Locale.ROOT).contains(filterQuery));
        }
    }

    protected final @Nullable Entry<T> getEntryAt(double posX, double posY) {
        for (Entry<T> e : this.children()) if (e.isMouseOver(posX, posY)) return e;
        return null;
    }

    protected void saveFocused(Entry<T> e) {
        saveFocused(e, false);
    }

    private void saveFocused(Entry<T> e, boolean overwrite) {
        if (e == null) return;

        String newIn = e.getInput();
        T newOut = e.getOutput();
        if (newOut instanceof String s && s.length() > 255) {
            overflowPopup(e);
            return;
        }

        if (!isValidEntry(newIn, newOut)) {
            invalidPopup(e);
            return;
        }

        boolean keyChanged = !e.isNew && !newIn.equals(e.savedInput);
        if (!overwrite && getConfig().containsKey(newIn) && (e.isNew || keyChanged)) {
            dupePopup(e, e.isNew);
            return;
        }

        if (!e.isNew) e.unfocus();
        boolean toggled = !e.isNew && isEntryToggled(e.savedInput);
        if (keyChanged) removeEntry(e.savedInput);
        setEntry(newIn, newOut);
        if (keyChanged) toggleEntry(newIn, toggled);
        if (e.isNew && e.draftToggled != null) toggleEntry(newIn, e.draftToggled);

        e.updateData(newIn, newOut);
        e.isNew = false;

        screen.setAdding(false);
        repopulate();
    }

    // -- Util --

    protected void overflowPopup(Entry<T> e) {
        ChoicePopup.set(
            "Output too long!",
            text(CONTINUE), () -> screen.setOverlay(null),
            text(DISCARD), () -> { 
                removeEntry(e);
                screen.setAdding(false);
                repopulate();
                screen.setOverlay(null);
            }, null, screen
        );
    }

    protected void dupePopup(Entry<T> e, boolean isNew) {
        var newIn = e.getInput();
        ChoicePopup.set(
            "Entry with key '" + newIn + "' already exists!",
            text("Overwrite Existing"), () -> {
                saveFocused(e, true);
                screen.setOverlay(null); },
            text(isNew ? DISCARD : CONTINUE), () -> {
                if (isNew) {
                    e.unfocus();
                    removeEntry(e);
                    screen.setAdding(false);
                    repopulate();
                }
                screen.setOverlay(null);
                
            }, null, screen
        );
    }

    protected void invalidPopup(Entry<T> e) {
        ChoicePopup.set(
            "Invalid entry!\n" + getInvalidMsg(),
            text(CONTINUE), () -> screen.setOverlay(null),
            text(DISCARD), () -> { 
                removeEntry(e);
                screen.setAdding(false);
                repopulate();
                screen.setOverlay(null);
            }, null, screen);
    }

    @Override
    public void updateWidgetNarration(NarrationElementOutput builder) {/**No-op */}

    public abstract static class Entry<T> implements LayoutElement, GuiEventListener {
        public final GenericEntryList<T> list;
        public final List<AbstractWidget> widgets = new ArrayList<>();

        protected boolean isNew;
        protected boolean visible = true;       
        private String savedInput;
        private Boolean draftToggled;
        private AbstractWidget inputWidget;
        private ValueHolder inputValue;

        private int x;
        private int y;
        private int h;
        private int w;

        private boolean hovered;
        private boolean interceptsInput;

        protected Entry(GenericEntryList<T> list, String input, boolean isNew) {
            this.list = list;
            this.savedInput = input;
            this.isNew = isNew;
        }

        /** Registers the string input used by saving, filtering and keyboard navigation. */
        protected final <W extends AbstractWidget & ValueHolder> void setInputWidget(W input) {
            this.inputWidget = input;
            this.inputValue = input;
            widgets.add(input);
        }

        protected final AbstractWidget inputWidget() {
            return inputWidget;
        }

        protected final void delete() {
            unfocus();
            if (isNew) list.screen.setAdding(false);
            else list.removeEntry(savedInput);
            list.repopulate();
        }

        protected final void toggle() {
            unfocus();
            if (isNew) {
                boolean current = draftToggled != null ? draftToggled : list.isEntryToggled(getInput());
                draftToggled = !current;
            } else {
                list.toggleEntry(savedInput, !list.isEntryToggled(savedInput));
            }
        }

        protected final boolean saveOnEnter(KeyEvent input, AbstractWidget wg) {
            if ((input.key() == InputConstants.KEY_RETURN || input.key() == InputConstants.KEY_NUMPADENTER)
                    && wg.isFocused()) {
                wg.setFocused(false);
                list.saveFocused(this);
                return true;
            }
            return false;
        }

        protected final boolean unfocusOnEscape(KeyEvent input) {
            if (input.key() != InputConstants.KEY_ESCAPE) return false;
            var focused = findFirst(AbstractWidget::isFocused);
            if (focused == null) return false;
            focused.setFocused(false);
            return true;
        }

        public void setSize(int w, int h) {
            this.w = w;
            this.h = h;
        }
        
        public void setHovered(boolean hovered) {
            this.hovered = hovered;
            if (hovered) findFirst(wg -> wg.isHovered());
        }

        public void setInterceptsInput(boolean interceptsInput) {
            this.interceptsInput = interceptsInput;
        }

        public void setVisible(boolean visible) {
            this.visible = visible;
        }

        @Override
        public void setPosition(int x, int y) {           
            this.x = x;
            this.y = y;
        }

        @Override public void setX(int x) { this.x = x;}
        @Override public void setY(int y) { this.y = y;}
        @Override public int getY() { return y;}
        @Override public int getX() { return x;}        
        @Override public int getHeight() { return h;}
        @Override public int getWidth() { return w;}
        @Override public boolean isFocused() { return list.getFocused() == this;}

        protected void unfocus() {
            visitWidgets(wg -> wg.setFocused(false));
        }

        protected boolean isHovered() { return hovered;}
        protected boolean isVisible() { return visible;}
        protected boolean interceptsInput() { return interceptsInput;}
        protected void setWidth(int w) { this.w = w;}

        @Override
        public boolean isMouseOver(double x, double y) {
            return this.getRectangle().containsPoint((int)x, (int)y);
        }

        @Override 
        public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
            this.visitWidgets(e -> onVisit(e, click, doubled));
            return true;
        }        

        protected void onVisit(AbstractWidget wg, MouseButtonEvent click, boolean doubled) {
            if (!wg.isHovered()) {
                wg.setFocused(false);
            } else wg.mouseClicked(click, doubled);
        }

        protected AbstractWidget findFirst(Predicate<AbstractWidget> condition) {
            for (AbstractWidget wg : widgets) {
                if (condition.test(wg)) return wg;
            }
            return null;
        }

        @Override 
        public ScreenRectangle getRectangle() {
            return new ScreenRectangle(this.getX(), this.getY(), this.getWidth(), this.getHeight());
        }

        @Override 
        public void visitWidgets(Consumer<AbstractWidget> visitor) {
            for (AbstractWidget wg : widgets) visitor.accept(wg);
        }           

        @Override
        public boolean charTyped(CharacterEvent chr) {
            var wg = findFirst(e -> e.isFocused());
            return wg != null && wg.charTyped(chr);
        }

        @Override 
        public void setFocused(boolean focused) {
            if (focused) visitWidgets(wg -> {
                if (wg != null && wg.isHovered()) wg.setFocused(true);
            });
            else unfocus();
        }        

        @SuppressWarnings("java:S1172")
        public void extractContent(GuiGraphicsExtractor gge, int mX, int mY, boolean hovered, float delta) {
            this.setHovered(hovered);
        }

        protected String getInput() { return inputValue.get(); }
        protected abstract T getOutput();

        protected final void updateData(String input, T output) {
            inputValue.update(input);
            savedInput = input;
            updateOutput(output);
        }

        protected abstract void updateOutput(T output);
    }

    private final class AddEntry extends Entry<T> {
        AddEntry(GenericEntryList<T> list) {
            super(list, "", false);

            var addBtn = new FaButton.Trigger(
                rowLeft, -20, settings.inW, settings.elH,
                screen.isAdding()
                ? styled("Cancel", 0xFFFF8080)
                : styled(getAddButtonText(), 0xFFCCFFCC),
                b -> onButtonClick()
            ) {};

            addBtn.setTooltip(
                screen.isAdding()
                ? Tooltip.create(text("This will delete your draft."))
                : null
            );
            
            widgets.add(addBtn);
            this.setSize(settings.inW, settings.entryH);
        }

        void onButtonClick() {
            screen.setAdding(!screen.isAdding());
            list.repopulate();
        }

        @Override
        public void extractContent(GuiGraphicsExtractor gge, int mX, int mY, boolean hovered, float delta) {
            this.setHovered(hovered);
            widgets.get(0).setPosition(rowLeft, getY());
            widgets.get(0).extractRenderState(gge, mX, mY, delta);
        }

        @Override
        public void visitWidgets(final Consumer<AbstractWidget> visitor) {
            visitor.accept(widgets.get(0));
        }

        @Override
        public boolean shouldTakeFocusAfterInteraction() {
            return false;
        }        

        @Override public void setFocused(boolean focused) {/** No-op */}
        @Override protected void updateOutput(T output) {/** No-op */}
        @Override public String getInput() { return ""; }
        @Override public T getOutput() { return null; }
    }    

    protected Component text(String s) {
        return Component.literal(s);
    }

    protected Component styled(String s, int color) {
        return Component.literal(s).withStyle(style -> style.withColor(color));
    }    
}
