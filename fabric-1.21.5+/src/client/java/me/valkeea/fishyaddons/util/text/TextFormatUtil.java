package me.valkeea.fishyaddons.util.text;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.serialization.DataResult;
import net.minecraft.client.MinecraftClient;
import net.minecraft.registry.RegistryOps;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;
import com.mojang.serialization.JsonOps;

public class TextFormatUtil {
    private TextFormatUtil() {}

    private static final Gson GSON = new Gson();

    public static String serialize(Text text) {
        if (text == null) return "";
        RegistryWrapper.WrapperLookup registries = MinecraftClient.getInstance().world.getRegistryManager();
        RegistryOps<JsonElement> ops = RegistryOps.of(JsonOps.INSTANCE, registries);
        DataResult<JsonElement> result = TextCodecs.CODEC.encodeStart(ops, text);
        return result.result().map(GSON::toJson).orElse("");
    }

    public static Text deserialize(String jsonOrPlain) {
        if (jsonOrPlain == null || jsonOrPlain.isEmpty()) return Text.empty();
        String trimmed = jsonOrPlain.trim();
        if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
            try {
                JsonElement json = GSON.fromJson(trimmed, JsonElement.class);
                RegistryWrapper.WrapperLookup registries = MinecraftClient.getInstance().world.getRegistryManager();
                RegistryOps<JsonElement> ops = RegistryOps.of(JsonOps.INSTANCE, registries);
                DataResult<Text> result = TextCodecs.CODEC.parse(ops, json);
                return result.result().orElseGet(() -> Text.literal(jsonOrPlain));
            } catch (Exception e) {
                // Fallback to plain if invalid
            }
        }
        return Text.literal(jsonOrPlain);
    }
}