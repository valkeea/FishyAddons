package me.valkeea.fishyaddons.hud.elements.interactive;

import java.util.ArrayList;
import java.util.List;

import me.valkeea.fishyaddons.event.EventPhase;
import me.valkeea.fishyaddons.event.EventPriority;
import me.valkeea.fishyaddons.event.impl.FaEvents;
import me.valkeea.fishyaddons.hud.base.InteractiveHudElement;
import me.valkeea.fishyaddons.hud.core.HudButtonManager;
import me.valkeea.fishyaddons.hud.core.HudDrawer;
import me.valkeea.fishyaddons.hud.core.HudElementState;
import me.valkeea.fishyaddons.tracker.collection.ActiveDisplay;
import me.valkeea.fishyaddons.tracker.collection.CollectionData;
import me.valkeea.fishyaddons.tracker.collection.CollectionTracker;
import me.valkeea.fishyaddons.tracker.collection.CraftingRecipes;
import me.valkeea.fishyaddons.tracker.collection.GoalManager;
import me.valkeea.fishyaddons.tracker.collection.RecipeScanner;
import me.valkeea.fishyaddons.tracker.collection.VisibilityManager;
import me.valkeea.fishyaddons.util.text.Color;
import me.valkeea.fishyaddons.util.text.StringUtils;
import me.valkeea.fishyaddons.vconfig.api.BooleanKey;
import me.valkeea.fishyaddons.vconfig.api.IntKey;
import me.valkeea.fishyaddons.vconfig.ui.widget.dropdown.VCToggleMenu;
import me.valkeea.fishyaddons.vconfig.ui.widget.dropdown.item.ToggleMenuItem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

public class CollectionDisplay extends InteractiveHudElement {
    
    private static CollectionDisplay instance = null;

    private VCToggleMenu toggleMenu;
    private VCToggleMenu goalToggleMenu;
    
    // Managers for data and caching
    private final VisibilityManager visibilityManager = VisibilityManager.getInstance();
    private final GoalManager goalManager = GoalManager.getInstance();
    private final ActiveDisplay displayCache = ActiveDisplay.getInstance();

    // --- Setup ---
    
    private CollectionDisplay() {
        super(
            BooleanKey.HUD_COLLECTION_ENABLED,
            "Collection Tracker",
            20, 280,
            12,
            0xFF9BF3FF,
            false,
            true
        );
        createMenus();
        registerEvents();
    }
    
    public static CollectionDisplay getInstance() {
        if (instance == null) {
            instance = new CollectionDisplay();
        }
        return instance;
    }
    
    public static void refreshDisplay() {
        if (instance != null) {
            instance.invalidateCache();
            instance.displayCache.invalidateAll();
            instance.goalManager.invalidateProgressCache();
        }
    }

    private void createMenus() {
        int y = - 20 * getHudSize() / 12;
        this.toggleMenu = new VCToggleMenu(
            this::getToggleMenuItems,
            0, y, 120, 12,
            CollectionDisplay::refreshDisplay
        );
        this.toggleMenu.setVisible(false);
        registerToggleMenu(toggleMenu);
        
        this.goalToggleMenu = new VCToggleMenu(
            this::getGoalToggleMenuItems,
            0, y, 120, 12,
            CollectionDisplay::refreshDisplay
        );
        this.goalToggleMenu.setVisible(false);
        registerToggleMenu(goalToggleMenu);
    }

    private void registerEvents() {
        FaEvents.MOUSE_CLICK.register(event -> {
            if (mouseClicked(event.click) 
                || instance.handleMouseClick(event.click)) {
                event.setConsumed(true);
            }
        }, EventPriority.HIGH, EventPhase.PRE);    
        
        FaEvents.MOUSE_SCROLL.register(event -> {
            if (handleMouseScroll(event.mouseX, event.mouseY, event.vertical)) {
                event.setConsumed(true);
            }
        }, EventPriority.LOW, EventPhase.NORMAL);
    }
    
    @Override
    protected IntKey getMaxLinesConfigKey() {
        return IntKey.HUD_COLLECTION_LINES;
    }
    
    @Override
    protected int getTotalLineCount() {
        return displayCache.getVisibleCollections().size();
    }
    
    private List<ToggleMenuItem> getToggleMenuItems() {

        List<ToggleMenuItem> items = new ArrayList<>();
        
        var tracked = CollectionData.getAllTracked();
        for (String itemName : tracked.keySet()) {

            var displayName = enhanceItemName(itemName);
            items.add(new ToggleMenuItem() {
                @Override
                public String getId() {
                    return itemName;
                }

                @Override
                public String getDisplayName() {
                    return displayName;
                }

                @Override
                public boolean isEnabled() {
                    return !visibilityManager.isHidden(itemName);
                }

                @Override
                public void toggle() {
                    visibilityManager.toggleVisibility(itemName);
                    displayCache.invalidateCollections();
                    invalidateCache();
                }
            });
        }
        
        return items;
    }
    
    public boolean isHidden(String itemName) {
        return visibilityManager.isHidden(itemName);
    }
    
