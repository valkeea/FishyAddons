package me.valkeea.fishyaddons.util;

import me.valkeea.fishyaddons.config.FishyConfig;
import me.valkeea.fishyaddons.config.Key;
import me.valkeea.fishyaddons.hud.InfoDisplay;
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

    static {
        fetchInfo();
    }

    private static void fetchInfo() {
        try {
            String url = "https://gist.githubusercontent.com/valkeea/dff3a7ee868b0d4bed22bd300c0119f9/raw/modinfo.json";
            java.net.URLConnection conn = java.net.URI.create(url).toURL().openConnection();
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);
            conn.setRequestProperty("User-Agent", "FishyAddons");

            try (java.io.InputStream in = conn.getInputStream();
                 java.io.InputStreamReader reader = new java.io.InputStreamReader(in)) {
                com.google.gson.Gson gson = new com.google.gson.Gson();
                java.lang.reflect.Type type = new com.google.gson.reflect.TypeToken<java.util.Map<String, Object>>(){}.getType();
                java.util.Map<String, Object> info = gson.fromJson(reader, type);

                if (info == null || info.isEmpty() || info.size() > 100) {
                    throw new IllegalStateException("Invalid gist data");
                }

                Object idObj = info.get("infoId");
                Object msgObj = info.get("infoMessage");
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
            var handle = client.getWindow().getHandle();

            if (InputUtil.isKeyPressed(handle, CLOSE_KEY)) {
                InfoDisplay.getInstance().hide();
                FishyConfig.setString(Key.INFO_ID, getInfoId());
                showInfo = false;
            }

            boolean keyDown = InputUtil.isKeyPressed(handle, COPY_LINK_KEY);
            if (wasPressed && !keyDown) {
                client.keyboard.setClipboard("https://modrinth.com/project/QOUIa2cU");
                FishyNotis.ccNoti();
            }
            wasPressed = keyDown;
        }
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

    private ModInfo() {
        throw new UnsupportedOperationException("Utility class");
    }
}