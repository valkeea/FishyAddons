package me.valkeea.fishyaddons.feature.item.animations;

import java.util.function.DoubleConsumer;

import me.valkeea.fishyaddons.ui.screen.HeldItemScreen;
import me.valkeea.fishyaddons.vconfig.annotation.UIRedirect;
import me.valkeea.fishyaddons.vconfig.annotation.UIToggle;
import me.valkeea.fishyaddons.vconfig.annotation.VCInit;
import me.valkeea.fishyaddons.vconfig.annotation.VCListener;
import me.valkeea.fishyaddons.vconfig.annotation.VCModule;
import me.valkeea.fishyaddons.vconfig.api.BooleanKey;
import me.valkeea.fishyaddons.vconfig.api.Config;
import me.valkeea.fishyaddons.vconfig.api.StringKey;
import me.valkeea.fishyaddons.vconfig.core.UICategory;
import me.valkeea.fishyaddons.vconfig.ui.manager.ScreenManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Hand;
import net.minecraft.util.math.RotationAxis;

@VCModule(UICategory.ITEMS)
public class HeldItems {
    private HeldItems() {}

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
        float scale = (float) getScale();
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
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees((float) getMainHandRotX()));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float) getMainHandRotY()));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float) getMainHandRotZ()));
        } else {
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees((float) getOffHandRotX()));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float) getOffHandRotY()));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float) getOffHandRotZ()));
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
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees((float) getRotOffsetX()));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float) getRotOffsetY()));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float) getRotOffsetZ()));
    }


    private static HeldItemModelData runtimeConfig = new HeldItemModelData();
    
    @VCInit
    public static void init() {
        enabled = Config.get(BooleanKey.HELD_ITEM_TRANSFORMS);
        String jsonData = Config.get(StringKey.ITEM_CONFIG);
        runtimeConfig = HeldItemModelData.fromJson(jsonData);
    }

    // Convenience method for UI components
    public static DoubleConsumer createSetter(DoubleConsumer setter) {
        return value -> {
            setter.accept(value);
            configChanged = true;
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
        
        configChanged = true;
    }

    /**
     * Get cached or calculate swing direction based on current rotation settings
     */
    private static SwingDirection getSwingDirection() {
        float rotX = (float) (isSeparateHandSettings() ? getMainHandRotX() : getRotOffsetX());
        float rotY = (float) (isSeparateHandSettings() ? getMainHandRotY() : getRotOffsetY());
        float rotZ = (float) (isSeparateHandSettings() ? getMainHandRotZ() : getRotOffsetZ());
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

    // Position
    public static double getPosOffsetX() { return runtimeConfig.posOffsetX; }
    public static double getPosOffsetY() { return runtimeConfig.posOffsetY; }
    public static double getPosOffsetZ() { return runtimeConfig.posOffsetZ; }

    public static void setPosOffsetX(double value) {
        runtimeConfig.posOffsetX = value;
    }
    public static void setPosOffsetY(double value) {
        runtimeConfig.posOffsetY = value;
    }
    public static void setPosOffsetZ(double value) {
        runtimeConfig.posOffsetZ = value;
    }
    
    // Rotation
    public static double getRotOffsetX() { return runtimeConfig.rotOffsetX; }
    public static double getRotOffsetY() { return runtimeConfig.rotOffsetY; }
    public static double getRotOffsetZ() { return runtimeConfig.rotOffsetZ; }

    public static void setRotOffsetX(double value) {
        runtimeConfig.rotOffsetX = value;
        configChanged = true;
    }
    public static void setRotOffsetY(double value) {
        runtimeConfig.rotOffsetY = value;
        configChanged = true;
    }
    public static void setRotOffsetZ(double value) {
        runtimeConfig.rotOffsetZ = value;
        configChanged = true;
    }
    
    // Unified scale
    public static double getScale() { return runtimeConfig.scale; }
    
    public static void setScale(double value) { 
        runtimeConfig.scale = value; 
        configChanged = true;
    }
    
    // Unified Animation Intensities
    public static double getSwingIntensity() { return runtimeConfig.swingIntensity; }
    public static double getEquipIntensity() { return runtimeConfig.equipIntensity; }

    public static double getSwingXMovement() {
        SwingDirection direction = getSwingDirection();
        return direction.xFactor * runtimeConfig.swingIntensity;
    }

    public static double getSwingYMovement() {
        SwingDirection direction = getSwingDirection();
        return direction.yFactor * runtimeConfig.swingIntensity;
    }

    public static double getSwingZMovement() {
        SwingDirection direction = getSwingDirection();
        return direction.zFactor * runtimeConfig.swingIntensity;
    }

    public static void setSwingIntensity(double value) { 
        runtimeConfig.swingIntensity = Math.clamp(value, 0.0, 1.0); 
        configChanged = true;
    }
    
    public static void setEquipIntensity(double value) { 
        runtimeConfig.equipIntensity = Math.clamp(value, 0.0, 1.0); 
        configChanged = true;
    }
    
    // Main hand
    public static double getMainHandPosX() { return runtimeConfig.mainHandPosX; }
    public static double getMainHandPosY() { return runtimeConfig.mainHandPosY; }
    public static double getMainHandPosZ() { return runtimeConfig.mainHandPosZ; }
    public static double getMainHandRotX() { return runtimeConfig.mainHandRotX; }
    public static double getMainHandRotY() { return runtimeConfig.mainHandRotY; }
    public static double getMainHandRotZ() { return runtimeConfig.mainHandRotZ; }
    
    public static void setMainHandPosX(double value) { 
        runtimeConfig.mainHandPosX = value; 
        configChanged = true;
    }
    public static void setMainHandPosY(double value) { 
        runtimeConfig.mainHandPosY = value; 
        configChanged = true;
    }
    public static void setMainHandPosZ(double value) { 
        runtimeConfig.mainHandPosZ = value; 
        configChanged = true;
    }
    public static void setMainHandRotX(double value) { 
        runtimeConfig.mainHandRotX = value; 
        configChanged = true;
    }
    public static void setMainHandRotY(double value) { 
        runtimeConfig.mainHandRotY = value; 
        configChanged = true;
    }
    public static void setMainHandRotZ(double value) { 
        runtimeConfig.mainHandRotZ = value; 
        configChanged = true;
    }
    
    // Offhand
    public static double getOffHandPosX() { return runtimeConfig.offHandPosX; }
    public static double getOffHandPosY() { return runtimeConfig.offHandPosY; }
    public static double getOffHandPosZ() { return runtimeConfig.offHandPosZ; }
    public static double getOffHandRotX() { return runtimeConfig.offHandRotX; }
    public static double getOffHandRotY() { return runtimeConfig.offHandRotY; }
    public static double getOffHandRotZ() { return runtimeConfig.offHandRotZ; }
    
    public static void setOffHandPosX(double value) { 
        runtimeConfig.offHandPosX = value; 
        configChanged = true;
    }
    public static void setOffHandPosY(double value) { 
        runtimeConfig.offHandPosY = value; 
        configChanged = true;
    }
    public static void setOffHandPosZ(double value) { 
        runtimeConfig.offHandPosZ = value; 
        configChanged = true;
    }
    public static void setOffHandRotX(double value) { 
        runtimeConfig.offHandRotX = value; 
        configChanged = true;
    }
    public static void setOffHandRotY(double value) { 
        runtimeConfig.offHandRotY = value; 
        configChanged = true;
    }
    public static void setOffHandRotZ(double value) { 
        runtimeConfig.offHandRotZ = value; 
        configChanged = true;
    }
    
    // Mode
    public static boolean isSeparateHandSettings() { return runtimeConfig.separateHandSettings; }
    
    public static void setSeparateHandSettings(boolean enabled) { 
        runtimeConfig.separateHandSettings = enabled; 
        configChanged = true;
    }
    
    private static boolean enabled = false;
    
    @UIToggle(
        key = BooleanKey.HELD_ITEM_TRANSFORMS,
        name = "Held Item Size and *Animations*",
        description = {"Configure the behavior and attributes of held items such as position,",
        "rotation, scale and swing."}
    )
    @UIRedirect(method = "openEditor", buttonText = "Edit")
    private static boolean heldItemTransforms;

    protected static void openEditor() {
        ScreenManager.navigateConfigScreen(new HeldItemScreen());
    }
    
    @VCListener(BooleanKey.HELD_ITEM_TRANSFORMS)
    private static void onChanged(boolean newValue) {
        enabled = newValue;
        if (runtimeConfig == null) init();
    }

    // --- Config API ---

    private static boolean configChanged = false;
    
    public static void saveChanges() {
        if (configChanged) {
            String jsonData = runtimeConfig.toJson();
            Config.set(StringKey.ITEM_CONFIG, jsonData);
            invalidateSwingCache();
            configChanged = false;
        }
    }
    
    private static void invalidateSwingCache() {
        cachedSwingDirection = null;
    }
    
    public static boolean isEnabled() {
        return enabled;
    }
    
    public static void reset() {
        runtimeConfig.reset();
        saveChanges();
    }    
}
