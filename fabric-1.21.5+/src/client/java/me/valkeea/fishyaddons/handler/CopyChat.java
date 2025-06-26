package me.valkeea.fishyaddons.handler;

import me.valkeea.fishyaddons.config.FishyConfig;
import me.valkeea.fishyaddons.mixin.ChatHudAccessor;
import me.valkeea.fishyaddons.util.FishyNotis;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.OrderedText;

import java.util.List;

public class CopyChat {
    private CopyChat() {}
    private static boolean isOn = true;
    public static boolean isOn() {
        return isOn;
    }

    public static void refresh() {
        isOn = FishyConfig.getState("copyChat", true);
    }

    public static void tryCopyChat(double mouseX, double mouseY) {
        MinecraftClient client = MinecraftClient.getInstance();
        ChatHud chatHud = client.inGameHud.getChatHud();
        ChatHudAccessor accessor = (ChatHudAccessor) chatHud;
        int lineIdx = getLineIndex(accessor, mouseX, mouseY);

        if (isValid(lineIdx, accessor)) {
            ChatHudLine.Visible line = accessor.getVisibleMessages().get(lineIdx);
            String text = extractVisibleLineText(line);

            List<ChatHudLine.Visible> visible = accessor.getVisibleMessages();

            if (shouldCopyFullMsg(client)) {
                String fullVisible = extractFull(visible, lineIdx);
                client.keyboard.setClipboard(fullVisible);
            } else {
                client.keyboard.setClipboard(text);
                FishyNotis.sendCopyConfirmation();
            }
        }
    }

    private static int getLineIndex(ChatHudAccessor accessor, double mouseX, double mouseY) {
        double chatLineX = accessor.invokeToChatLineX(mouseX);
        double chatLineY = accessor.invokeToChatLineY(mouseY);
        return accessor.invokeGetMessageLineIndex(chatLineX, chatLineY);
    }

    private static boolean isValid(int lineIdx, ChatHudAccessor accessor) {
        return lineIdx >= 0 && lineIdx < accessor.getVisibleMessages().size();
    }

    private static String extractVisibleLineText(ChatHudLine.Visible line) {
        OrderedText ordered = line.content();
        StringBuilder sb = new StringBuilder();
        ordered.accept((index, style, codePoint) -> {
            sb.appendCodePoint(codePoint);
            return true;
        });
        return sb.toString();
    }

    private static boolean shouldCopyFullMsg(MinecraftClient client) {
        if (client.options == null) return false;
        long handle = client.getWindow().getHandle();
        int keyCode = client.options.sneakKey.getDefaultKey().getCode();
        boolean pressed = InputUtil.isKeyPressed(handle, keyCode);
        return pressed;
    }


    private static String extractFull(List<ChatHudLine.Visible> visible, int lineIdx) {
        if (visible.isEmpty() || lineIdx < 0 || lineIdx >= visible.size()) return "";

        // Walk backwards to the previous endOfEntry or start
        int start = lineIdx;
        while (start > 0 && !visible.get(start - 1).endOfEntry()) {
            start--;
        }
        // Prevent skipping single-line messages
        if (start != lineIdx && visible.get(start).endOfEntry() && start < visible.size() - 1) {
            start++;
        }

        // Walk forward to the next endOfEntry
        int end = lineIdx;
        while (end + 1 < visible.size() && !visible.get(end).endOfEntry()) {
            end++;
        }

        // Collect all lines in reverse order
        StringBuilder sb = new StringBuilder();
        for (int i = end; i >= start; i--) {
            OrderedText ordered = visible.get(i).content();
            ordered.accept((index, style, codePoint) -> {
                sb.appendCodePoint(codePoint);
                return true;
            });
            if (i != start) sb.append('\n');
        }
        return sb.toString().strip();
    }
}