package me.valkeea.fishyaddons.feature.qol;

import java.util.List;

import com.mojang.blaze3d.platform.InputConstants;

import me.valkeea.fishyaddons.mixin.ChatHudAccessor;
import me.valkeea.fishyaddons.util.FishyNotis;
import me.valkeea.fishyaddons.vconfig.annotation.VCListener;
import me.valkeea.fishyaddons.vconfig.annotation.VCModule;
import me.valkeea.fishyaddons.vconfig.api.BooleanKey;
import me.valkeea.fishyaddons.vconfig.api.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.chat.GuiMessage;

@VCModule
public class CopyChat {
    private CopyChat() {}
    private static boolean isOn = true;
    private static boolean isNotiOn = true;
    public static boolean isOn() { return isOn; }
    public static boolean isNotiOn() { return isNotiOn; }

    @VCListener({BooleanKey.COPY_CHAT, BooleanKey.COPY_NOTI})
    public static void refresh() {
        isOn = Config.get(BooleanKey.COPY_CHAT);
        isNotiOn = Config.get(BooleanKey.COPY_NOTI);
    }

    public static void tryCopyChat(Minecraft mc, double mouseX, double mouseY) {
        if (!(mc.gui.getChat() instanceof ChatHudAccessor chat)) return;

        int lineIdx = calcLineIndex(mc, chat, mouseX, mouseY);

        if (isValid(lineIdx, chat)) {

            var line = chat.getVisibleMessages().get(lineIdx);
            String text = extractVisible(line);
            List<GuiMessage.Line> visible = chat.getVisibleMessages();

            if (shouldCopyLine(mc)) {
                toClipboard(mc, text);
            } else {
                toClipboard(mc, extractFull(visible, lineIdx));
            }
        }
    }

    public static void toClipboard(Minecraft mc, String text) {
        mc.keyboardHandler.setClipboard(text);
        FishyNotis.ccNoti();
    }

    public static void toClipboard(String text) {
        Minecraft.getInstance().keyboardHandler.setClipboard(text);
        FishyNotis.ccNoti();
    }

    private static int calcLineIndex(
        Minecraft mc,
        ChatHudAccessor cha,
        double mouseX,
        double mouseY
    ) {

        double s = cha.invokeGetChatScale();
        int w = cha.invokeGetWidth();
        int ch = cha.invokeGetHeight();
        int lh = cha.invokeGetLineHeight();
        int wh = mc.getWindow().getGuiScaledHeight();
        
        int btm = wh - 40; // Match ChatHud's 40px offset
        int top = btm - (int)(ch * s);
        int lEdge = 4;
        int rEdge = lEdge + (int)(w * s);
        
        if (mouseX < lEdge || mouseX > rEdge || mouseY < top || mouseY > btm) {
            return -1; // Not in chat area
        }

        double scaledY = (btm - mouseY) / s; // Relative pos
        
        // Line index (0 = bottom visible line)
        int lineFromBottom = (int)(scaledY / lh);
        int scrolledLines = cha.getScrolledLines();
        
        return lineFromBottom + scrolledLines;
    }

    private static boolean isValid(int lineIdx, ChatHudAccessor cha) {
        return lineIdx >= 0 && lineIdx < cha.getVisibleMessages().size();
    }

    private static String extractVisible(GuiMessage.Line line) {

        var ordered = line.content();
        var sb = new StringBuilder();

        ordered.accept((index, style, codePoint) -> {
            sb.appendCodePoint(codePoint);
            return true;
        });

        return sb.toString();
    }

    private static boolean shouldCopyLine(Minecraft mc) {
        if (mc.options == null) return false;
        var window = mc.getWindow();
        int keyCode = mc.options.keyShift.getDefaultKey().getValue();
        return InputConstants.isKeyDown(window, keyCode);
    }

    private static String extractFull(List<GuiMessage.Line> visible, int lineIdx) {
        if (visible.isEmpty() || lineIdx < 0 || lineIdx >= visible.size()) return "";

        int start = lineIdx;
        int end = lineIdx;
        while (end >= 0 && !visible.get(end).endOfEntry()) {
            end--;
        }

        var sb = new StringBuilder();
        for (int i = start; i >= end; i--) {
            var ordered = visible.get(i).content();
            ordered.accept((index, style, codePoint) -> {
                sb.appendCodePoint(codePoint);
                return true;
            });
            if (i != end) sb.append('\n');
        }

        return sb.toString().strip();
    }
}
