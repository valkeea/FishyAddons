package me.valkeea.fishyaddons.util;

import java.util.Map;

import me.valkeea.fishyaddons.config.FishyConfig;
import me.valkeea.fishyaddons.config.Key;
import me.valkeea.fishyaddons.hud.elements.custom.InfoDisplay;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;

public class ModInfo {
    private static final int CLOSE_KEY = 88;
    private static final int COPY_LINK_KEY = 67;
    private static final boolean FOR_NEW = false;

    private static String infoId = "";
    private static String infoMessage = "";
    private static boolean showInfo = false;
    private static boolean wasPressed = false;
    private static boolean wasClosePressed = false;
    private static long displayStartTime = 0;
    private static final long MAX_DISPLAY_TIME = 30000;

    static {
        fetchInfo();
    }

    private static void fetchInfo() {
        try {
            String url = "https://gist.githubusercontent.com/valkeea/dff3a7ee868b0d4bed22bd300c0119f9/raw/modinfo.json";
            var conn = java.net.URI.create(url).toURL().openConnection();
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);
            conn.setRequestProperty("User-Agent", "FishyAddons");

            try (var in = conn.getInputStream();
                 var reader = new java.io.InputStreamReader(in)) {
                var gson = new com.google.gson.Gson();
                var type = new com.google.gson.reflect.TypeToken<Map<String, Object>>(){}.getType();
                Map<String, Object> info = gson.fromJson(reader, type);

                if (info == null || info.isEmpty() || info.size() > 100) {
                    throw new IllegalStateException("Invalid gist data");
                }

                var idObj = info.get("infoId");
                var msgObj = info.get("infoMessage");
                infoId = idObj != null ? String.valueOf(idObj) : "";
                infoMessage = msgObj != null ? String.valueOf(msgObj) : "";
                check();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void tick() {
        if (shouldShowInfo()) {
            var client = MinecraftClient.getInstance();
            var window = client.getWindow();

            if (displayStartTime > 0 && System.currentTimeMillis() - displayStartTime > MAX_DISPLAY_TIME) {
                hideInfo();
                return;
            }

            boolean closeKeyDown = InputUtil.isKeyPressed(window, CLOSE_KEY);
            if (closeKeyDown && !wasClosePressed) {
                hideInfo();
                return;
            }

            wasClosePressed = closeKeyDown;

            boolean copyKeyDown = InputUtil.isKeyPressed(window, COPY_LINK_KEY);
            if (wasPressed && !copyKeyDown) {
                client.keyboard.setClipboard("https://modrinth.com/project/QOUIa2cU");
                FishyNotis.ccNoti();
            }
            wasPressed = copyKeyDown;
        }
    }

    private static void hideInfo() {
        InfoDisplay.getInstance().hide();
        FishyConfig.setString(Key.INFO_ID, getInfoId());
        showInfo = false;
        displayStartTime = 0;
        wasClosePressed = false;
        wasPressed = false;
    }


    private static void check() {
        if (infoId == null || infoId.isEmpty()) return;

        String lastId = FishyConfig.getString(Key.INFO_ID, "");
        boolean foundId = lastId != null && !lastId.isEmpty();
        if (!foundId) {
            lastId = FOR_NEW ? "000000" : infoId;
            foundId = FOR_NEW;
            FishyConfig.setString(Key.INFO_ID, lastId);
        }

        if (foundId) {
            try {
                int infoNum = Integer.parseInt(infoId);
                int lastNum = Integer.parseInt(lastId);
                showInfo = infoNum > lastNum;
            } catch (NumberFormatException e) {
                showInfo = !infoId.equals(lastId);
            }
        } else {
            showInfo = false;
        }
        
        if (showInfo && displayStartTime == 0) {
            displayStartTime = System.currentTimeMillis();
        }
    }

    public static String getInfoId() {
        return infoId;
    }

    public static String getInfoMessage() {
        return infoMessage;
    }

    public static boolean shouldShowInfo() {
        return showInfo;
    }

    public static void forceHide() {
        hideInfo();
    }

    public static void forceShow() {
        showInfo = true;
        displayStartTime = System.currentTimeMillis();
        InfoDisplay.getInstance().show(getInfoMessage());
    }

    private ModInfo() {
        throw new UnsupportedOperationException("Utility class");
    }
}
