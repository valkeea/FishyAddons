package me.valkeea.fishyaddons.gui;

import java.util.List;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;

public class SearchList {
    private static MinecraftClient client = MinecraftClient.getInstance();
    private static Screen screen = client.currentScreen;

    public static final List<SearchEntry> FEATURES = List.of(
        new SearchEntry("Slotlocking", "Protect your slots from undesired \nactions.", () -> { Qol(); }),
        new SearchEntry("Slotbinding", 
        "Bind slots by dragging the lock key\n(default L). Shift clicking will swap\nthe items between these slots.", () -> { Qol(); }),
        new SearchEntry("Item protection", "Protect any item with a uuid", () -> { Qol(); }),
        new SearchEntry("Clean Wither Impact/Hyperion", "Removes particles and sound.", () -> { Qol(); }),
        new SearchEntry("Custom F5", "No front perspective.", () -> { Qol(); }),
        new SearchEntry("Coordinate beacon/Waypoint", "Render a beacon on coordinates \nmatching x: %, y: %, z: %.", () -> { Qol(); }),
        new SearchEntry("Ping command and display", "Use /fa hud to edit the element", () -> { Qol(); }),
        new SearchEntry("Moonglade Beacon alarm and display", "Toast alarm and HUD timer.", () -> { Skills();}),
        new SearchEntry("Mute Phantoms", "Disables phantom sounds.", () -> { Skills();}),        
        new SearchEntry("Clear lava", "Removes lava fog when submerged.", () -> { Visual();}),
        new SearchEntry("Custom redstone particles", "Alter the color of Jawbus beam and \nFlaming Flay ability.", () -> { Visual();}),
        new SearchEntry("Keybinds", "Create and toggle keybinds", () -> { Keybinds();}),
        new SearchEntry("Command Aliases", "Create your own versions \nof existing commands.", () -> { Aliases();}),
        new SearchEntry("Modify Chat", "Create replacements for text you \nsend in chat", () -> { Chat();}),
        new SearchEntry("Chat Event-based alerts", "Title alert, automated chat message, \nsound alarm", ( ) -> { Alerts();}),
        new SearchEntry("Visual Settings", "Purely visual -related settings.", () -> { Visual();}),
        new SearchEntry("Skill Settings", "Foraging/Galatea -related settings.", () -> { Skills();}),
        new SearchEntry("HUD Editor", "Edit all HUD elements.", () -> { Hud();}),
        new SearchEntry("Safeguard", "Protect your items from drops\nblock clicks in blacklisted guis.", () -> { Guard();}),
        new SearchEntry("General Qol", "Quality of life improvements.", () -> { Qol();}),
        new SearchEntry("Alert Settings", "Manage chat alerts.", () -> { Alerts();})
    );

    private static void Visual () { client.setScreen(new VisualSettingsScreen()); }
    private static void Skills () { client.setScreen(new SkillScreen()); }
    private static void Keybinds () { client.setScreen(new TabbedListScreen(screen, TabbedListScreen.Tab.KEYBINDS)); }
    private static void Aliases () { client.setScreen(new TabbedListScreen(screen, TabbedListScreen.Tab.COMMANDS)); }
    private static void Chat () { client.setScreen(new TabbedListScreen(screen, TabbedListScreen.Tab.CHAT)); }
    private static void Alerts () { client.setScreen(new TabbedListScreen(screen, TabbedListScreen.Tab.ALERT)); }
    private static void Hud () { client.setScreen(new HudEditScreen()); }
    private static void Guard () { client.setScreen(new SafeguardScreen()); }
    private static void Qol () { client.setScreen(new QolScreen()); }    
}