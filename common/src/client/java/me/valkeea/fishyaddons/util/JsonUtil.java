package me.valkeea.fishyaddons.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;

import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.CustomModelDataComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryOps;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;
import net.minecraft.util.Identifier;

public class JsonUtil {
    private JsonUtil() {}

    private static final Gson GSON = new Gson();
    private static final Logger LOGGER = LoggerFactory.getLogger("FishyAddons/JsonSerialization");

    /**
     * Serialize a Text component to JSON string
     */
    public static String serializeText(Text text) {
        if (text == null) {
            return "";
        }

        try {
            var client = MinecraftClient.getInstance();
            if (client.world == null) return "";

            RegistryWrapper.WrapperLookup registries = client.world.getRegistryManager();
            RegistryOps<JsonElement> ops = RegistryOps.of(JsonOps.INSTANCE, registries);
            DataResult<JsonElement> result = TextCodecs.CODEC.encodeStart(ops, text);
            String serialized = result.result().map(GSON::toJson).orElse("");
            if (serialized.isEmpty()) {
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
                if (parsed == null) {
                    LOGGER.debug("Failed to parse as Text component, falling back to literal");
                    return Text.literal(jsonOrPlain);
                }
                return parsed;
            } catch (Exception e) {
                LOGGER.debug("Exception while deserializing Text component: {}, falling back to literal", e.getMessage());
            }
        }

        return Text.literal(jsonOrPlain);
    }

    private static final String COUNT = "count";
    private static final String PROFILE = "profile";
    private static final String CUSTOM_DATA = "customData";
    private static final String CUSTOM_MODEL_DATA = "customModelData";
    private static final String GLINT = "glint";
    private static final String DYED_COLOR = "dyedColor";

    /**
     * Serialize an ItemStack.
     * Stores: item ID, count, custom name, profile (for skulls), and custom model data.
     * 
     * @param itemStack The ItemStack to serialize
     * @return JSON string containing only display-relevant data
     */
    public static String serializeItemStack(ItemStack itemStack) {
        if (itemStack == null || itemStack.isEmpty()) return "";
        
        try {
            var client = MinecraftClient.getInstance();
            if (client.world == null) return "";
            
            JsonObject obj = new JsonObject();
            
            var itemId = Registries.ITEM.getId(itemStack.getItem());
            obj.addProperty("id", itemId.toString());
            obj.addProperty(COUNT, itemStack.getCount());
            
            var customName = itemStack.get(DataComponentTypes.CUSTOM_NAME);
            if (customName != null) obj.addProperty("name", customName.getString());
            
            var profile = itemStack.get(DataComponentTypes.PROFILE);
            if (profile != null) serialize(obj, profile, client);
            
            var customData = itemStack.get(DataComponentTypes.CUSTOM_DATA);
            if (customData != null) serialize(obj, customData, client);
            
            var customModelData = itemStack.get(DataComponentTypes.CUSTOM_MODEL_DATA);
            if (customModelData != null) serialize(obj, customModelData, client);
            
            if (itemStack.hasGlint()) obj.addProperty(GLINT, true);
            
            var dyedColor = itemStack.get(DataComponentTypes.DYED_COLOR);
            if (dyedColor != null) obj.addProperty(DYED_COLOR, dyedColor.rgb());
            
            String result = GSON.toJson(obj);
            LOGGER.debug("Serialized ItemStack for display, length: {}", result.length());
            return result;
            
        } catch (Exception e) {
            LOGGER.warn("Exception while serializing ItemStack for display: {}", e.getMessage());
            return "";
        }
    }  

