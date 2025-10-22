package me.valkeea.fishyaddons;

import me.valkeea.fishyaddons.api.skyblock.GameChat;
import me.valkeea.fishyaddons.command.CmdManager;
import me.valkeea.fishyaddons.config.FishyConfig;
import me.valkeea.fishyaddons.config.FishyPresets;
import me.valkeea.fishyaddons.config.ItemConfig;
import me.valkeea.fishyaddons.config.StatConfig;
import me.valkeea.fishyaddons.event.FishyKeys;
import me.valkeea.fishyaddons.handler.ActiveBeacons;
import me.valkeea.fishyaddons.handler.CakeTimer;
import me.valkeea.fishyaddons.handler.ChatAlert;
import me.valkeea.fishyaddons.handler.ChatReplacement;
import me.valkeea.fishyaddons.handler.ChatTimers;
import me.valkeea.fishyaddons.handler.CommandAlias;
import me.valkeea.fishyaddons.handler.CopyChat;
import me.valkeea.fishyaddons.handler.FaColors;
import me.valkeea.fishyaddons.handler.FishingHotspot;
import me.valkeea.fishyaddons.handler.GuiIcons;
import me.valkeea.fishyaddons.handler.HeldItems;
import me.valkeea.fishyaddons.handler.KeyShortcut;
import me.valkeea.fishyaddons.handler.MobAnimations;
import me.valkeea.fishyaddons.handler.NetworkMetrics;
import me.valkeea.fishyaddons.handler.ParticleVisuals;
import me.valkeea.fishyaddons.handler.PetInfo;
import me.valkeea.fishyaddons.handler.RenderTweaks;
import me.valkeea.fishyaddons.handler.ResourceHandler;
import me.valkeea.fishyaddons.handler.SkyblockCleaner;
import me.valkeea.fishyaddons.handler.XpColor;
import me.valkeea.fishyaddons.hud.ElementRegistry;
import me.valkeea.fishyaddons.hud.FishyToast;
import me.valkeea.fishyaddons.listener.ClientChat;
import me.valkeea.fishyaddons.listener.ClientConnected;
import me.valkeea.fishyaddons.listener.ClientDisconnected;
import me.valkeea.fishyaddons.listener.ClientStop;
import me.valkeea.fishyaddons.listener.ClientTick;
import me.valkeea.fishyaddons.listener.ModifyChat;
import me.valkeea.fishyaddons.listener.WorldEvent;
import me.valkeea.fishyaddons.tool.FishyMode;
import me.valkeea.fishyaddons.tool.GuiScheduler;
import me.valkeea.fishyaddons.tool.ModCheck;
import me.valkeea.fishyaddons.tool.PlaySound;
import me.valkeea.fishyaddons.tracker.ActivityMonitor;
import me.valkeea.fishyaddons.tracker.ItemTrackerData;
import me.valkeea.fishyaddons.tracker.SackDropParser;
import me.valkeea.fishyaddons.tracker.SkillTracker;
import me.valkeea.fishyaddons.tracker.TrackerUtils;
import me.valkeea.fishyaddons.tracker.ValuableMobs;
import me.valkeea.fishyaddons.tracker.fishing.ScData;
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
        TrackerUtils.refresh();
        ValuableMobs.refresh();
        SackDropParser.refresh();
        NetworkMetrics.refresh();
        ActiveBeacons.refresh();
        FishingHotspot.refresh();
        ActivityMonitor.refresh();
        ChatTimers.getInstance().refresh();

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
        CakeTimer.getInstance().init();
        ActiveBeacons.init();
        ClientChat.init();
        FishyToast.init();
        ModCheck.init();
        ElementRegistry.init();
        ItemTrackerData.init();        

        FishyPresets.ensureDefaultPresets();

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