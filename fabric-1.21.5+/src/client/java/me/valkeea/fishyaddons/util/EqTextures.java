package me.valkeea.fishyaddons.util;

import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;

import me.valkeea.fishyaddons.config.FishyConfig;
import me.valkeea.fishyaddons.config.ItemConfig;
import me.valkeea.fishyaddons.config.Key;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;

public class EqTextures {
    private EqTextures() {}
    private static final Logger LOGGER = LoggerFactory.getLogger("FishyAddons/SkullTexture");
    
    private static final Map<Integer, Identifier> slotTextures = new HashMap<>();
    private static final Map<Integer, String> skullTextureUrls = new HashMap<>();
    private static final Map<Integer, ItemStack> skullItemStacks = new HashMap<>();
    private static final Map<Integer, Boolean> emptySlots = new HashMap<>();

    static {
        loadSkullData();
    }

    private static final String TEXTURES = "textures";

    public static String extractSkullTexture(ItemStack itemStack) {
        if (itemStack == null || itemStack.getItem() != Items.PLAYER_HEAD) {
            return null;
        }

        var profile = itemStack.getOrDefault(DataComponentTypes.PROFILE, null);
        if (profile == null) {
            return null;
        }

        try {
            var properties = profile.properties();
            Collection<Property> textures = properties.get(TEXTURES);
            String value = firstTextureValue(textures);

            if (value == null || value.isEmpty()) {
                return null;
            }

            String json = decodeBase64(value);
            return urlFromJson(json);

        } catch (Exception e) {
            LOGGER.warn("Failed to extract skull texture: {}", e.getMessage());
            return null;
        }
    }

    private static String firstTextureValue(Collection<Property> textures) {
        if (textures == null || textures.isEmpty()) {
            return null;
        }

        var first = textures.iterator().next();
        return first != null ? first.value() : null;
    }

    private static String decodeBase64(String value) {
        try {
            byte[] decoded = Base64.getDecoder().decode(value);
            return new String(decoded);

        } catch (Exception e) {
            LOGGER.warn("Failed to decode base64 texture value: {}", e.getMessage());
            return "";
        }
    }

    private static String urlFromJson(String json) {
        int texturesStart = json.indexOf("\"textures\"");
        if (texturesStart == -1) {
            return null;
        }

        int skinStart = json.indexOf("\"SKIN\"", texturesStart);
        if (skinStart == -1) {
            return null;
        }

        int urlStart = json.indexOf("\"url\"", skinStart);
        if (urlStart == -1) {
            return null;
        }

        int colonIndex = json.indexOf(":", urlStart);
        int openQuote = json.indexOf("\"", colonIndex + 1);
        int closeQuote = json.indexOf("\"", openQuote + 1);
        if (openQuote > 0 && closeQuote > openQuote) {
            return json.substring(openQuote + 1, closeQuote);
        }
        
        LOGGER.warn("Could not find URL value in SKIN object");
        return null;
    }
    
    public static void saveSkullTexture(int slotIndex, ItemStack itemStack) {
        if (!FishyConfig.getState(Key.EQ_DISPLAY, false)) return;
        
        String textureUrl = extractSkullTexture(itemStack);
        if (textureUrl != null) {

            skullTextureUrls.put(slotIndex, textureUrl);
            skullItemStacks.put(slotIndex, itemStack.copy());
            emptySlots.put(slotIndex, false);
            
            Identifier textureId = createTextureFromUrl(slotIndex);
            if (textureId != null) {
                slotTextures.put(slotIndex, textureId);
            }
            
            ItemConfig.setEqSkull(slotIndex, textureUrl);
            ItemConfig.setEqItemData(slotIndex, "minecraft:player_head");
        }
    }
    
    public static void saveEmptySlot(int slotIndex) {
        if (!FishyConfig.getState(Key.EQ_DISPLAY, false)) return;
        
        // Clear any existing data for this slot
        slotTextures.remove(slotIndex);
        skullTextureUrls.remove(slotIndex);
        skullItemStacks.remove(slotIndex);
        emptySlots.put(slotIndex, true);
        
        // Clear from config as well
        ItemConfig.clearEquipmentSlot(slotIndex);
    }
    
