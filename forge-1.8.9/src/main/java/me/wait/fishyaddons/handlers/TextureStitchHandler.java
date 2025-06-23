package me.wait.fishyaddons.handlers;

import me.wait.fishyaddons.config.TextureConfig;
import me.wait.fishyaddons.util.ModelUtils;
import me.wait.fishyaddons.impl.GhostModel;
import me.wait.fishyaddons.handlers.RetexHandler;
import net.minecraft.util.ResourceLocation;

import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.Arrays;
import java.util.Map;


@SideOnly(Side.CLIENT)
public class TextureStitchHandler {

    public static final TextureStitchHandler INSTANCE = new TextureStitchHandler();

    private TextureStitchHandler() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onTextureStitch(TextureStitchEvent.Pre event) {
        if (!TextureConfig.isRetexStatus()) {
            System.out.println("[FishyAddons] Retexturing is disabled. Skipping texture stitch.");
            return;
        }
    
        for (String island : RetexHandler.getKnownIslands()) {
            List<String> models = RetexHandler.getIslandOverrides(island);
            for (String model : models) {
                String texPath = ModelUtils.extractBasePath(model);
                ResourceLocation textureLoc = new ResourceLocation("fishyaddons:islands/" + island + "/" + texPath);
                event.map.registerSprite(textureLoc);
            }
        } 
        
        for (String island : RetexHandler.getForced()) {
            for (Map.Entry<String, String[]> entry : ModelUtils.VARIANT_SUFFIXES.entrySet()) {
                String modelKey = entry.getKey();
                String textureBase = ModelUtils.MODEL_TO_TEXTURE_BASE.getOrDefault(modelKey, modelKey);
                for (String suffix : entry.getValue()) {
                    String texName;
                    if (modelKey.contains("stained_glass_pane")) {
                        if (suffix.equals("side")) {
                            texName = "glass_" + modelKey.replace("_stained_glass_pane", "");
                        } else if (suffix.equals("top")) {
                            texName = "glass_pane_top_" + modelKey.replace("_stained_glass_pane", "");
                        } else {
                            texName = textureBase + "_" + suffix;
                        }
                    } else {
                        texName = textureBase + "_" + suffix;
                    }
                    ResourceLocation texLoc = new ResourceLocation("fishyaddons:islands/" + island + "/" + texName);
                    event.map.registerSprite(texLoc);
                }
            }
        }
    }   
}

