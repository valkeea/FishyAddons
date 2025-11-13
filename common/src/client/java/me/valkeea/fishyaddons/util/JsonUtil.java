package me.valkeea.fishyaddons.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;

import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryOps;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;

public class JsonUtil {
    private JsonUtil() {}

    private static final Gson GSON = new Gson();
    private static final Logger LOGGER = LoggerFactory.getLogger("FishyAddons/JsonSerialization");

    /**
     * Serialize an ItemStack, preserving all component data
     */
    public static String serialize(ItemStack itemStack) {
        if (itemStack == null || itemStack.isEmpty()) {
            return "";
        }
        
        try {
            var client = MinecraftClient.getInstance();
            if (client.world == null) {
                LOGGER.debug("Cannot serialize ItemStack: client world is null");
                return "";
            }
            
            RegistryWrapper.WrapperLookup registries = client.world.getRegistryManager();
            RegistryOps<JsonElement> ops = RegistryOps.of(JsonOps.INSTANCE, registries);
            DataResult<JsonElement> result = ItemStack.CODEC.encodeStart(ops, itemStack);
            
            String serialized = result.result().map(GSON::toJson).orElse("");
            if (!serialized.isEmpty()) {
                LOGGER.debug("Successfully serialized ItemStack, length: {}", serialized.length());
            } else {
                LOGGER.warn("Serialization resulted in empty string");
            }
            return serialized;
        } catch (Exception e) {
            LOGGER.warn("Exception while serializing ItemStack: {}", e.getMessage());
            return "";
        }
    }

    /**
     * Deserialize an ItemStack, restoring all component data
     */
    public static ItemStack deserialize(String json) {
        if (json == null || json.isEmpty()) {
            return ItemStack.EMPTY;
        }
        
        try {
            var jsonElement = GSON.fromJson(json, JsonElement.class);
            var client = MinecraftClient.getInstance();
            if (client.world == null) {
                LOGGER.debug("Cannot deserialize ItemStack: client world is null");
                return ItemStack.EMPTY;
            }
            
            RegistryWrapper.WrapperLookup registries = client.world.getRegistryManager();
            RegistryOps<JsonElement> ops = RegistryOps.of(JsonOps.INSTANCE, registries);
            DataResult<ItemStack> result = ItemStack.CODEC.parse(ops, jsonElement);

            return result.result().orElse(ItemStack.EMPTY);

        } catch (Exception e) {
            LOGGER.warn("Exception while deserializing ItemStack: {}", e.getMessage());
            return ItemStack.EMPTY;
        }
    }

    /**
     * Serialize a Text component to JSON string
     */
    public static String serializeText(Text text) {
        if (text == null) {
            return "";
        }

        try {
            var client = MinecraftClient.getInstance();
            if (client.world == null) {
                LOGGER.debug("Cannot serialize Text: client world is null");
                return "";
            }

            RegistryWrapper.WrapperLookup registries = client.world.getRegistryManager();
            RegistryOps<JsonElement> ops = RegistryOps.of(JsonOps.INSTANCE, registries);
            DataResult<JsonElement> result = TextCodecs.CODEC.encodeStart(ops, text);

            String serialized = result.result().map(GSON::toJson).orElse("");
            if (!serialized.isEmpty()) {
                LOGGER.debug("Successfully serialized Text, length: {}", serialized.length());
            } else {
                LOGGER.warn("Text serialization resulted in empty string");
            }
            return serialized;
        } catch (Exception e) {
            LOGGER.warn("Exception while serializing Text: {}", e.getMessage());
            return "";
        }
    }

    /**
     * Deserialize a Text component from JSON string or plain text
     */
    public static Text deserializeText(String jsonOrPlain) {
        if (jsonOrPlain == null || jsonOrPlain.isEmpty()) {
            return Text.empty();
        }

        String trimmed = jsonOrPlain.trim();
        
        if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
            try {
                var json = GSON.fromJson(trimmed, JsonElement.class);
                var client = MinecraftClient.getInstance();
                if (client.world == null) {
                    LOGGER.debug("Cannot deserialize Text: client world is null, falling back to literal");
                    return Text.literal(jsonOrPlain);
                }

                RegistryWrapper.WrapperLookup registries = client.world.getRegistryManager();
                RegistryOps<JsonElement> ops = RegistryOps.of(JsonOps.INSTANCE, registries);
                DataResult<Text> result = TextCodecs.CODEC.parse(ops, json);
                
                Text parsed = result.result().orElse(null);
                if (parsed != null) {
                    LOGGER.debug("Successfully deserialized Text component");
                    return parsed;
                } else {
                    LOGGER.debug("Failed to parse as Text component, falling back to literal");
                    return Text.literal(jsonOrPlain);
                }
            } catch (Exception e) {
                LOGGER.debug("Exception while deserializing Text component: {}, falling back to literal", e.getMessage());
            }
        }

        return Text.literal(jsonOrPlain);
    }
}
