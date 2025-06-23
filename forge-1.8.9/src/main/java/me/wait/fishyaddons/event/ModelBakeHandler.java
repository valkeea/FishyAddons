package me.wait.fishyaddons.event;

import me.wait.fishyaddons.impl.GhostModel;
import me.wait.fishyaddons.util.ModelUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import me.wait.fishyaddons.handlers.RetexHandler;
import me.wait.fishyaddons.config.TextureConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;

import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;


@SideOnly(Side.CLIENT)
public class ModelBakeHandler {
    public static final ModelBakeHandler INSTANCE = new ModelBakeHandler();

    private ModelBakeHandler() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onModelBake(ModelBakeEvent event) {
        if (!TextureConfig.isRetexStatus()) {
            return;
        }

        for (String island : RetexHandler.getKnownIslands()) {
            for (String model : RetexHandler.getIslandOverrides(island)) {
                processModel(event, model);
            }
        }            
     }
 
     private void processModel(ModelBakeEvent event, String modelString) {
        ModelResourceLocation loc = ModelUtils.safeLocation(modelString);
        IBakedModel original = event.modelRegistry.getObject(loc);
    
        if (original != null) {
            try {
                IModel unbaked = ModelLoaderRegistry.getModel(loc);
                String originalTexture = ModelUtils.getPrimaryPath(unbaked);
    
                event.modelRegistry.putObject(loc, new GhostModel(original, loc, originalTexture));
            } catch (Exception e) {
                System.err.println("[FishyAddons] Error processing model: " + modelString);
                e.printStackTrace();
            }
        } else {
            System.out.println("[FishyAddons] Warning: Model not found: " + modelString + " (" + loc + ")");
        }
    }    
}
