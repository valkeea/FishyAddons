package me.valkeea.fishyaddons.ui.list;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import me.valkeea.fishyaddons.ui.GuiUtil;
import me.valkeea.fishyaddons.vconfig.api.Config;
import me.valkeea.fishyaddons.vconfig.config.BackupManager;
import me.valkeea.fishyaddons.vconfig.config.BackupManager.Backup;
import me.valkeea.fishyaddons.vconfig.core.ConfigRegistry;
import me.valkeea.fishyaddons.vconfig.ui.layout.UIScaleCalculator;
import me.valkeea.fishyaddons.vconfig.ui.manager.ScreenManager;
import me.valkeea.fishyaddons.vconfig.ui.render.VCText;
import me.valkeea.fishyaddons.vconfig.ui.widget.FaButton;
import me.valkeea.fishyaddons.vconfig.ui.widget.VCButton;
import me.valkeea.fishyaddons.vconfig.ui.widget.VCLabelField;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

public class ConfigManager extends Screen {
    private static final String TITLE_TEXT = "Reset or Restore Settings";
    private static final int LIST_TOP = 100;

    private static float uiScale;
    private static int entryH;
    private static int btnW;
    private static int fieldH;
    private static int labelW;
    private static int filesW;

    private final List<Row> rows = new ArrayList<>();
    private int scrollOffset = 0;
    private int maxVisible = 1;

    private boolean confirmReset = false;
    private Backup confirmRestore = null;
    private String status = "";

    public ConfigManager() {
        super(Component.literal(TITLE_TEXT));
    }

    @Override
    protected void init() {
        rows.clear();
        this.clearWidgets();
        calcDimensions(UIScaleCalculator.calculateUIScaleLegacy());

        int listBottom = this.height - 60;
        int rowW = labelW + filesW + btnW * 2;
        int mid = this.width / 2;
        int left = mid - rowW / 2;        

        var resetBtn = new FaButton(
            mid - btnW * 2, 40, btnW * 4, btnH(),
            Component.literal(confirmReset ? "§cClick again to confirm reset" : "Reset all settings to defaults"),
            btn -> {
                if (!confirmReset) {
                    confirmReset = true;
                    confirmRestore = null;
                    status = "";
                } else {
                    confirmReset = false;
                    int count = Config.resetAllToDefaults();
                    status = "§aReset " + count + " settings to defaults. A backup of the previous state was saved.";
                }
                rebuildWidgets();
            }
        );
        resetBtn.setUIScale(uiScale);
        this.addRenderableWidget(resetBtn);

        for (var backup : BackupManager.list()) {
            var row = new Row(backup, left);
            rows.add(row);
            row.addToScreen();
        }

        maxVisible = Math.max(1, (listBottom - LIST_TOP) / entryH);
        scrollOffset = Math.clamp(scrollOffset, 0, Math.max(0, rows.size() - maxVisible));

        var backButton = new FaButton(
            this.width / 2 - btnW - btnW / 2, this.height - 40, btnW, btnH(),
            Component.literal("Back").withStyle(style -> style.withColor(0xFF808080)),
            btn -> ScreenManager.openConfigScreen()
        );
        backButton.setUIScale(uiScale);
        this.addRenderableWidget(backButton);

        var closeButton = new FaButton(
            this.width / 2 + btnW / 2, this.height - 40, btnW, btnH(),
            Component.literal("Close").withStyle(style -> style.withColor(0xFF808080)),
            btn -> this.onClose()
        );
        closeButton.setUIScale(uiScale);
        this.addRenderableWidget(closeButton);
    }

    private static int btnH() {
        return fieldH;
    }

