package me.valkeea.fishyaddons.feature.qol;

import java.util.List;

import me.valkeea.fishyaddons.config.FishyConfig;
import me.valkeea.fishyaddons.config.Key;
import me.valkeea.fishyaddons.mixin.ChatHudAccessor;
import me.valkeea.fishyaddons.util.FishyNotis;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.client.util.InputUtil;

public class CopyChat {
    private CopyChat() {}
    private static boolean isOn = true;
    private static boolean isNotiOn = true;
    public static boolean isOn() { return isOn; }
    public static boolean isNotiOn() { return isNotiOn; }

    public static void refresh() {
        isOn = FishyConfig.getState(Key.COPY_CHAT, true);
        isNotiOn = FishyConfig.getState(Key.COPY_NOTI, true);
    }

    public static void tryCopyChat(MinecraftClient client, double mouseX, double mouseY) {

        if (!(client.inGameHud.getChatHud() instanceof ChatHudAccessor chat)) return;

        int lineIdx = getLineIndex(chat, mouseX, mouseY);

        if (isValid(lineIdx, chat)) {

            var line = chat.getVisibleMessages().get(lineIdx);
            String text = extractVisible(line);
            List<ChatHudLine.Visible> visible = chat.getVisibleMessages();

            if (shouldCopyLine(client)) {
                toClipboard(client, text);
            } else {
                toClipboard(client, extractFull(visible, lineIdx));
            }
        }
    }

    public static void toClipboard(MinecraftClient client, String text) {
        client.keyboard.setClipboard(text);
        FishyNotis.ccNoti();
    }

    public static void toClipboard(String text) {
        var client = MinecraftClient.getInstance();
        if (client != null) {
            client.keyboard.setClipboard(text);
            FishyNotis.ccNoti();
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

        var ordered = line.content();
        var sb = new StringBuilder();

        ordered.accept((index, style, codePoint) -> {
            sb.appendCodePoint(codePoint);
            return true;
        });

        return sb.toString();
    }

    private static boolean shouldCopyLine(MinecraftClient client) {
        if (client.options == null) return false;
        var window = client.getWindow();
        int keyCode = client.options.sneakKey.getDefaultKey().getCode();
        return InputUtil.isKeyPressed(window, keyCode);
    }

    private static String extractFull(List<ChatHudLine.Visible> visible, int lineIdx) {
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
