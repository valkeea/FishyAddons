package me.valkeea.fishyaddons.gui;

import java.util.List;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;

public class SearchList {
    private SearchList() {}
    private static MinecraftClient client = MinecraftClient.getInstance();
    private static Screen screen = client.currentScreen;

    public static final List<SearchEntry> FEATURES = List.of(
        new SearchEntry("Slotlocking", "Protect your slots from undesired \nactions.", () -> qol()),
        new SearchEntry("Slotbinding", 
        "Bind slots by dragging the lock key\n(default ö). Shift clicking will swap\nthe items between these slots.", () -> qol()),
        new SearchEntry("Item protection", "Protect any item with a uuid.", () -> qol()),
        new SearchEntry("Clean Wither Impact/Hyperion", "Removes particles and sound.", () -> qol()),
        new SearchEntry("Pet Display", "Hud element for showing currently\nactive pet.", () -> visual()),
        new SearchEntry("Skip Mob Death Animation", "Prevents rendering if an entity\nis already dead.", () -> qol()),
        new SearchEntry("Skip Fire Animation", "Prevents fire from rendering\non entities.", () -> qol()),
        new SearchEntry("Custom F5", "No front perspective.", () -> qol()),
        new SearchEntry("Coordinate beacon/Waypoint", "Render a beacon on coordinates \nmatching x: %, y: %, z: %.", () -> qol()),
        new SearchEntry("Ping command and display", "Use /fa hud to edit the element.", () -> qol()),
        new SearchEntry("Moonglade Beacon alarm and display", "Toast alarm and HUD timer.", () -> skills()),
        new SearchEntry("Mute Phantoms", "Disables phantom sounds.", () -> skills()),        
        new SearchEntry("Clear lava", "Removes lava fog when submerged.", () -> visual()),
        new SearchEntry("Custom redstone particles", "Alter the color of Jawbus beam and \nFlaming Flay ability.", () -> visual()),
        new SearchEntry("Keybinds", "Create and toggle keybinds", () -> keybinds()),
        new SearchEntry("Command Aliases", "Create your own versions \nof existing commands.", () -> aliases()),
        new SearchEntry("Modify Chat", "Create replacements for text you \nsend in chat.", () -> chat()),
        new SearchEntry("Chat Event-based alerts", "Title alert, automated chat message, \nsound alarm.", ( ) -> alerts()),
        new SearchEntry("Visual Settings", "Purely visual -related settings.", () -> visual()),
        new SearchEntry("XP Bar Text", "Customize the color of\nvanilla XP bar text.", () -> visual()),
        new SearchEntry("HD Font", "Use a builtin resource pack for font", () -> visual()),
        new SearchEntry("Transparent guis and hud", "Use a builtin resource pack\n(WIP) migrating from 1.8.9", () -> visual()),
        new SearchEntry("Skill Settings", "Foraging/Galatea -related settings.", () -> skills()),
        new SearchEntry("HUD Editor", "Edit all HUD elements.", () -> hud()),
        new SearchEntry("Safeguard", "Protect your items from drops\nblock clicks in blacklisted guis.", () -> guard()),
        new SearchEntry("General Qol", "Quality of life improvements.", () -> qol()),
        new SearchEntry("Alert Settings", "Manage chat alerts.", () -> alerts())
    );

    private static void visual () { client.setScreen(new VisualSettingsScreen()); }
    private static void skills () { client.setScreen(new SkillScreen()); }
    private static void keybinds () { client.setScreen(new TabbedListScreen(screen, TabbedListScreen.Tab.KEYBINDS)); }
    private static void aliases () { client.setScreen(new TabbedListScreen(screen, TabbedListScreen.Tab.COMMANDS)); }
    private static void chat () { client.setScreen(new TabbedListScreen(screen, TabbedListScreen.Tab.CHAT)); }
    private static void alerts () { client.setScreen(new TabbedListScreen(screen, TabbedListScreen.Tab.ALERT)); }
    private static void hud () { client.setScreen(new HudEditScreen()); }
    private static void guard () { client.setScreen(new SafeguardScreen()); }
    private static void qol () { client.setScreen(new QolScreen()); }    
}