    private List<ToggleMenuItem> getGoalToggleMenuItems() {

        List<ToggleMenuItem> items = new ArrayList<>();
        
        var goals = CraftingRecipes.getAllGoals();
        if (goals.isEmpty()) return getGoalGuide(items);

        for (String goalName : goals.keySet()) {
            var displayName = enhanceItemName(goalName);
            items.add(new ToggleMenuItem() {
                @Override
                public String getId() {
                    return goalName;
                }

                @Override
                public String getDisplayName() {
                    return displayName;
                }

                @Override
                public boolean isEnabled() {
                    return goalName.equals(goalManager.getActiveGoal());
                }

                @Override
                public void toggle() {
                    goalManager.setActiveGoal(goalName);
                    invalidateCache();
                }
            });
        }
        
        return items;
    }

    private List<ToggleMenuItem> getGoalGuide(List<ToggleMenuItem> items) {

        var guide = List.of(
            "§bHow to add goals:",
            "§71. §3Use /recipe <item>.",
            "§72. §3Click the §b§l§n'Add Goal'§r§3 button.",
            "§73. §3If the item has any ingredients",
            "   §3with recipes, open those as well."
        );

        for (String line : guide) {
            if (line.trim().isEmpty()) continue;
            items.add(new ToggleMenuItem() {
                @Override
                public String getId() {
                    return "guide_" + line;
                }

                @Override
                public String getDisplayName() {
                    return line;
                }

                @Override
                public boolean isEnabled() {
                    return false;
                }

                @Override
                public void toggle() {/* None */}
            });
        }

        return items;
    }

    

    // --- Display Data ---

    @Override
    protected boolean shouldRender() {
        return CollectionTracker.isEnabled() && (!displayCache.getVisibleCollections().isEmpty() || isEditingMode());
    }

    @Override
    public List<Text> getDisplayLines(HudElementState state) {
        List<Text> lines = new ArrayList<>();
        
        if (isEditingMode() && displayCache.getVisibleCollections().isEmpty()) {
            lines.add(Text.literal("§dCollection Tracker"));
            return lines;
        }
        
        if (goalManager.hasActiveGoal() && !isEditingMode()) {
            double progress = goalManager.getProgress();
            if (progress > 0) {
                lines.add(getGoalProgressLine(progress, state.color));
            }
        }

        List<Text> formattedLines = displayCache.getFormattedCollectionLines(state.color);
        int startIdx = visibleLineIdx - 1;
        int endIdx = Math.min(startIdx + maxVisibleLines, formattedLines.size());
        
        if (startIdx < formattedLines.size()) {
            lines.addAll(formattedLines.subList(startIdx, endIdx));
        }
        
        lines.add(displayCache.getTimerLine(state.color));

        List<Text> promptLines = displayCache.getFormattedRecipePromptLines();
        if (!promptLines.isEmpty()) {
            lines.addAll(promptLines);
        }
        
        return lines;
    }
    
    private Text getGoalProgressLine(double progress, int color) {
        var goalName = enhanceItemName(goalManager.getActiveGoal());
        var progressStr = String.format("%.3fx", progress);
        
        return Text.literal("     " + goalName + " §8|§r ")
            .styled(s -> s.withColor(Color.brighten(color, 0.5f)))
            .append(style(progressStr, Color.mulRGB(color, 0.5f)));
    }

    // --- Rendering ---    

    @Override
    protected void drawCustomContent(HudDrawer drawer, MinecraftClient mc, HudElementState state) {
        if (!isEditingMode() && goalManager.hasActiveGoal()) {
            double progress = goalManager.getProgress();
            if (progress > 0) {
                drawGoalItemStack(drawer, mc.textRenderer.fontHeight);
            }
        }    
    }

    private void drawGoalItemStack(HudDrawer drawer, int fontHeight) {
        var activeGoal = goalManager.getActiveGoal();
        if (activeGoal == null) return;
        
        var goalRecipe = goalManager.getActiveGoalRecipe();
        if (goalRecipe == null) return;
        
        var stack = goalRecipe.getItemStack();

        if (stack.isEmpty()) return;

        drawer.drawItem(stack, 0, (-fontHeight * 2 / 3));
    }

    @Override
    protected void postRenderCustom(DrawContext context, MinecraftClient mc, 
                                    HudElementState state, int mouseX, int mouseY) {

        var screen = mc.currentScreen;

        if (!isEditingMode() && isInventoryOpen(mc)) {
        
            float scale = state.size / 12.0F;
            int hudX = state.x;
            int hudY = state.y;
            int buttonWidth = (int)(45 * scale);
            
            if (toggleMenu.isVisible() && screen != null) {
                int buttonSpacing = (int)(2 * scale);
                int toggleButtonX = hudX + 2 * (buttonWidth + buttonSpacing);
                int menuX = toggleButtonX;
                int menuY = hudY + 2;
                toggleMenu.setPosition(menuX, menuY, screen.height);
                toggleMenu.render(context, screen, mouseX, mouseY, scale);
            }
            
            if (goalToggleMenu.isVisible() && screen != null) {
                int buttonSpacing = (int)(2 * scale);
                int goalsButtonX = hudX + 3 * (buttonWidth + buttonSpacing);
                int menuX = goalsButtonX;
                int menuY = hudY + 2;
                goalToggleMenu.setPosition(menuX, menuY, screen.height);
                goalToggleMenu.render(context, screen, mouseX, mouseY, scale);
            }

        } else if (screen != null && (toggleMenu.isVisible() || goalToggleMenu.isVisible())) {
            toggleMenu.setVisible(false);
            goalToggleMenu.setVisible(false);
        }
    }

