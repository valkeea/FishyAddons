package me.valkeea.fishyaddons;

import com.mojang.blaze3d.platform.InputConstants;

import me.valkeea.fishyaddons.api.skyblock.GameChat;
import me.valkeea.fishyaddons.command.CmdManager;
import me.valkeea.fishyaddons.feature.filter.FilterConfig;
import me.valkeea.fishyaddons.feature.item.safeguard.BlacklistManager;
import me.valkeea.fishyaddons.feature.qol.FishyKeys;
import me.valkeea.fishyaddons.feature.qol.FishyPresets;
import me.valkeea.fishyaddons.feature.waypoints.WaypointChains;
import me.valkeea.fishyaddons.hud.core.ElementRegistry;
import me.valkeea.fishyaddons.hud.ui.FishyToast;
import me.valkeea.fishyaddons.listener.*;
import me.valkeea.fishyaddons.processor.ChatHandlerRegistry;
import me.valkeea.fishyaddons.tool.GuiScheduler;
import me.valkeea.fishyaddons.tool.ModCheck;
import me.valkeea.fishyaddons.tool.PlaySound;
import me.valkeea.fishyaddons.tracker.profit.InventorySnapshot;
import me.valkeea.fishyaddons.tracker.profit.ValuableMobs;
import me.valkeea.fishyaddons.util.ContainerScanner;
import me.valkeea.fishyaddons.util.CustomSounds;
import me.valkeea.fishyaddons.util.text.GradientRenderer;
import me.valkeea.fishyaddons.vconfig.api.Config;
import me.valkeea.fishyaddons.vconfig.core.ConfigRegistry;
import me.valkeea.fishyaddons.vconfig.core.ConfigScanner;
import me.valkeea.fishyaddons.vconfig.ui.manager.ScreenManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

public class FishyAddons implements ClientModInitializer {

    @Override
    public void onInitializeClient() {

        Config.init();
        FilterConfig.init(); 
        ConfigScanner.scanDefaultPackages();
        ConfigRegistry.initializeAll();

        GameChat.init();
        GradientRenderer.init();
        ValuableMobs.refresh();
        ModifyChat.init();

        ContainerScanner.getInstance().init();       
        CustomSounds.init();
        ClientTick.init();
        WorldEvent.init();
        ClientStop.init();
        ClientConnected.init();
        ClientDisconnected.init();
        InventorySnapshot.init();
        ChatHandlerRegistry.init();
        FishyToast.init();
        ModCheck.init();

        ElementRegistry.init();       
        FishyPresets.ensureDefaultPresets();
        WaypointChains.init();
        
        FishyKeys.register();        
        GuiScheduler.register();
        CmdManager.register();
        BlacklistManager.init(); 
        
        Registry.register(BuiltInRegistries.SOUND_EVENT,
            PlaySound.PROTECT_TRIGGER_ID,
            PlaySound.PROTECT_TRIGGER_EVENT
        );

        var mainKey = KeyMappingHelper.registerKeyMapping(
            new KeyMapping("Open Config", InputConstants.Type.KEYSYM, 240, KeyMapping.Category.register(Identifier.fromNamespaceAndPath("fishyaddons", "mod_keybinds")))
        );

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (mainKey.consumeClick()) {
                ScreenManager.openConfigScreen();
            }
        });
    }
}