    private static void calcDimensions(float scale) {
        uiScale = Math.clamp(scale, 0.7f, 1.2f);
        btnW = (int) (60 * uiScale);
        fieldH = (int) (20 * uiScale);
        entryH = (int) (28 * uiScale);
        labelW = (int) (200 * uiScale);
        filesW = (int) (220 * uiScale);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor gge, int mouseX, int mouseY, float delta) {
        super.extractRenderState(gge, mouseX, mouseY, delta);

        var title = VCText.header(TITLE_TEXT, Style.EMPTY.withBold(true));
        GuiUtil.drawScaledCenteredText(gge, this.font, title, this.width / 2, 15, 0xFF55FFFF, uiScale - 0.1f);

        if (!status.isEmpty()) {
            gge.centeredText(this.font, Component.literal(status), this.width / 2, 64, 0xFFFFFFFF);
        } else if (rows.isEmpty()) {
            gge.centeredText(this.font, Component.literal("§7No backups found."), this.width / 2, 64, 0xFFFFFFFF);
        } else {
            gge.centeredText(this.font, Component.literal("§7Available backups (newest first)"), this.width / 2, LIST_TOP - 20, 0xFFFFFFFF);
        }

        int y = LIST_TOP;
        for (int i = 0; i < rows.size(); i++) {
            boolean visible = i >= scrollOffset && i < scrollOffset + maxVisible;
            rows.get(i).setVisible(visible);
            if (visible) {
                rows.get(i).setY(y);
                y += entryH;
            }
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double hAmount, double vAmount) {
        if (rows.size() > maxVisible) {
            scrollOffset = Math.clamp(scrollOffset - (long) Math.signum(vAmount), 0, rows.size() - maxVisible);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, hAmount, vAmount);
    }

    private class Row {
        private final VCLabelField label;
        private final VCLabelField files;
        private final Button restoreBtn;

        Row(Backup backup, int x) {
            final int offScreenY = -1000;
            boolean confirming = backup.equals(confirmRestore);

            label = new VCLabelField(ConfigManager.this.font, x, offScreenY, labelW, fieldH, Component.literal("Backup"));
            label.setText(backup.label());
            label.setUIScale(uiScale);

            var fileStr = String.join(", ", backup.files().stream().map(this::cleanName).toList());
            files = new VCLabelField(ConfigManager.this.font, x + labelW, offScreenY, filesW, fieldH, Component.literal("Files"));
            files.setText("§7" + fileStr);
            files.setUIScale(uiScale);

            if (files.getTextWidth() > filesW) {
                var tt = Tooltip.create(Component.literal(fileStr));
                files.setTooltip(tt);
                files.setTooltipDelay(Duration.ofMillis(500));
            }

            restoreBtn = VCButton.createNavigationButton(
                x + labelW + filesW, offScreenY, btnW * 2, fieldH,
                Component.literal(confirming ? "§cConfirm" : "Restore"),
                btn -> onRestore(backup),
                uiScale - 0.1f
            );
        }

        void addToScreen() {
            ConfigManager.this.addRenderableWidget(label);
            ConfigManager.this.addRenderableWidget(files);
            ConfigManager.this.addRenderableWidget(restoreBtn);
        }

        void setY(int y) {
            label.setY(y);
            files.setY(y);
            restoreBtn.setY(y);
        }

        void setVisible(boolean visible) {
            label.setVisible(visible);
            files.setVisible(visible);
            restoreBtn.visible = visible;
        }

        void onRestore(Backup bu) {
            if (!bu.equals(confirmRestore)) {
                confirmRestore = bu;
                confirmReset = false;
                status = "";

            } else {
                confirmRestore = null;
                var restored = BackupManager.restore(bu);
                boolean success = !restored.isEmpty();
                if (success) {
                    ConfigRegistry.initializeConfigValues();
                    Minecraft.getInstance().reloadResourcePacks();
                }
                status = !success
                    ? "§cRestore failed. The backup may be unreadable or invalid."
                    : "§aRestored " + String.join(", ", restored) + ".";
            }
            
            rebuildWidgets();
        }

        String cleanName(String file) {
            return file.replaceAll("fishy|.json", "");
        }
    }
}
