package me.valkeea.fishyaddons.util;

import me.valkeea.fishyaddons.config.FishyConfig;
import me.valkeea.fishyaddons.config.ItemConfig;
import me.valkeea.fishyaddons.config.Key;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ProfileComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import java.util.UUID;

import java.util.HashMap;
import java.util.Map;
import java.util.Collection;
import java.util.Base64;

public class EqTextures {
    private EqTextures() {}
    private static final Logger LOGGER = LoggerFactory.getLogger("FishyAddons/SkullTexture");
    
    // Store skull textures for the 4 equipment slots
    private static final Map<Integer, Identifier> slotTextures = new HashMap<>();
    private static final Map<Integer, String> skullTextureUrls = new HashMap<>();
    private static final Map<Integer, ItemStack> skullItemStacks = new HashMap<>();
    private static final Map<Integer, Boolean> emptySlots = new HashMap<>();

    static {
        loadSkullData();
    }

    public static final int HELMET_SLOT = 0;
    public static final int CHESTPLATE_SLOT = 1;
    public static final int LEGGINGS_SLOT = 2;
    public static final int BOOTS_SLOT = 3;

    public static String extractSkullTexture(ItemStack itemStack) {
        if (itemStack != null && itemStack.getItem() == Items.PLAYER_HEAD) {
            ProfileComponent profile = (ProfileComponent)itemStack.getOrDefault(DataComponentTypes.PROFILE, (Object)null);

            if (profile == null) {
                return null;
            } else {
                try {
                PropertyMap properties = profile.properties();
                Collection<Property> textures = properties.get("textures");
                if (textures == null || textures.isEmpty()) {
                    return null;
                }

                String value = ((Property)textures.iterator().next()).value();
                if (value.isEmpty()) {
                    return null;
                }

                byte[] decoded = Base64.getDecoder().decode(value);
                String json = new String(decoded);
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

                } catch (Exception e) {
                LOGGER.warn("Failed to extract skull texture: {}", e.getMessage());
                }

                return null;
            }
        } else {
            return null;
        }
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
    
    public static Map<Integer, Identifier> getAllTextures() {
        return new HashMap<>(slotTextures);
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

    private static ItemStack createSkullWithTexture(String textureUrl) {
        try {
            ItemStack skull = new ItemStack(Items.PLAYER_HEAD);
            
            // Create the base64 encoded texture data
            String textureJson = String.format(
                "{\"textures\":{\"SKIN\":{\"url\":\"%s\"}}}", 
                textureUrl
            );
            String encodedTexture = Base64.getEncoder().encodeToString(textureJson.getBytes());
            
            // Create a game profile with the texture
            GameProfile profile = new GameProfile(UUID.randomUUID(), "EquipmentSlot");
            profile.getProperties().put("textures", new Property("textures", encodedTexture));
            
            // Create profile component and set it on the skull
            ProfileComponent profileComponent = new ProfileComponent(profile);
            skull.set(DataComponentTypes.PROFILE, profileComponent);
            
            return skull;
        } catch (Exception e) {
            LOGGER.warn("Failed to create skull with texture: {}", e.getMessage());
            return new ItemStack(Items.PLAYER_HEAD); // Fallback to basic skull
        }
    }
}
