package me.wait.fishyaddons.listener;

import me.wait.fishyaddons.impl.GhostModel;
import me.wait.fishyaddons.config.TextureConfig;
import me.wait.fishyaddons.handlers.RetexHandler;

import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraft.client.Minecraft;

import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;


@SideOnly(Side.CLIENT)
public class RetexListener implements IResourceManagerReloadListener {
    public static final RetexListener INSTANCE = new RetexListener();

    public static void init () {
        if (TextureConfig.isRetexStatus()) {
            IResourceManager resourceManager = Minecraft.getMinecraft().getResourceManager();
            if (resourceManager instanceof IReloadableResourceManager) {
                ((IReloadableResourceManager) resourceManager).registerReloadListener(RetexListener.INSTANCE);
            } else {
                System.out.println("[FishyAddons] Resource manager is not reloadable. Skipping resource reload listener registration.");
            }
        } 
    }

    @Override
    public void onResourceManagerReload(IResourceManager resourceManager) {
        GhostModel.clearSpriteCache();
        RetexHandler.reloadOverrides();
    }
}
