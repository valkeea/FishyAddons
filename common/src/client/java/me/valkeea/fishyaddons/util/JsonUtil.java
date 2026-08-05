package me.valkeea.fishyaddons.util;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;

import net.minecraft.client.Minecraft;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.component.ResolvableProfile;

public class JsonUtil {
    private JsonUtil() {}

    private static final Gson GSON = new Gson();
    private static final Logger LOGGER = LoggerFactory.getLogger("FishyAddons/JsonSerialization");

    /**
     * Serialize a Text component to JSON string
     */
    public static String serializeText(Component text) {
        if (text == null) {
            return "";
        }

        try {
            var mc = Minecraft.getInstance();
            if (mc.level == null) return "";

            HolderLookup.Provider registries = mc.level.registryAccess();
            RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, registries);
            DataResult<JsonElement> result = ComponentSerialization.CODEC.encodeStart(ops, text);
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
    public static Component deserializeText(String jsonOrPlain) {
        if (jsonOrPlain == null || jsonOrPlain.isEmpty()) {
            return Component.empty();
        }

        String trimmed = jsonOrPlain.trim();
        
        if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
            try {
                var json = GSON.fromJson(trimmed, JsonElement.class);
                var mc = Minecraft.getInstance();
                if (mc.level == null) {
                    LOGGER.debug("Cannot deserialize Text: client world is null, falling back to literal");
                    return Component.literal(jsonOrPlain);
                }

                HolderLookup.Provider registries = mc.level.registryAccess();
                RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, registries);
                DataResult<Component> result = ComponentSerialization.CODEC.parse(ops, json);
                Component parsed = result.result().orElse(null);
                if (parsed == null) {
                    LOGGER.debug("Failed to parse as Text component, falling back to literal");
                    return Component.literal(jsonOrPlain);
                }
                return parsed;
            } catch (Exception e) {
                LOGGER.debug("Exception while deserializing Text component: {}, falling back to literal", e.getMessage());
            }
        }

