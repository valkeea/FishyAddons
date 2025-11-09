package me.valkeea.fishyaddons.feature.item.animations;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

@SuppressWarnings("squid:S1104")
public class HeldItemModelData {
    
    // Position offsets (coordinates)
    public float posOffsetX = 0.0f;
    public float posOffsetY = 0.0f; 
    public float posOffsetZ = 0.0f;
    
    // Rotation offsets (degrees)
    public float rotOffsetX = 0.0f;
    public float rotOffsetY = 0.0f;
    public float rotOffsetZ = 0.0f;
    
    // Scale multiplier
    public float scale = 1.0f;
    
    // Swing animation intensity (0.0 to 1.0, where 1.0 = 100%)
    public float swingIntensity = 0.5f;
    
    // Equip animation intensity (0.0 to 1.0, where 1.0 = 100%)
    public float equipIntensity = 0.5f;
    
    // Main hand settings
    public float mainHandPosX = 0.0f;
    public float mainHandPosY = 0.0f;
    public float mainHandPosZ = 0.0f;
    public float mainHandRotX = 0.0f;
    public float mainHandRotY = 0.0f;
    public float mainHandRotZ = 0.0f;
    
    // Offhand settings
    public float offHandPosX = 0.0f;
    public float offHandPosY = 0.0f;
    public float offHandPosZ = 0.0f;
    public float offHandRotX = 0.0f;
    public float offHandRotY = 0.0f;
    public float offHandRotZ = 0.0f;
    
    // Toggle state
    public boolean separateHandSettings = false;
    
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    public HeldItemModelData() {
        // Default values set above
    }
    
    /**
     * Copy constructor
     */
    public HeldItemModelData(HeldItemModelData other) {
        this.posOffsetX = other.posOffsetX;
        this.posOffsetY = other.posOffsetY;
        this.posOffsetZ = other.posOffsetZ;
        this.rotOffsetX = other.rotOffsetX;
        this.rotOffsetY = other.rotOffsetY;
        this.rotOffsetZ = other.rotOffsetZ;
        this.scale = other.scale;
        this.swingIntensity = other.swingIntensity;
        
        this.mainHandPosX = other.mainHandPosX;
        this.mainHandPosY = other.mainHandPosY;
        this.mainHandPosZ = other.mainHandPosZ;
        this.mainHandRotX = other.mainHandRotX;
        this.mainHandRotY = other.mainHandRotY;
        this.mainHandRotZ = other.mainHandRotZ;
        
        this.offHandPosX = other.offHandPosX;
        this.offHandPosY = other.offHandPosY;
        this.offHandPosZ = other.offHandPosZ;
        this.offHandRotX = other.offHandRotX;
        this.offHandRotY = other.offHandRotY;
        this.offHandRotZ = other.offHandRotZ;
        
        this.separateHandSettings = other.separateHandSettings;
    }
    
    public String toJson() {
        return GSON.toJson(this);
    }
    
    /**
     * Deserialize configuration from JSON string
     * Returns default configuration if parsing fails
     */
    public static HeldItemModelData fromJson(String json) {
        if (json == null || json.trim().isEmpty()) {
            return new HeldItemModelData();
        }
        
        try {
            return GSON.fromJson(json, HeldItemModelData.class);
        } catch (Exception e) {
            return new HeldItemModelData();
        }
    }
    
    /**
     * Reset all values to defaults
     */
    public void reset() {
        posOffsetX = posOffsetY = posOffsetZ = 0.0f;
        rotOffsetX = rotOffsetY = rotOffsetZ = 0.0f;
        scale = 1.0f;
        swingIntensity = 0.5f;
        equipIntensity = 0.5f;
        
        mainHandPosX = mainHandPosY = mainHandPosZ = 0.0f;
        mainHandRotX = mainHandRotY = mainHandRotZ = 0.0f;
        
        offHandPosX = offHandPosY = offHandPosZ = 0.0f;
        offHandRotX = offHandRotY = offHandRotZ = 0.0f;
        
        separateHandSettings = false;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        HeldItemModelData that = (HeldItemModelData) obj;
        
        return Float.compare(that.posOffsetX, posOffsetX) == 0 &&
               Float.compare(that.posOffsetY, posOffsetY) == 0 &&
               Float.compare(that.posOffsetZ, posOffsetZ) == 0 &&
               Float.compare(that.rotOffsetX, rotOffsetX) == 0 &&
               Float.compare(that.rotOffsetY, rotOffsetY) == 0 &&
               Float.compare(that.rotOffsetZ, rotOffsetZ) == 0 &&
               Float.compare(that.scale, scale) == 0 &&
               Float.compare(that.swingIntensity, swingIntensity) == 0 &&
               Float.compare(that.mainHandPosX, mainHandPosX) == 0 &&
               Float.compare(that.mainHandPosY, mainHandPosY) == 0 &&
               Float.compare(that.mainHandPosZ, mainHandPosZ) == 0 &&
               Float.compare(that.mainHandRotX, mainHandRotX) == 0 &&
               Float.compare(that.mainHandRotY, mainHandRotY) == 0 &&
               Float.compare(that.mainHandRotZ, mainHandRotZ) == 0 &&
               Float.compare(that.offHandPosX, offHandPosX) == 0 &&
               Float.compare(that.offHandPosY, offHandPosY) == 0 &&
               Float.compare(that.offHandPosZ, offHandPosZ) == 0 &&
               Float.compare(that.offHandRotX, offHandRotX) == 0 &&
               Float.compare(that.offHandRotY, offHandRotY) == 0 &&
               Float.compare(that.offHandRotZ, offHandRotZ) == 0 &&
               separateHandSettings == that.separateHandSettings;
    }
    
    @Override
    public int hashCode() {
        int result = Float.hashCode(posOffsetX);
        result = 31 * result + Float.hashCode(posOffsetY);
        result = 31 * result + Float.hashCode(posOffsetZ);
        result = 31 * result + Float.hashCode(rotOffsetX);
        result = 31 * result + Float.hashCode(rotOffsetY);
        result = 31 * result + Float.hashCode(rotOffsetZ);
        result = 31 * result + Float.hashCode(scale);
        result = 31 * result + Float.hashCode(swingIntensity);
        result = 31 * result + Float.hashCode(mainHandPosX);
        result = 31 * result + Float.hashCode(mainHandPosY);
        result = 31 * result + Float.hashCode(mainHandPosZ);
        result = 31 * result + Float.hashCode(mainHandRotX);
        result = 31 * result + Float.hashCode(mainHandRotY);
        result = 31 * result + Float.hashCode(mainHandRotZ);
        result = 31 * result + Float.hashCode(offHandPosX);
        result = 31 * result + Float.hashCode(offHandPosY);
        result = 31 * result + Float.hashCode(offHandPosZ);
        result = 31 * result + Float.hashCode(offHandRotX);
        result = 31 * result + Float.hashCode(offHandRotY);
        result = 31 * result + Float.hashCode(offHandRotZ);
        result = 31 * result + Boolean.hashCode(separateHandSettings);
        return result;
    }
}
