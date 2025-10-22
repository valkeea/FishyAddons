package me.valkeea.fishyaddons.handler;

import me.valkeea.fishyaddons.config.FishyConfig;
import me.valkeea.fishyaddons.config.HeldItemConfigData;
import me.valkeea.fishyaddons.config.ItemConfig;
import me.valkeea.fishyaddons.config.Key;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Hand;
import net.minecraft.util.math.RotationAxis;

public class HeldItems {
    private HeldItems() {}
    
    public static void init() {
        load();
    }

    public static void applyAllTransformations(MatrixStack matrices, Hand hand) {
        if (!isEnabled()) {
            return;
        }

        position(matrices, hand);
        scale(matrices);
        rotation(matrices, hand);
    }

    /**
     * Apply position transformations in world coordinate space
     */
    private static void position(MatrixStack matrices, Hand hand) {
        if (isSeparateHandSettings()) {
            separatePosition(matrices, hand);
        } else {
            globalPosition(matrices);
        }
    }

    /**
     * Apply rotation transformations around item center
     */
    private static void rotation(MatrixStack matrices, Hand hand) {
        if (isSeparateHandSettings()) {
            separateRotation(matrices, hand);
        } else {
            globalRotation(matrices);
        }
    }

    /**
     * Apply scale transformations
     */
    private static void scale(MatrixStack matrices) {
        float scale = getScale();
        matrices.scale(scale, scale, scale);
    }

    private static void separatePosition(MatrixStack matrices, Hand hand) {
        boolean isMainHand = hand == Hand.MAIN_HAND;
        
        if (isMainHand) {
            matrices.translate(
                getMainHandPosX(),
                getMainHandPosY(),
                getMainHandPosZ()
            );
        } else {
            matrices.translate(
                getOffHandPosX(),
                getOffHandPosY(),
                getOffHandPosZ()
            );
        }
    }

