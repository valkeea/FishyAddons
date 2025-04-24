package me.wait.fishyaddons.handlers;

import me.wait.fishyaddons.config.ConfigHandler;
import me.wait.fishyaddons.config.UUIDConfigHandler;
import me.wait.fishyaddons.util.GuiClick;
import me.wait.fishyaddons.util.PlaySound;

import java.util.Map;
import java.util.WeakHashMap;

import me.wait.fishyaddons.fishyprotection.BlacklistMatcher;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class SellProtectionHandler {
    private static final SellProtectionHandler INSTANCE = new SellProtectionHandler();
    private static final Map<ItemStack, Boolean> protectionCache = new WeakHashMap<>();

    private SellProtectionHandler() {
    }

    public static SellProtectionHandler getInstance() {
        return INSTANCE;
    }

    public static void register() {
        MinecraftForge.EVENT_BUS.register(INSTANCE);
    }

    public static void unregister() {
        MinecraftForge.EVENT_BUS.unregister(INSTANCE);
    }

    public static void updateRegistration() {
        if (UUIDConfigHandler.isSellProtectionEnabled()) {
            register();
        } else {
            unregister();
        }
    }

    public static boolean isProtectedCached(ItemStack stack) {

        if (stack == null || !stack.hasTagCompound()) return false;

        NBTTagCompound extra = stack.getSubCompound("ExtraAttributes", false);
        if (extra == null || !extra.hasKey("uuid")) return false;

        return protectionCache.computeIfAbsent(stack, ProtectedItemHandler::isProtected);
    }

    @SubscribeEvent
    public void onSlotClick(GuiClick.SlotClickEvent event) {
        ItemStack stack;
    
        // Normal slot click
        if (event.getSlot() != null) {
            stack = event.getSlot().getStack();
        } else {
            // Clicking outside while holding an item (drag/drop)
            stack = Minecraft.getMinecraft().thePlayer.inventory.getItemStack();
        }
    
        if (stack == null) return;
        if (!isProtectedCached(stack)) return;
    
        GuiScreen gui = Minecraft.getMinecraft().currentScreen;
        if (!(gui instanceof GuiContainer)) return;
    
        String screenName = gui.getClass().getSimpleName();
        if (BlacklistMatcher.isBlacklistedGUI((GuiContainer) gui, screenName)) {
            event.setCanceled(true);
            PlaySound.playProtectTrigger();            
        }
    } 
}