        return Component.literal(jsonOrPlain);
    }

    private static final String COUNT = "count";
    private static final String PROFILE = "profile";
    private static final String CUSTOM_DATA = "customData";
    private static final String CUSTOM_MODEL_DATA = "customModelData";
    private static final String ITEM_MODEL = "item_model";
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
            var mc = Minecraft.getInstance();
            if (mc.level == null) return "";
            
            JsonObject obj = new JsonObject();
            
            var itemId = BuiltInRegistries.ITEM.getKey(itemStack.getItem());
            obj.addProperty("id", itemId.toString());
            obj.addProperty(COUNT, itemStack.getCount());
            
            var customName = itemStack.get(DataComponents.CUSTOM_NAME);
            if (customName != null) obj.addProperty("name", customName.getString());
            
            var profile = itemStack.get(DataComponents.PROFILE);
            if (profile != null) serialize(obj, profile, mc);
            
            var customData = itemStack.get(DataComponents.CUSTOM_DATA);
            if (customData != null) serialize(obj, customData, mc);
            
            var customModelData = itemStack.get(DataComponents.CUSTOM_MODEL_DATA);
            if (customModelData != null) serialize(obj, customModelData, mc);

            var itemModel = itemStack.get(DataComponents.ITEM_MODEL);
            if (itemModel != null) serialize(obj, itemModel, mc);
            
            if (itemStack.hasFoil()) obj.addProperty(GLINT, true);
            
            var dyedColor = itemStack.get(DataComponents.DYED_COLOR);
            if (dyedColor != null) obj.addProperty(DYED_COLOR, dyedColor.rgb());

            var tooltipLore = itemStack.getTooltipLines(
                TooltipContext.of(mc.level),
                mc.player, mc.options.advancedItemTooltips
                ? TooltipFlag.ADVANCED
                : TooltipFlag.NORMAL
            );

            if (tooltipLore != null && !tooltipLore.isEmpty()) {
                HolderLookup.Provider ttRegistries = mc.level.registryAccess();
                RegistryOps<JsonElement> tooltipOps = RegistryOps.create(JsonOps.INSTANCE, ttRegistries);
                JsonArray tooltipArray = new JsonArray();
                for (Component line : tooltipLore) {
                    ComponentSerialization.CODEC.encodeStart(tooltipOps, line).result().ifPresent(tooltipArray::add);
                }
                obj.add("tooltip", tooltipArray);
            }
            
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
            var mc = Minecraft.getInstance();
            if (mc.level == null) return ItemStack.EMPTY;
            
            JsonObject obj = GSON.fromJson(json, JsonObject.class);
            
            if (!obj.has("id")) return ItemStack.EMPTY;
            
            var itemIdStr = obj.get("id").getAsString();
            var itemId = Identifier.tryParse(itemIdStr);
            if (itemId == null) return ItemStack.EMPTY;
            
            if (!BuiltInRegistries.ITEM.containsKey(itemId)) return ItemStack.EMPTY;
            
            Item item = BuiltInRegistries.ITEM.getValue(itemId);
            int count = obj.has(COUNT) ? obj.get(COUNT).getAsInt() : 1;
            var stack = new ItemStack(item, count);
            
            if (obj.has("name")) {
                var customName = obj.get("name").getAsString();
                stack.set(DataComponents.CUSTOM_NAME, Component.literal(customName));
            }
            
            if (obj.has(PROFILE)) setProfile(stack, obj.get(PROFILE), mc);
            if (obj.has(CUSTOM_DATA)) setCustomData(stack, obj.get(CUSTOM_DATA), mc);
            if (obj.has(CUSTOM_MODEL_DATA)) setCustomModelData(stack, obj.get(CUSTOM_MODEL_DATA), mc);
            if (obj.has(ITEM_MODEL)) setItemModel(stack, obj.get(ITEM_MODEL), mc);
            if (obj.has(GLINT) && obj.get(GLINT).getAsBoolean()) stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
            if (obj.has(DYED_COLOR)) {
                int rgb = obj.get(DYED_COLOR).getAsInt();
                stack.set(DataComponents.DYED_COLOR, new net.minecraft.world.item.component.DyedItemColor(rgb));
            }

            return stack;
            
        } catch (Exception e) {
            LOGGER.warn("Exception while deserializing ItemStack for display: {}", e.getMessage());
            return ItemStack.EMPTY;
        }
    }

    /**
     * Extract the saved tooltip lines from serialized ItemStack JSON
     */
    public static List<Component> extractTooltip(String json) {
        if (json == null || json.isEmpty()) return List.of();

        try {
            var mc = Minecraft.getInstance();
            if (mc.level == null) return List.of();

            JsonObject obj = GSON.fromJson(json, JsonObject.class);
            if (obj == null || !obj.has("tooltip") || !obj.get("tooltip").isJsonArray()) {
                return List.of();
            }

            HolderLookup.Provider registries = mc.level.registryAccess();
            RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, registries);

            List<Component> lines = new ArrayList<>();
            for (JsonElement lineJson : obj.get("tooltip").getAsJsonArray()) {
                ComponentSerialization.CODEC.parse(ops, lineJson).result().ifPresent(lines::add);
            }
            return lines;

        } catch (Exception e) {
            LOGGER.debug("Failed to extract tooltip: {}", e.getMessage());
            return List.of();
        }
    }

    private static void serialize(JsonObject obj, ResolvableProfile profile, Minecraft mc) {
        serializeComponent(obj, profile, ResolvableProfile.CODEC, PROFILE, mc);
    }

    private static void serialize(JsonObject obj, CustomData customData, Minecraft mc) {
        serializeComponent(obj, customData, CustomData.CODEC, CUSTOM_DATA, mc);
    }

    private static void serialize(JsonObject obj, CustomModelData customModelData, Minecraft mc) {
        serializeComponent(obj, customModelData, CustomModelData.CODEC, CUSTOM_MODEL_DATA, mc);
    }

    private static void serialize(JsonObject obj, Identifier itemModel, Minecraft mc) {
        serializeComponent(obj, itemModel, Identifier.CODEC, ITEM_MODEL, mc);
    }

    private static void setProfile(ItemStack stack, JsonElement json, Minecraft mc) {
        deserializeComponent(stack, json, ResolvableProfile.CODEC, DataComponents.PROFILE, mc);
    }

    private static void setCustomData(ItemStack stack, JsonElement json, Minecraft mc) {
        deserializeComponent(stack, json, CustomData.CODEC, DataComponents.CUSTOM_DATA, mc);
    }

    private static void setItemModel(ItemStack stack, JsonElement json, Minecraft mc) {
        deserializeComponent(stack, json, Identifier.CODEC, DataComponents.ITEM_MODEL, mc);
    }

    private static void setCustomModelData(ItemStack stack, JsonElement json, Minecraft mc) {
        deserializeComponent(stack, json, CustomModelData.CODEC, DataComponents.CUSTOM_MODEL_DATA, mc);
    }

    private static <T> void serializeComponent(
        JsonObject obj, 
        T component, 
        com.mojang.serialization.Codec<T> codec, 
        String key, 
        Minecraft mc
    ) {
        try {
            HolderLookup.Provider registries = mc.level.registryAccess();
            RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, registries);
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
        net.minecraft.core.component.DataComponentType<T> componentType,
        Minecraft mc
    ) {
        try {
            HolderLookup.Provider registries = mc.level.registryAccess();
            RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, registries);
            DataResult<T> result = codec.parse(ops, json);
            result.result().ifPresent(value -> stack.set(componentType, value));
        } catch (Exception e) {
            LOGGER.debug("Failed to deserialize component: {}", e.getMessage());
        }
    }      
}
