package me.valkeea.fishyaddons.feature.item.animations;

import java.util.function.DoubleConsumer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

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
import net.minecraft.world.InteractionHand;

@VCModule(UICategory.ITEMS)
public class HeldItems {
    private HeldItems() {}

    public static void applyAllTransformations(PoseStack matrices, InteractionHand hand) {
        if (!isEnabled()) return;

        position(matrices, hand);
        scale(matrices);
        rotation(matrices, hand);
    }

    /**
     * Apply position transformations in world coordinate space
     */
    private static void position(PoseStack matrices, InteractionHand hand) {
        if (isSeparateHandSettings()) {
            separatePosition(matrices, hand);
        } else {
            globalPosition(matrices);
        }
    }

    /**
     * Apply rotation transformations around item center
     */
    private static void rotation(PoseStack matrices, InteractionHand hand) {
        if (isSeparateHandSettings()) {
            separateRotation(matrices, hand);
        } else {
            globalRotation(matrices);
        }
    }

    /**
     * Apply scale transformations
     */
    private static void scale(PoseStack matrices) {
        float scale = (float) getScale();
        matrices.scale(scale, scale, scale);
    }

    private static void separatePosition(PoseStack matrices, InteractionHand hand) {
        boolean isMainHand = hand == InteractionHand.MAIN_HAND;
        
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

    private static void separateRotation(PoseStack matrices, InteractionHand hand) {
        boolean isMainHand = hand == InteractionHand.MAIN_HAND;

        if (isMainHand) {
            matrices.rotate(Axis.XP.rotationDegrees((float) getMainHandRotX()));
            matrices.rotate(Axis.YP.rotationDegrees((float) getMainHandRotY()));
            matrices.rotate(Axis.ZP.rotationDegrees((float) getMainHandRotZ()));
        } else {
            matrices.rotate(Axis.XP.rotationDegrees((float) getOffHandRotX()));
            matrices.rotate(Axis.YP.rotationDegrees((float) getOffHandRotY()));
            matrices.rotate(Axis.ZP.rotationDegrees((float) getOffHandRotZ()));
        }
    }

    private static void globalPosition(PoseStack matrices) {
        matrices.translate(
            getPosOffsetX(),
            getPosOffsetY(),
            getPosOffsetZ()
        );
    }

    private static void globalRotation(PoseStack matrices) {
        matrices.rotate(Axis.XP.rotationDegrees((float) getRotOffsetX()));
        matrices.rotate(Axis.YP.rotationDegrees((float) getRotOffsetY()));
        matrices.rotate(Axis.ZP.rotationDegrees((float) getRotOffsetZ()));
    }


    private static HeldItemModelData config = new HeldItemModelData();
    
    @VCInit
    public static void init() {
        enabled = Config.get(BooleanKey.HELD_ITEM_TRANSFORMS);
        String jsonData = Config.get(StringKey.ITEM_CONFIG);
        config = HeldItemModelData.fromJson(jsonData);
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
        config.mainHandPosX = config.posOffsetX;
        config.mainHandPosY = config.posOffsetY;
        config.mainHandPosZ = config.posOffsetZ;
        config.mainHandRotX = config.rotOffsetX;
        config.mainHandRotY = config.rotOffsetY;
        config.mainHandRotZ = config.rotOffsetZ;
        
        config.offHandPosX = config.posOffsetX;
        config.offHandPosY = config.posOffsetY;
        config.offHandPosZ = config.posOffsetZ;
        config.offHandRotX = config.rotOffsetX;
        config.offHandRotY = config.rotOffsetY;
        config.offHandRotZ = config.rotOffsetZ;
        
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
    
    private record SwingDirection(float xFactor, float yFactor, float zFactor) {}

    private static SwingDirection cachedSwingDirection = null;
    private static float lastRotX = Float.NaN;
    private static float lastRotY = Float.NaN;
    private static float lastRotZ = Float.NaN;
    private static boolean lastSeparateHandSettings = false;    

    // Position
    public static double getPosOffsetX() { return config.posOffsetX; }
    public static double getPosOffsetY() { return config.posOffsetY; }
    public static double getPosOffsetZ() { return config.posOffsetZ; }

    public static void setPosOffsetX(double value) {
        config.posOffsetX = value;
    }
    public static void setPosOffsetY(double value) {
        config.posOffsetY = value;
    }
    public static void setPosOffsetZ(double value) {
        config.posOffsetZ = value;
    }
    
    // Rotation
    public static double getRotOffsetX() { return config.rotOffsetX; }
    public static double getRotOffsetY() { return config.rotOffsetY; }
    public static double getRotOffsetZ() { return config.rotOffsetZ; }

    public static void setRotOffsetX(double value) {
        config.rotOffsetX = value;
        configChanged = true;
    }
    public static void setRotOffsetY(double value) {
        config.rotOffsetY = value;
        configChanged = true;
    }
    public static void setRotOffsetZ(double value) {
        config.rotOffsetZ = value;
        configChanged = true;
    }
    
    // Unified scale
    public static double getScale() { return config.scale; }
    
    public static void setScale(double value) { 
        config.scale = value; 
        configChanged = true;
    }
    
    // Unified Animation Intensities
    public static double getSwingIntensity() { return config.swingIntensity; }
    public static double getEquipIntensity() { return config.equipIntensity; }

    public static double getSwingXMovement() {
        return getSwingDirection().xFactor() * config.swingIntensity;
    }

    public static double getSwingYMovement() {
        return getSwingDirection().yFactor() * config.swingIntensity;
    }

    public static double getSwingZMovement() {
        return getSwingDirection().zFactor() * config.swingIntensity;
    }

    public static void setSwingIntensity(double value) { 
        config.swingIntensity = Math.clamp(value, 0.0, 1.0); 
        configChanged = true;
    }
    
    public static void setEquipIntensity(double value) { 
        config.equipIntensity = Math.clamp(value, 0.0, 1.0); 
        configChanged = true;
    }
    
    // Main hand
    public static double getMainHandPosX() { return config.mainHandPosX; }
    public static double getMainHandPosY() { return config.mainHandPosY; }
    public static double getMainHandPosZ() { return config.mainHandPosZ; }
    public static double getMainHandRotX() { return config.mainHandRotX; }
    public static double getMainHandRotY() { return config.mainHandRotY; }
    public static double getMainHandRotZ() { return config.mainHandRotZ; }
    
    public static void setMainHandPosX(double value) { 
        config.mainHandPosX = value; 
        configChanged = true;
    }
    public static void setMainHandPosY(double value) { 
        config.mainHandPosY = value; 
        configChanged = true;
    }
    public static void setMainHandPosZ(double value) { 
        config.mainHandPosZ = value; 
        configChanged = true;
    }
    public static void setMainHandRotX(double value) { 
        config.mainHandRotX = value; 
        configChanged = true;
    }
    public static void setMainHandRotY(double value) { 
        config.mainHandRotY = value; 
        configChanged = true;
    }
    public static void setMainHandRotZ(double value) { 
        config.mainHandRotZ = value; 
        configChanged = true;
    }
    
    // Offhand
    public static double getOffHandPosX() { return config.offHandPosX; }
    public static double getOffHandPosY() { return config.offHandPosY; }
    public static double getOffHandPosZ() { return config.offHandPosZ; }
    public static double getOffHandRotX() { return config.offHandRotX; }
    public static double getOffHandRotY() { return config.offHandRotY; }
    public static double getOffHandRotZ() { return config.offHandRotZ; }
    
    public static void setOffHandPosX(double value) { 
        config.offHandPosX = value; 
        configChanged = true;
    }
    public static void setOffHandPosY(double value) { 
        config.offHandPosY = value; 
        configChanged = true;
    }
    public static void setOffHandPosZ(double value) { 
        config.offHandPosZ = value; 
        configChanged = true;
    }
    public static void setOffHandRotX(double value) { 
        config.offHandRotX = value; 
        configChanged = true;
    }
    public static void setOffHandRotY(double value) { 
        config.offHandRotY = value; 
        configChanged = true;
    }
    public static void setOffHandRotZ(double value) { 
        config.offHandRotZ = value; 
        configChanged = true;
    }
    
    // Mode
    public static boolean isSeparateHandSettings() { return config.separateHandSettings; }
    
    public static void setSeparateHandSettings(boolean enabled) { 
        config.separateHandSettings = enabled; 
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
        if (config == null) init();
    }

    // --- Config API ---

    private static boolean configChanged = false;
    
    public static void saveChanges() {
        if (configChanged) {
            String jsonData = config.toJson();
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
        config.reset();
        saveChanges();
    }    
}
