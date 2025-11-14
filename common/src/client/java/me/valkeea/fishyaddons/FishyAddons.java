package me.valkeea.fishyaddons;

import me.valkeea.fishyaddons.api.skyblock.GameChat;
import me.valkeea.fishyaddons.command.CmdManager;
import me.valkeea.fishyaddons.config.FishyConfig;
import me.valkeea.fishyaddons.config.FishyPresets;
import me.valkeea.fishyaddons.config.ItemConfig;
import me.valkeea.fishyaddons.config.StatConfig;
import me.valkeea.fishyaddons.feature.item.animations.HeldItems;
import me.valkeea.fishyaddons.feature.item.safeguard.GuiHandler;
import me.valkeea.fishyaddons.feature.item.safeguard.SlotHandler;
import me.valkeea.fishyaddons.feature.qol.ChatAlert;
import me.valkeea.fishyaddons.feature.qol.ChatReplacement;
import me.valkeea.fishyaddons.feature.qol.CommandAlias;
import me.valkeea.fishyaddons.feature.qol.CopyChat;
import me.valkeea.fishyaddons.feature.qol.FishyKeys;
import me.valkeea.fishyaddons.feature.qol.ItemSearchOverlay;
import me.valkeea.fishyaddons.feature.qol.KeyShortcut;
import me.valkeea.fishyaddons.feature.qol.NetworkMetrics;
import me.valkeea.fishyaddons.feature.skyblock.CakeTimer;
import me.valkeea.fishyaddons.feature.skyblock.ChatTimers;
import me.valkeea.fishyaddons.feature.skyblock.FishingHotspot;
import me.valkeea.fishyaddons.feature.skyblock.GuiIcons;
import me.valkeea.fishyaddons.feature.skyblock.PetInfo;
import me.valkeea.fishyaddons.feature.skyblock.SkyblockCleaner;
import me.valkeea.fishyaddons.feature.skyblock.TransLava;
import me.valkeea.fishyaddons.feature.visual.FaColors;
import me.valkeea.fishyaddons.feature.visual.MobAnimations;
import me.valkeea.fishyaddons.feature.visual.ParticleVisuals;
import me.valkeea.fishyaddons.feature.visual.RenderTweaks;
import me.valkeea.fishyaddons.feature.visual.ResourceHandler;
import me.valkeea.fishyaddons.feature.visual.XpColor;
import me.valkeea.fishyaddons.feature.waypoints.TempWaypoint;
import me.valkeea.fishyaddons.hud.core.ElementRegistry;
import me.valkeea.fishyaddons.hud.ui.FishyToast;
import me.valkeea.fishyaddons.listener.ClientConnected;
import me.valkeea.fishyaddons.listener.ClientDisconnected;
import me.valkeea.fishyaddons.listener.ClientStop;
import me.valkeea.fishyaddons.listener.ClientTick;
import me.valkeea.fishyaddons.listener.ModifyChat;
import me.valkeea.fishyaddons.listener.WorldEvent;
import me.valkeea.fishyaddons.processor.ChatHandlerRegistry;
import me.valkeea.fishyaddons.tool.FishyMode;
import me.valkeea.fishyaddons.tool.GuiScheduler;
import me.valkeea.fishyaddons.tool.ModCheck;
import me.valkeea.fishyaddons.tool.PlaySound;
import me.valkeea.fishyaddons.tracker.ActivityMonitor;
import me.valkeea.fishyaddons.tracker.SkillTracker;
import me.valkeea.fishyaddons.tracker.fishing.ScData;
import me.valkeea.fishyaddons.tracker.profit.ItemTrackerData;
import me.valkeea.fishyaddons.tracker.profit.SackDropParser;
import me.valkeea.fishyaddons.tracker.profit.TrackerUtils;
import me.valkeea.fishyaddons.tracker.profit.ValuableMobs;
import me.valkeea.fishyaddons.util.CustomSounds;
import me.valkeea.fishyaddons.util.text.GradientRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public class FishyAddons implements ClientModInitializer {

    @Override
    public void onInitializeClient() {

        FishyConfig.init();
        StatConfig.init();
        ItemConfig.init();

        GameChat.init();
        GradientRenderer.init();

        KeyShortcut.refresh();
        ChatReplacement.refresh();
        CommandAlias.refresh();
        GuiIcons.refresh();
        ParticleVisuals.refreshCache();
        ChatAlert.refresh();
        CopyChat.refresh();
        PetInfo.refresh();
        XpColor.refresh();
        ScData.refresh();
        SkillTracker.refresh();
        RenderTweaks.refresh();
        MobAnimations.refresh();
        SkyblockCleaner.refresh();
        ValuableMobs.refresh();
        SackDropParser.refresh();
        NetworkMetrics.refresh();
        TempWaypoint.refresh();
        FishingHotspot.refresh();
        ActivityMonitor.refresh();
        ChatTimers.getInstance().refresh();

        TrackerUtils.init();  
        TransLava.init();      
        HeldItems.init();        
        CustomSounds.init();
        FaColors.init();
        FishyMode.init();
        ClientTick.init();
        ModifyChat.init();
        WorldEvent.init();
        ClientStop.init();
        ClientConnected.init();
        ClientDisconnected.init();
        GuiHandler.init();
        SlotHandler.init();
        ItemSearchOverlay.init();
        GuiIcons.init();
        CakeTimer.getInstance().init();
        TempWaypoint.init();
        ChatHandlerRegistry.init();
        FishyToast.init();
        ModCheck.init();
        ElementRegistry.init();
        ItemTrackerData.init();        

        FishyPresets.ensureDefaultPresets();
        me.valkeea.fishyaddons.feature.waypoints.WaypointChains.init();
        
        FishyKeys.register();        
        GuiScheduler.register();
        CmdManager.register();  
        ResourceHandler.register();
        Registry.register(Registries.SOUND_EVENT, PlaySound.PROTECT_TRIGGER_ID, PlaySound.PROTECT_TRIGGER_EVENT);

        KeyBinding mainKey = KeyBindingHelper.registerKeyBinding(
            new KeyBinding("Open Config", InputUtil.Type.KEYSYM, 240, "FishyAddons")
        );

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (mainKey.wasPressed()) {
                MinecraftClient.getInstance().setScreen(
                    new me.valkeea.fishyaddons.ui.VCScreen()
                );
            }
        });
    }
}
