package me.valkeea.fishyaddons.api.skyblock;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Stores information about the player's SkyBlock profile, such as profile name, selected profile, and other relevant data.
 */
public class Profile {
    private static String userName = "";

    /** Return the current username or empty if not initialized */
    public static String getUserName() {
        return userName;
    }

    public static void setUserName(String userName) {
        Profile.userName = userName;
    }

    /** Attempt to initialize the username from an ItemStack (player head) */
    public static void initUsername(ItemStack s) {
        if (s == null || s.isEmpty() || !s.getItem().equals(Items.PLAYER_HEAD)) return;

        var profile = s.get(DataComponents.PROFILE);
        if (profile == null) return;

        var username = profile.name().orElse("");
        if (!username.isEmpty()) setUserName(username);       
    }

    private Profile() {}    
}
