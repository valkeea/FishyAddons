package me.valkeea.fishyaddons.handler;

import java.util.List;

import me.valkeea.fishyaddons.config.FishyConfig;
import me.valkeea.fishyaddons.mixin.ChatHudAccessor;
import me.valkeea.fishyaddons.util.FishyNotis;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.OrderedText;

public class CopyChat {
    private CopyChat() {}
    private static boolean isOn = true;
    private static boolean isNotiOn = true;
    public static boolean isOn() { return isOn; }
    public static boolean isNotiOn() { return isNotiOn; }

    public static void refresh() {
        isOn = FishyConfig.getState("copyChat", true);
        isNotiOn = FishyConfig.getState("ccNoti", true);
    }

    public static void tryCopyChat(double mouseX, double mouseY) {
        MinecraftClient client = MinecraftClient.getInstance();
        ChatHud chatHud = client.inGameHud.getChatHud();
        ChatHudAccessor accessor = (ChatHudAccessor) chatHud;
        int lineIdx = getLineIndex(accessor, mouseX, mouseY);

        if (isValid(lineIdx, accessor)) {
            ChatHudLine.Visible line = accessor.getVisibleMessages().get(lineIdx);
            String text = extractVisible(line);
            List<ChatHudLine.Visible> visible = accessor.getVisibleMessages();

            if (shouldCopyLine(client)) {
                client.keyboard.setClipboard(text);
                FishyNotis.ccNoti();
            } else {
                String fullVisible = extractFull(visible, lineIdx);
                client.keyboard.setClipboard(fullVisible);
                FishyNotis.ccNoti();
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

    private static String extractVisible(ChatHudLine.Visible line) {
        OrderedText ordered = line.content();
        StringBuilder sb = new StringBuilder();       
        ordered.accept((index, style, codePoint) -> {
            sb.appendCodePoint(codePoint);
            return true;
        });      
        return sb.toString();
    }

    private static boolean shouldCopyLine(MinecraftClient client) {
        if (client.options == null) return false;
        long handle = client.getWindow().getHandle();
        int keyCode = client.options.sneakKey.getDefaultKey().getCode();
        return InputUtil.isKeyPressed(handle, keyCode);
    }


    private static String extractFull(List<ChatHudLine.Visible> visible, int lineIdx) {
        if (visible.isEmpty() || lineIdx < 0 || lineIdx >= visible.size()) return "";

        int start = lineIdx;
        int end = lineIdx;
        while (end >= 0 && !visible.get(end).endOfEntry()) {
            end--;
        }

        StringBuilder sb = new StringBuilder();
        for (int i = start; i >= end; i--) {
            OrderedText ordered = visible.get(i).content();
            ordered.accept((index, style, codePoint) -> {
                sb.appendCodePoint(codePoint);
                return true;
            });
            if (i != end) sb.append('\n');
        }
        return sb.toString().strip();
    }
}