    public static boolean hasSkullTexture(int slotIndex) {
        return slotTextures.containsKey(slotIndex) || ItemConfig.hasEquipmentSkull(slotIndex);
    }
    
    public static boolean hasSlotData(int slotIndex) {
        return hasSkullTexture(slotIndex) || emptySlots.containsKey(slotIndex);
    }
    
    public static boolean isEmptySlot(int slotIndex) {
        return emptySlots.getOrDefault(slotIndex, false);
    }

    public static ItemStack getSlotItemStack(int slotIndex) {
        if (!skullItemStacks.containsKey(slotIndex)) {
            loadSlotFromConfig(slotIndex);
        }
        return skullItemStacks.get(slotIndex);
    }

    private static void loadSlotFromConfig(int slotIndex) {
        String textureUrl = ItemConfig.getEquipmentSkullTexture(slotIndex);
        if (textureUrl != null && !textureUrl.isEmpty()) {
            skullTextureUrls.put(slotIndex, textureUrl);
            
            Identifier textureId = createTextureFromUrl(slotIndex);
            if (textureId != null) {
                slotTextures.put(slotIndex, textureId);
            }
            
            // Create a proper skull ItemStack with texture data
            ItemStack skullStack = createSkullWithTexture(textureUrl);
            skullItemStacks.put(slotIndex, skullStack);
        }
    }
    
    public static void clearAll() {
        slotTextures.clear();
        skullTextureUrls.clear();
        skullItemStacks.clear();
        emptySlots.clear();
        ItemConfig.clearEquipmentSkulls();
    }

    // Create a texture identifier from a URL
    private static Identifier createTextureFromUrl(int slotIndex) {
        try {
            String textureUrl = skullTextureUrls.get(slotIndex);
            if (textureUrl == null) return null;
            
            // Extract the texture hash from the URL for creating identifier
            String textureHash = textureUrl.substring(textureUrl.lastIndexOf('/') + 1);
            return Identifier.of("fishyaddons", "skull_texture_" + textureHash);
        } catch (Exception e) {
            LOGGER.warn("Failed to create texture from URL: {}", e.getMessage());
            return null;
        }
    }

    private static ItemStack createSkullWithTexture(String textureUrl) {
        try {
            ItemStack skull = new ItemStack(Items.PLAYER_HEAD);
            String textureJson = String.format(
                "{\"textures\":{\"SKIN\":{\"url\":\"%s\"}}}", 
                textureUrl
            );

            String encodedTexture = Base64.getEncoder().encodeToString(textureJson.getBytes());
            var profile = new GameProfile(UUID.randomUUID(), "EquipmentSlot");
            var profileComponent = new ProfileComponent(profile);

            profile.getProperties().put(TEXTURES, new Property(TEXTURES, encodedTexture));            
            skull.set(DataComponentTypes.PROFILE, profileComponent);
            
            return skull;
        } catch (Exception e) {
            LOGGER.warn("Failed to create skull with texture: {}", e.getMessage());
            return new ItemStack(Items.PLAYER_HEAD);
        }
    }

    private static void loadSkullData() {
        try {
            Map<Integer, String> persistedTextures = ItemConfig.getAllEquipmentSkullTextures();
            for (Map.Entry<Integer, String> entry : persistedTextures.entrySet()) {
                int slot = entry.getKey();
                String textureUrl = entry.getValue();
                
                if (textureUrl != null && !textureUrl.isEmpty()) {
                    skullTextureUrls.put(slot, textureUrl);
                    
                    Identifier textureId = createTextureFromUrl(slot);
                    if (textureId != null) {
                        slotTextures.put(slot, textureId);
                    }
                    
                    ItemStack skullStack = createSkullWithTexture(textureUrl);
                    skullItemStacks.put(slot, skullStack);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load skull data from ItemConfig", e);
        }
    }
    
    public static Map<Integer, Identifier> getAllTextures() {
        return new HashMap<>(slotTextures);
    }    
}
