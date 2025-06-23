package me.valkeea.fishyaddons.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.text.Text;

public class TextFormatUtil {
    private TextFormatUtil() {}

    // Serialize a Text object to JSON for config storage
    public static String serialize(Text text) {
        RegistryWrapper.WrapperLookup registries = MinecraftClient.getInstance().world.getRegistryManager();
        return Text.Serialization.toJsonString(text, registries);
    }

    // Deserialize from JSON (or fallback to plain text if not JSON)
    public static Text deserialize(String jsonOrPlain) {
        if (jsonOrPlain == null || jsonOrPlain.isEmpty()) return Text.empty();
        if (jsonOrPlain.trim().startsWith("{") || jsonOrPlain.trim().startsWith("[")) {
            try {
                RegistryWrapper.WrapperLookup registries = MinecraftClient.getInstance().world.getRegistryManager();
                return Text.Serialization.fromJson(jsonOrPlain, registries);
            } catch (Exception e) {
                // Fallback to plain text if JSON is invalid
            }
        }
        return Text.literal(jsonOrPlain);
    }
}