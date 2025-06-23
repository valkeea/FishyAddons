package me.valkeea.fishyaddons;

import me.valkeea.fishyaddons.command.CmdManager;
import me.valkeea.fishyaddons.config.FishyConfig;
import me.valkeea.fishyaddons.config.ItemConfig;
import me.valkeea.fishyaddons.event.FishyKeys;
import me.valkeea.fishyaddons.handler.ChatReplacement;
import me.valkeea.fishyaddons.handler.KeyShortcut;
import me.valkeea.fishyaddons.handler.CommandAlias;
import me.valkeea.fishyaddons.handler.SkyblockCleaner;
import me.valkeea.fishyaddons.hud.ElementRegistry;
import me.valkeea.fishyaddons.hud.FishyToast;
import me.valkeea.fishyaddons.hud.PingDisplay;
import me.valkeea.fishyaddons.hud.TimerDisplay;
import me.valkeea.fishyaddons.listener.ClientChat;
import me.valkeea.fishyaddons.listener.ClientConnected;
import me.valkeea.fishyaddons.listener.ClientDisconnected;
import me.valkeea.fishyaddons.listener.ClientTick;
import me.valkeea.fishyaddons.listener.ModifyChat;
import me.valkeea.fishyaddons.listener.WorldEvent;
import me.valkeea.fishyaddons.render.BeaconRenderer;
import me.valkeea.fishyaddons.gui.FishyAddonsScreen;
import me.valkeea.fishyaddons.tool.GuiScheduler;
import me.valkeea.fishyaddons.util.PlaySound;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


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
        SkyblockCleaner.refresh();
        GuiScheduler.register();
        CmdManager.register();  

        ClientTick.init();
        ModifyChat.init();
        WorldEvent.init();
        ClientConnected.init();
        ClientDisconnected.init();
        BeaconRenderer.init();
        ClientChat.init();
        FishyToast.init();

        PingDisplay pingDisplay = new PingDisplay();
        TimerDisplay timerDisplay = new TimerDisplay();
        ElementRegistry.register(pingDisplay);
        ElementRegistry.register(timerDisplay);
        pingDisplay.register();
        timerDisplay.register();        

        Registry.register(Registries.SOUND_EVENT, PlaySound.PROTECT_TRIGGER_ID, PlaySound.PROTECT_TRIGGER_EVENT);

        KeyBinding mainKey = KeyBindingHelper.registerKeyBinding(
            new KeyBinding("Open FishyAddons gui", InputUtil.Type.KEYSYM, -1, "FishyAddons")
        );

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (mainKey.wasPressed()) {
                client.setScreen(new FishyAddonsScreen());
            }
        });
    }
}