    /**
     * Deserialize an ItemStack from display data.
     * 
     * @param json Compact JSON string from serializeForDisplay
     * @return ItemStack with visual properties for rendering
     */
    public static ItemStack deserializeItemStack(String json) {
        if (json == null || json.isEmpty()) {
            return ItemStack.EMPTY;
        }
        
        try {
            var client = MinecraftClient.getInstance();
            if (client.world == null) return ItemStack.EMPTY;
            
            JsonObject obj = GSON.fromJson(json, JsonObject.class);
            
            if (!obj.has("id")) return ItemStack.EMPTY;
            
            var itemIdStr = obj.get("id").getAsString();
            var itemId = Identifier.tryParse(itemIdStr);
            if (itemId == null) return ItemStack.EMPTY;
            
            if (!Registries.ITEM.containsId(itemId)) return ItemStack.EMPTY;
            
            Item item = Registries.ITEM.get(itemId);
            int count = obj.has(COUNT) ? obj.get(COUNT).getAsInt() : 1;
            var stack = new ItemStack(item, count);
            
            if (obj.has("name")) {
                var customName = obj.get("name").getAsString();
                stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal(customName));
            }
            
            if (obj.has(PROFILE)) setProfile(stack, obj.get(PROFILE), client);
            if (obj.has(CUSTOM_DATA)) setCustomData(stack, obj.get(CUSTOM_DATA), client);
            if (obj.has(CUSTOM_MODEL_DATA)) setCustomModelData(stack, obj.get(CUSTOM_MODEL_DATA), client);
            if (obj.has(GLINT) && obj.get(GLINT).getAsBoolean()) stack.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
            if (obj.has(DYED_COLOR)) {
                int rgb = obj.get(DYED_COLOR).getAsInt();
                stack.set(DataComponentTypes.DYED_COLOR, new net.minecraft.component.type.DyedColorComponent(rgb));
            }

            return stack;
            
        } catch (Exception e) {
            LOGGER.warn("Exception while deserializing ItemStack for display: {}", e.getMessage());
            return ItemStack.EMPTY;
        }
    }

    private static void serialize(JsonObject obj, ProfileComponent profile, MinecraftClient client) {
        serializeComponent(obj, profile, ProfileComponent.CODEC, PROFILE, client);
    }

    private static void serialize(JsonObject obj, NbtComponent customData, MinecraftClient client) {
        serializeComponent(obj, customData, NbtComponent.CODEC, CUSTOM_DATA, client);
    }

    private static void serialize(JsonObject obj, CustomModelDataComponent customModelData, MinecraftClient client) {
        serializeComponent(obj, customModelData, CustomModelDataComponent.CODEC, CUSTOM_MODEL_DATA, client);
    }    

    private static void setProfile(ItemStack stack, JsonElement json, MinecraftClient client) {
        deserializeComponent(stack, json, ProfileComponent.CODEC, DataComponentTypes.PROFILE, client);
    }

    private static void setCustomData(ItemStack stack, JsonElement json, MinecraftClient client) {
        deserializeComponent(stack, json, NbtComponent.CODEC, DataComponentTypes.CUSTOM_DATA, client);
    }

    private static void setCustomModelData(ItemStack stack, JsonElement json, MinecraftClient client) {
        deserializeComponent(stack, json, CustomModelDataComponent.CODEC, DataComponentTypes.CUSTOM_MODEL_DATA, client);
    }

    private static <T> void serializeComponent(
        JsonObject obj, 
        T component, 
        com.mojang.serialization.Codec<T> codec, 
        String key, 
        MinecraftClient client
    ) {
        try {
            RegistryWrapper.WrapperLookup registries = client.world.getRegistryManager();
            RegistryOps<JsonElement> ops = RegistryOps.of(JsonOps.INSTANCE, registries);
            DataResult<JsonElement> result = codec.encodeStart(ops, component);
            result.result().ifPresent(json -> obj.add(key, json));
        } catch (Exception e) {
            LOGGER.debug("Failed to serialize {}: {}", key, e.getMessage());
        }
    }

    private static <T> void deserializeComponent(
        ItemStack stack,
        JsonElement json,
        com.mojang.serialization.Codec<T> codec,
        net.minecraft.component.ComponentType<T> componentType,
        MinecraftClient client
    ) {
        try {
            RegistryWrapper.WrapperLookup registries = client.world.getRegistryManager();
            RegistryOps<JsonElement> ops = RegistryOps.of(JsonOps.INSTANCE, registries);
            DataResult<T> result = codec.parse(ops, json);
            result.result().ifPresent(value -> stack.set(componentType, value));
        } catch (Exception e) {
            LOGGER.debug("Failed to deserialize component: {}", e.getMessage());
        }
    }      
}
