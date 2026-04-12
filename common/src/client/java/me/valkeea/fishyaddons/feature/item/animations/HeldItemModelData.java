package me.valkeea.fishyaddons.feature.item.animations;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

@SuppressWarnings("squid:S1104")
public class HeldItemModelData {
    
    // Position offsets (coordinates)
    public double posOffsetX = 0.0;
    public double posOffsetY = 0.0; 
    public double posOffsetZ = 0.0;
    
    // Rotation offsets (degrees)
    public double rotOffsetX = 0.0;
    public double rotOffsetY = 0.0;
    public double rotOffsetZ = 0.0;
    
    // Scale multiplier
    public double scale = 1.0;
    
    // Swing animation intensity (0.0 to 1.0, where 1.0 = 100%)
    public double swingIntensity = 0.5;
    
    // Equip animation intensity (0.0 to 1.0, where 1.0 = 100%)
    public double equipIntensity = 0.5;
    
    // Main hand settings
    public double mainHandPosX = 0.0;
    public double mainHandPosY = 0.0;
    public double mainHandPosZ = 0.0;
    public double mainHandRotX = 0.0;
    public double mainHandRotY = 0.0;
    public double mainHandRotZ = 0.0;
    
    // Offhand settings
    public double offHandPosX = 0.0;
    public double offHandPosY = 0.0;
    public double offHandPosZ = 0.0;
    public double offHandRotX = 0.0;
    public double offHandRotY = 0.0;
    public double offHandRotZ = 0.0;
    
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
            System.err.println("Empty JSON string for HeldItemModelData, using defaults");
            return new HeldItemModelData();
        }
        
        try {
            return GSON.fromJson(json, HeldItemModelData.class);
        } catch (Exception e) {
            System.err.println("Failed to parse HeldItemModelData from JSON: " + e.getMessage());
            e.printStackTrace();
            return new HeldItemModelData();
        }
    }
    
    /**
     * Reset all values to defaults
     */
    public void reset() {
        posOffsetX = posOffsetY = posOffsetZ = 0.0;
        rotOffsetX = rotOffsetY = rotOffsetZ = 0.0;
        scale = 1.0;
        swingIntensity = 0.5;
        equipIntensity = 0.5;
        
        mainHandPosX = mainHandPosY = mainHandPosZ = 0.0;
        mainHandRotX = mainHandRotY = mainHandRotZ = 0.0;
        
        offHandPosX = offHandPosY = offHandPosZ = 0.0;
        offHandRotX = offHandRotY = offHandRotZ = 0.0;
        
        separateHandSettings = false;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        HeldItemModelData that = (HeldItemModelData) obj;
        
        return Double.compare(that.posOffsetX, posOffsetX) == 0 &&
               Double.compare(that.posOffsetY, posOffsetY) == 0 &&
               Double.compare(that.posOffsetZ, posOffsetZ) == 0 &&
               Double.compare(that.rotOffsetX, rotOffsetX) == 0 &&
               Double.compare(that.rotOffsetY, rotOffsetY) == 0 &&
               Double.compare(that.rotOffsetZ, rotOffsetZ) == 0 &&
               Double.compare(that.scale, scale) == 0 &&
               Double.compare(that.swingIntensity, swingIntensity) == 0 &&
               Double.compare(that.mainHandPosX, mainHandPosX) == 0 &&
               Double.compare(that.mainHandPosY, mainHandPosY) == 0 &&
               Double.compare(that.mainHandPosZ, mainHandPosZ) == 0 &&
               Double.compare(that.mainHandRotX, mainHandRotX) == 0 &&
               Double.compare(that.mainHandRotY, mainHandRotY) == 0 &&
               Double.compare(that.mainHandRotZ, mainHandRotZ) == 0 &&
               Double.compare(that.offHandPosX, offHandPosX) == 0 &&
               Double.compare(that.offHandPosY, offHandPosY) == 0 &&
               Double.compare(that.offHandPosZ, offHandPosZ) == 0 &&
               Double.compare(that.offHandRotX, offHandRotX) == 0 &&
               Double.compare(that.offHandRotY, offHandRotY) == 0 &&
               Double.compare(that.offHandRotZ, offHandRotZ) == 0 &&
               separateHandSettings == that.separateHandSettings;
    }
    
    @Override
    public int hashCode() {
        int result = Double.hashCode(posOffsetX);
        result = 31 * result + Double.hashCode(posOffsetY);
        result = 31 * result + Double.hashCode(posOffsetZ);
        result = 31 * result + Double.hashCode(rotOffsetX);
        result = 31 * result + Double.hashCode(rotOffsetY);
        result = 31 * result + Double.hashCode(rotOffsetZ);
        result = 31 * result + Double.hashCode(scale);
        result = 31 * result + Double.hashCode(swingIntensity);
        result = 31 * result + Double.hashCode(mainHandPosX);
        result = 31 * result + Double.hashCode(mainHandPosY);
        result = 31 * result + Double.hashCode(mainHandPosZ);
        result = 31 * result + Double.hashCode(mainHandRotX);
        result = 31 * result + Double.hashCode(mainHandRotY);
        result = 31 * result + Double.hashCode(mainHandRotZ);
        result = 31 * result + Double.hashCode(offHandPosX);
        result = 31 * result + Double.hashCode(offHandPosY);
        result = 31 * result + Double.hashCode(offHandPosZ);
        result = 31 * result + Double.hashCode(offHandRotX);
        result = 31 * result + Double.hashCode(offHandRotY);
        result = 31 * result + Double.hashCode(offHandRotZ);
        result = 31 * result + Boolean.hashCode(separateHandSettings);
        return result;
    }
}