    private static void separateRotation(MatrixStack matrices, Hand hand) {
        boolean isMainHand = hand == Hand.MAIN_HAND;

        if (isMainHand) {
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(getMainHandRotX()));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(getMainHandRotY()));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(getMainHandRotZ()));
        } else {
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(getOffHandRotX()));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(getOffHandRotY()));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(getOffHandRotZ()));
        }
    }

    private static void globalPosition(MatrixStack matrices) {
        matrices.translate(
            getPosOffsetX(),
            getPosOffsetY(),
            getPosOffsetZ()
        );
    }

    private static void globalRotation(MatrixStack matrices) {
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(getRotOffsetX()));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(getRotOffsetY()));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(getRotOffsetZ()));
    }


    private static HeldItemConfigData runtimeConfig = new HeldItemConfigData();
    private static final String CONFIG_DATA_KEY = "heldItemConfigData";
    
    public static void load() {
        String jsonData = ItemConfig.getString(CONFIG_DATA_KEY, "");
        runtimeConfig = HeldItemConfigData.fromJson(jsonData);
        boolean configState = FishyConfig.getState(Key.HELD_ITEM_TRANSFORMS, false);
        enabled = configState;
    }
    

    // Convenience method for UI components
    public static java.util.function.Consumer<Float> createSetter(java.util.function.Consumer<Float> setter) {
        return value -> {
            setter.accept(value);
            saveConfig();
        };
    }

    /**
     * Copies the global settings to separate for both hands
     */
    public static void cloneGlobal() {
        runtimeConfig.mainHandPosX = runtimeConfig.posOffsetX;
        runtimeConfig.mainHandPosY = runtimeConfig.posOffsetY;
        runtimeConfig.mainHandPosZ = runtimeConfig.posOffsetZ;
        runtimeConfig.mainHandRotX = runtimeConfig.rotOffsetX;
        runtimeConfig.mainHandRotY = runtimeConfig.rotOffsetY;
        runtimeConfig.mainHandRotZ = runtimeConfig.rotOffsetZ;
        
        runtimeConfig.offHandPosX = runtimeConfig.posOffsetX;
        runtimeConfig.offHandPosY = runtimeConfig.posOffsetY;
        runtimeConfig.offHandPosZ = runtimeConfig.posOffsetZ;
        runtimeConfig.offHandRotX = runtimeConfig.rotOffsetX;
        runtimeConfig.offHandRotY = runtimeConfig.rotOffsetY;
        runtimeConfig.offHandRotZ = runtimeConfig.rotOffsetZ;
        
        saveConfig();
    }

    /**
     * Get cached or calculate swing direction based on current rotation settings
     */
    private static SwingDirection getSwingDirection() {
        float rotX = isSeparateHandSettings() ? getMainHandRotX() : getRotOffsetX();
        float rotY = isSeparateHandSettings() ? getMainHandRotY() : getRotOffsetY();
        float rotZ = isSeparateHandSettings() ? getMainHandRotZ() : getRotOffsetZ();
        boolean separateSettings = isSeparateHandSettings();
        
        if (cachedSwingDirection == null || 
            lastRotX != rotX || 
            lastRotY != rotY || 
            lastRotZ != rotZ ||
            lastSeparateHandSettings != separateSettings) {
            
            cachedSwingDirection = calcOptimalDirection(rotX, rotY, rotZ);
            lastRotX = rotX;
            lastRotY = rotY;
            lastRotZ = rotZ;
            lastSeparateHandSettings = separateSettings;
        }
        
        return cachedSwingDirection;
    }

    /**
     * Attempt to create a metronome effect where movement is perpendicular to the grip
     */
    private static SwingDirection calcOptimalDirection(float rotX, float rotY, float rotZ) {

        double radX = Math.toRadians(rotX);
        double radY = Math.toRadians(rotY);
        double radZ = Math.toRadians(rotZ);

        double totalRotation = Math.sqrt(rotX * rotX + rotY * rotY + rotZ * rotZ);
        float dampingFactor = (float) Math.clamp(90.0 / (totalRotation + 1.0), 0.1, 1.0);
        
        final float globalIntensityReducer = 0.15f;

        float xFactor;
        float yFactor;
        float zFactor;

        // Pitched up/down
        if (Math.abs(rotX) > 30) {
            zFactor = Math.signum(rotX) * dampingFactor * globalIntensityReducer;
            xFactor = (float) (Math.sin(radY) * 0.3 * dampingFactor * globalIntensityReducer);
            yFactor = Math.abs(rotX) > 60 ? (float) (-Math.sin(radX) * 0.15 * dampingFactor * globalIntensityReducer) : 0;
        }

        // Yawed left/right
        else if (Math.abs(rotY) > 30) {
            xFactor = Math.signum(rotY) * dampingFactor * globalIntensityReducer;
            zFactor = (float) (-Math.sin(radX) * 0.3 * dampingFactor * globalIntensityReducer);
            yFactor = (Math.abs(rotX) > 45 && Math.abs(rotY) > 60) ? 
                      (float) (Math.sin(radX) * Math.sin(radY) * 0.2 * dampingFactor * globalIntensityReducer) : 0;
        }

        // Has Z rotation but minimal X/Y
        else if (Math.abs(rotZ) > 15) {
            xFactor = (float) (Math.sin(radZ) * 0.7 * dampingFactor * globalIntensityReducer);
            zFactor = (float) (Math.cos(radZ) * 0.7 * dampingFactor * globalIntensityReducer);
            // Y movement for extreme roll angles
            yFactor = Math.abs(rotZ) > 60 ? (float) (Math.cos(radZ) * 0.1 * dampingFactor * globalIntensityReducer) : 0;
        }
        
        // Default - no Y movement
        else {
            xFactor = 0.2f * dampingFactor * globalIntensityReducer;
            yFactor = 0.0f;
            zFactor = 0.8f * dampingFactor * globalIntensityReducer;
        }
        
        return new SwingDirection(xFactor, yFactor, zFactor);
    }
    
    private static class SwingDirection {
        final float xFactor;
        final float yFactor;
        final float zFactor;
        
        SwingDirection(float xFactor, float yFactor, float zFactor) {
            this.xFactor = xFactor;
            this.yFactor = yFactor;
            this.zFactor = zFactor;
        }
    }
    
    private static SwingDirection cachedSwingDirection = null;
    private static float lastRotX = Float.NaN;
    private static float lastRotY = Float.NaN;
    private static float lastRotZ = Float.NaN;
    private static boolean lastSeparateHandSettings = false;    
    
    // --- Config API ---
    
    public static HeldItemConfigData getConfigData() {
        return runtimeConfig;
    }
    
    public static void setConfigData(HeldItemConfigData newConfig) {
        runtimeConfig = new HeldItemConfigData(newConfig);
        saveConfig();
        refresh();
    }
    
    public static void saveConfig() {
        String jsonData = runtimeConfig.toJson();
        ItemConfig.settings.set(CONFIG_DATA_KEY, jsonData);
        ItemConfig.save();
        invalidateSwingCache();
    }
    
    private static void invalidateSwingCache() {
        cachedSwingDirection = null;
    }

    private static boolean enabled = false;

    public static boolean isEnabled() {
        if (!enabled) {
            refresh();
        }
        return enabled;
    }
    
    public static void refresh() {
        enabled = FishyConfig.getState(Key.HELD_ITEM_TRANSFORMS, false);
    }
    
    public static void reset() {
        runtimeConfig.reset();
        saveConfig();
    }

    // Position
    public static float getPosOffsetX() { return runtimeConfig.posOffsetX; }
    public static float getPosOffsetY() { return runtimeConfig.posOffsetY; }
    public static float getPosOffsetZ() { return runtimeConfig.posOffsetZ; }

    public static void setPosOffsetX(float value) {
        runtimeConfig.posOffsetX = value;
    }
    public static void setPosOffsetY(float value) {
        runtimeConfig.posOffsetY = value;
    }
    public static void setPosOffsetZ(float value) {
        runtimeConfig.posOffsetZ = value;
    }
    
    // Rotation
    public static float getRotOffsetX() { return runtimeConfig.rotOffsetX; }
    public static float getRotOffsetY() { return runtimeConfig.rotOffsetY; }
    public static float getRotOffsetZ() { return runtimeConfig.rotOffsetZ; }

    public static void setRotOffsetX(float value) {
        runtimeConfig.rotOffsetX = value;
        saveConfig();
    }
    public static void setRotOffsetY(float value) {
        runtimeConfig.rotOffsetY = value;
        saveConfig();
    }
    public static void setRotOffsetZ(float value) {
        runtimeConfig.rotOffsetZ = value;
        saveConfig();
    }
    
    // Unified scale
    public static float getScale() { return runtimeConfig.scale; }
    
    public static void setScale(float value) { 
        runtimeConfig.scale = value; 
        saveConfig();
    }
    
    // Unified Animation Intensities
    public static float getSwingIntensity() { return runtimeConfig.swingIntensity; }
    public static float getEquipIntensity() { return runtimeConfig.equipIntensity; }

    public static float getSwingXMovement() {
        SwingDirection direction = getSwingDirection();
        return direction.xFactor * runtimeConfig.swingIntensity;
    }

    public static float getSwingYMovement() {
        SwingDirection direction = getSwingDirection();
        return direction.yFactor * runtimeConfig.swingIntensity;
    }

    public static float getSwingZMovement() {
        SwingDirection direction = getSwingDirection();
        return direction.zFactor * runtimeConfig.swingIntensity;
    }

    public static void setSwingIntensity(float value) { 
        runtimeConfig.swingIntensity = Math.clamp(value, 0.0f, 1.0f); 
        saveConfig();
    }
    
    public static void setEquipIntensity(float value) { 
        runtimeConfig.equipIntensity = Math.clamp(value, 0.0f, 1.0f); 
        saveConfig();
    }
    
    // Main hand
    public static float getMainHandPosX() { return runtimeConfig.mainHandPosX; }
    public static float getMainHandPosY() { return runtimeConfig.mainHandPosY; }
    public static float getMainHandPosZ() { return runtimeConfig.mainHandPosZ; }
    public static float getMainHandRotX() { return runtimeConfig.mainHandRotX; }
    public static float getMainHandRotY() { return runtimeConfig.mainHandRotY; }
    public static float getMainHandRotZ() { return runtimeConfig.mainHandRotZ; }
    
    public static void setMainHandPosX(float value) { 
        runtimeConfig.mainHandPosX = value; 
        saveConfig();
    }
    public static void setMainHandPosY(float value) { 
        runtimeConfig.mainHandPosY = value; 
        saveConfig();
    }
    public static void setMainHandPosZ(float value) { 
        runtimeConfig.mainHandPosZ = value; 
        saveConfig();
    }
    public static void setMainHandRotX(float value) { 
        runtimeConfig.mainHandRotX = value; 
        saveConfig();
    }
    public static void setMainHandRotY(float value) { 
        runtimeConfig.mainHandRotY = value; 
        saveConfig();
    }
    public static void setMainHandRotZ(float value) { 
        runtimeConfig.mainHandRotZ = value; 
        saveConfig();
    }
    
    // Offhand
    public static float getOffHandPosX() { return runtimeConfig.offHandPosX; }
    public static float getOffHandPosY() { return runtimeConfig.offHandPosY; }
    public static float getOffHandPosZ() { return runtimeConfig.offHandPosZ; }
    public static float getOffHandRotX() { return runtimeConfig.offHandRotX; }
    public static float getOffHandRotY() { return runtimeConfig.offHandRotY; }
    public static float getOffHandRotZ() { return runtimeConfig.offHandRotZ; }
    
    public static void setOffHandPosX(float value) { 
        runtimeConfig.offHandPosX = value; 
        saveConfig();
    }
    public static void setOffHandPosY(float value) { 
        runtimeConfig.offHandPosY = value; 
        saveConfig();
    }
    public static void setOffHandPosZ(float value) { 
        runtimeConfig.offHandPosZ = value; 
        saveConfig();
    }
    public static void setOffHandRotX(float value) { 
        runtimeConfig.offHandRotX = value; 
        saveConfig();
    }
    public static void setOffHandRotY(float value) { 
        runtimeConfig.offHandRotY = value; 
        saveConfig();
    }
    public static void setOffHandRotZ(float value) { 
        runtimeConfig.offHandRotZ = value; 
        saveConfig();
    }
    
    // Mode
    public static boolean isSeparateHandSettings() { return runtimeConfig.separateHandSettings; }
    
    public static void setSeparateHandSettings(boolean enabled) { 
        runtimeConfig.separateHandSettings = enabled; 
        saveConfig();
    }    
}