    @Override
    public boolean isHovered(double mouseX, double mouseY) {
        if (isAnyMenuVisible()) return true;
        return super.isHovered(mouseX, mouseY);
    }

    // --- Interaction ---

    private boolean handleMouseScroll(double mouseX, double mouseY, double scrollAmount) {
        if (handleMenuScroll(mouseX, mouseY, scrollAmount)) return true;
        if (isInventoryOpen(MinecraftClient.getInstance()) && shouldRender()) {
            return handleLineScroll(mouseX, mouseY, scrollAmount);
        }
        return false;
    }    

    @Override
    protected void setupButtons(HudButtonManager manager, HudElementState state) {

        var pauseLabel = CollectionTracker.isDownTiming() ? "Resume" : "Pause";
        
        manager.addButton(pauseLabel, btn -> togglePause());
        manager.addButton("Reset", btn -> resetAllCollections());

        manager.addButton("Toggle",
        btn -> {
            boolean wasVisible = toggleMenu.isVisible();
            toggleMenu.setVisible(!wasVisible);
            if (!wasVisible) {
                goalToggleMenu.setVisible(false);
            }
        });

        manager.addButton("Goals",
        btn -> {
            boolean wasGoalVisible = goalToggleMenu.isVisible();
            goalToggleMenu.setVisible(!wasGoalVisible);
            if (!wasGoalVisible) {
                toggleMenu.setVisible(false);
            }
        });
    }

    @Override
    protected boolean isLineClickable(int lineIndex, Text line) {
        var s = line.getString();
        return goalLine(s) || itemLine(s) || recipeLine(s);
    }

    @Override
    protected List<Text> getLineTooltip(int lineIndex, Text line) {
        var s = line.getString();
        if (isLineClickable(lineIndex, line)) {
            if (itemLine(s)) {
                return List.of(
                    Text.literal("[Click to hide from tracking]"),
                    Text.literal("§7Open '§3Toggle§7' dropdown to re-enable.")
                );
            } else if (recipeLine(s)) {
                return List.of(
                    Text.literal("[Click to open recipe]"),
                    Text.literal("§7Calculations are based on known craft recipes.")
                );
            } else if (goalLine(s)) {
                return ActiveDisplay.getGoalBreakdown();
            }
        }
        return List.of();
    }

    @Override
    protected List<Text> getTooltipForLine(int lineIndex, Text line) {
        if (lineIndex >= maxVisibleLines) {
            int hiddenCount = getTotalLineCount() - maxVisibleLines;
            if (hiddenCount > 0) {
                return List.of(Text.literal("§bScroll§7 to view hidden items (" + hiddenCount + " hidden)"));
            }
        }
        return super.getTooltipForLine(lineIndex, line);
    }    

    @Override  
    protected void handleLineClick(Text line) {

        var s = line.getString();

        if (recipeLine(s)) {
            RecipeScanner.openRecipeFor(s.split("§7- ")[1].trim());

        } else if (itemLine(s)) {
            var itemName = s.split("§8| ")[0]
            .trim()
            .toLowerCase();

            visibilityManager.toggleVisibility(itemName);
            invalidateCache();

        } else if (goalLine(s)) GoalManager.removeActive();
    }

    public boolean mouseClicked(Click click) {

        if (isInventoryOpen(MinecraftClient.getInstance()) && shouldRender()) {

            float scale = getCachedState().size / 12.0F;
            if (handleMenuClick(click, scale)) return true;
        }
        
        if (toggleMenu.isVisible()) toggleMenu.setVisible(false);
        if (goalToggleMenu.isVisible()) goalToggleMenu.setVisible(false);

        return false;
    }
    
    private boolean recipeLine(String s) {
        return s.contains("§7- ");
    }

    private boolean goalLine(String s) {
        return s.contains("§8|") && !itemLine(s);
    }

    private boolean itemLine(String s) {
        return s.contains("/h");
    }
    
    // --- Utilities ---

    @Override
    public void onCacheRefresh() {
        CollectionData.refreshDisplays();
        super.onCacheRefresh();
    }
    
    private void togglePause() {
        CollectionTracker.toggleDownTime();
        
        var manager = getButtonManager();
        if (manager != null) {
            manager.clear();
        }
        
        displayCache.reset();
        goalManager.invalidateProgressCache();
        invalidateCache();
    }
    
    private void resetAllCollections() {
        CollectionTracker.resetSession();
        displayCache.reset();
        goalManager.invalidateProgressCache();
        invalidateCache();
    }

    private String enhanceItemName(String itemName) {
        return StringUtils.capitalize(itemName);
    }

    private Text style(String s, int color) {
        return Text.literal(s).styled(style -> style.withColor(color));
    }
}
