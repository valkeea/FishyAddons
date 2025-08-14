package me.valkeea.fishyaddons;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import me.valkeea.fishyaddons.command.CmdManager;
import me.valkeea.fishyaddons.config.FishyConfig;
import me.valkeea.fishyaddons.config.FishyPresets;
import me.valkeea.fishyaddons.config.ItemConfig;
import me.valkeea.fishyaddons.event.FishyKeys;
import me.valkeea.fishyaddons.handler.CakeTimer;
import me.valkeea.fishyaddons.handler.ChatAlert;
import me.valkeea.fishyaddons.handler.ChatReplacement;
import me.valkeea.fishyaddons.handler.CommandAlias;
import me.valkeea.fishyaddons.handler.CopyChat;
import me.valkeea.fishyaddons.handler.KeyShortcut;
import me.valkeea.fishyaddons.handler.MobAnimations;
import me.valkeea.fishyaddons.handler.ParticleVisuals;
import me.valkeea.fishyaddons.handler.PetInfo;
import me.valkeea.fishyaddons.handler.ResourceHandler;
import me.valkeea.fishyaddons.handler.SkyblockCleaner;
import me.valkeea.fishyaddons.handler.XpColor;
import me.valkeea.fishyaddons.hud.CakeDisplay;
import me.valkeea.fishyaddons.hud.ElementRegistry;
import me.valkeea.fishyaddons.hud.FishyToast;
import me.valkeea.fishyaddons.hud.PetDisplay;
import me.valkeea.fishyaddons.hud.PingDisplay;
import me.valkeea.fishyaddons.hud.SearchHudElement;
import me.valkeea.fishyaddons.hud.TimerDisplay;
import me.valkeea.fishyaddons.hud.TitleDisplay;
import me.valkeea.fishyaddons.hud.TrackerDisplay;
import me.valkeea.fishyaddons.listener.ClientChat;
import me.valkeea.fishyaddons.listener.ClientConnected;
import me.valkeea.fishyaddons.listener.ClientDisconnected;
import me.valkeea.fishyaddons.listener.ClientTick;
import me.valkeea.fishyaddons.listener.ModifyChat;
import me.valkeea.fishyaddons.listener.WorldEvent;
import me.valkeea.fishyaddons.render.BeaconRenderer;
import me.valkeea.fishyaddons.tool.FishyMode;
import me.valkeea.fishyaddons.tool.GuiScheduler;
import me.valkeea.fishyaddons.tracker.ItemTrackerData;
import me.valkeea.fishyaddons.tracker.SackDropParser;
import me.valkeea.fishyaddons.tracker.TrackerUtils;
import me.valkeea.fishyaddons.util.PlaySound;
import me.valkeea.fishyaddons.util.ScoreboardUtils;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;


public class FishyAddons implements ClientModInitializer {
    public static final String MOD_ID = "fishyaddons";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        FishyConfig.init();
        ItemConfig.init();
        FishyKeys.register();
        KeyShortcut.refreshCache();
        ChatReplacement.refreshCache();
        CommandAlias.refreshCache();
        ParticleVisuals.refreshCache();
        FishyMode.getTheme();
        ChatAlert.refresh();
        CopyChat.refresh();
        PetInfo.refresh();
        XpColor.refresh();
        MobAnimations.refresh();
        SkyblockCleaner.refresh();
        TrackerUtils.refresh();
        SackDropParser.refresh();
        GuiScheduler.register();
        CmdManager.register();  

        ClientTick.init();
        ModifyChat.init();
        WorldEvent.init();
        ClientConnected.init();
        ClientDisconnected.init();
        CakeTimer.getInstance().init();
        BeaconRenderer.init();
        ClientChat.init();
        FishyToast.init();

        PingDisplay pingDisplay = new PingDisplay();
        TimerDisplay timerDisplay = new TimerDisplay();
        TitleDisplay titleDisplay = new TitleDisplay();
        PetDisplay petDisplay = new PetDisplay();
        TrackerDisplay trackerDisplay = new TrackerDisplay();
        SearchHudElement searchHudElement = new SearchHudElement();
        CakeDisplay centuryCakeDisplay = new CakeDisplay();

        ResourceHandler.register();
        ElementRegistry.register(pingDisplay);
        ElementRegistry.register(timerDisplay);
        ElementRegistry.register(titleDisplay);
        ElementRegistry.register(petDisplay);
        ElementRegistry.register(trackerDisplay);
        ElementRegistry.register(searchHudElement);
        ElementRegistry.register(centuryCakeDisplay);
        
        pingDisplay.register();
        timerDisplay.register();
        titleDisplay.register();
        petDisplay.register();
        trackerDisplay.register();
        searchHudElement.register();
        centuryCakeDisplay.register();

        ItemTrackerData.init();
        FishyPresets.ensureDefaultPresets();

        me.valkeea.fishyaddons.handler.ItemSearchOverlay searchOverlay = me.valkeea.fishyaddons.handler.ItemSearchOverlay.getInstance();
        searchOverlay.setSearchHudElement(searchHudElement);

        Registry.register(Registries.SOUND_EVENT, PlaySound.PROTECT_TRIGGER_ID, PlaySound.PROTECT_TRIGGER_EVENT);

        KeyBinding mainKey = KeyBindingHelper.registerKeyBinding(
            new KeyBinding("Open FishyAddons gui", InputUtil.Type.KEYSYM, 240, "FishyAddons")
        );

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (mainKey.wasPressed()) {
                MinecraftClient.getInstance().setScreen(
                    new me.valkeea.fishyaddons.gui.FishyAddonsScreen()
                );
            }
        });
    }
}