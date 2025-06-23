package me.wait.fishyaddons;

import me.wait.fishyaddons.config.ConfigHandler;
import me.wait.fishyaddons.config.ParticleColorConfig;
import me.wait.fishyaddons.config.UUIDConfigHandler;
import me.wait.fishyaddons.config.TextureConfig;
import me.wait.fishyaddons.fishyprotection.BlacklistConfigHandler;
import me.wait.fishyaddons.handlers.KeybindHandler;
import me.wait.fishyaddons.handlers.SellProtectionHandler;
import me.wait.fishyaddons.handlers.RetexHandler;
import me.wait.fishyaddons.handlers.AliasHandler;
import me.wait.fishyaddons.handlers.FishyLavaHandler;
import me.wait.fishyaddons.handlers.TextureStitchHandler;
import me.wait.fishyaddons.command.KeybindCommand;
import me.wait.fishyaddons.command.AliasCommand;
import me.wait.fishyaddons.command.FishyAddonsCommand;
import me.wait.fishyaddons.command.ProtectCommand;
import me.wait.fishyaddons.listener.RetexListener;
import me.wait.fishyaddons.listener.WorldEventListener;
import me.wait.fishyaddons.util.SkyblockCheck;
import me.wait.fishyaddons.util.ResourceUtils;
import me.wait.fishyaddons.event.ModelBakeHandler;
import me.wait.fishyaddons.event.ClientConnectedToServer;
import me.wait.fishyaddons.event.ClientDisconnectedFromServer;
import me.wait.fishyaddons.event.ClientChatEvent;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Keyboard;


@Mod(modid = "fishyaddons", name = "FishyAddons", version = FishyAddons.VERSION, clientSideOnly = true)
public class FishyAddons {
    public static final String MODID = "fishyaddons";
    public static final String NAME = "FishyAddons";
    public static final String VERSION = "@VERSION@";

    public static final KeyBinding openGUI = new KeyBinding("Open Keybind List", Keyboard.KEY_NONE, NAME);

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {

        ClientConnectedToServer.init();
        ClientDisconnectedFromServer.init();
        ClientChatEvent.init();
        WorldEventListener.init();
        RetexListener.init();
        ConfigHandler.init();
        UUIDConfigHandler.init();
        TextureConfig.load();
        ResourceUtils.preload();
        BlacklistConfigHandler.loadUserBlacklist();
        ParticleColorConfig.invalidateCache();
        TextureConfig.updateRegistration();
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {

        MinecraftForge.EVENT_BUS.register(this);

        ClientRegistry.registerKeyBinding(openGUI);

        FishyLavaHandler.updateRegistration();
        KeybindHandler.updateRegistration();
        SellProtectionHandler.updateRegistration();
        KeybindHandler.refreshKeybindCache();
        AliasHandler.refreshCommandCache();

        ClientCommandHandler.instance.registerCommand(new KeybindCommand());
        ClientCommandHandler.instance.registerCommand(new FishyAddonsCommand());
        ClientCommandHandler.instance.registerCommand(new AliasCommand());
        ClientCommandHandler.instance.registerCommand(new ProtectCommand());
    }

    @Mod.EventHandler
    public void onServerStopping(FMLServerStoppingEvent event) {
        ConfigHandler.save();
        TextureConfig.save();
        UUIDConfigHandler.save();
    }
}