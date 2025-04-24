package me.wait.fishyaddons.fishyprotection;

import me.wait.fishyaddons.util.GuiBlacklistEntry;
import me.wait.fishyaddons.config.UUIDConfigHandler;
import net.minecraft.item.ItemStack;
import net.minecraft.client.gui.inventory.GuiContainer;

import java.util.*;

public class BlacklistStore {
    private static final List<GuiBlacklistEntry> defaultBlacklist = new ArrayList<>();
    private static final List<GuiBlacklistEntry> userBlacklist = new ArrayList<>();

    static {
        defaultBlacklist.add(new GuiBlacklistEntry(Arrays.asList("Create Auction",
                "Create BIN Auction",
                "Auction House"), true, true));
        defaultBlacklist.add(new GuiBlacklistEntry(Arrays.asList("Coins Transaction"), true, false));
        defaultBlacklist.add(new GuiBlacklistEntry(Arrays.asList("Salvage Items"), true, true));
        defaultBlacklist.add(new GuiBlacklistEntry(Arrays.asList(
                "Sell Item",
                "Click items in your inventory to sell",
                "Click to buyback"), true, false));

        BlacklistConfigHandler.loadUserBlacklist();
    }

    public static List<GuiBlacklistEntry> getMergedBlacklist() {
        List<GuiBlacklistEntry> merged = new ArrayList<>();

        for (GuiBlacklistEntry def : defaultBlacklist) {
            GuiBlacklistEntry override = findUserOverride(def);
            if (override != null) {
                merged.add(new GuiBlacklistEntry(def.identifiers, override.enabled, def.checkTitle));
            } else {
                merged.add(def);
            }
        }

        for (GuiBlacklistEntry userEntry : userBlacklist) {
            boolean overridesExisting = false;
            for (GuiBlacklistEntry def : defaultBlacklist) {
                if (matchesIdentifier(def, userEntry.identifiers.get(0))) {
                    overridesExisting = true;
                    break;
                }
            }
            if (!overridesExisting) {
                merged.add(userEntry);
            }
        }

        return merged;
    }

    public static List<GuiBlacklistEntry> getUserBlacklist() {
        return new ArrayList<>(userBlacklist);
    }

    public static void updateBlacklistEntry(String identifier, boolean enabled) {
        for (GuiBlacklistEntry entry : userBlacklist) {
            for (String id : entry.identifiers) {
                if (id.equalsIgnoreCase(identifier)) {
                    entry.enabled = enabled;
                    saveUserBlacklist();
                    return;
                }
            }
        }

        for (GuiBlacklistEntry def : defaultBlacklist) {
            for (String id : def.identifiers) {
                if (id.equalsIgnoreCase(identifier)) {
                    if (def.enabled == enabled) return;
                    break;
                }
            }
        }

        userBlacklist.add(new GuiBlacklistEntry(Collections.singletonList(identifier), enabled, false));
        saveUserBlacklist();
    }

    private static GuiBlacklistEntry findUserOverride(GuiBlacklistEntry def) {
        for (String id : def.identifiers) {
            for (GuiBlacklistEntry userEntry : userBlacklist) {
                if (matchesIdentifier(userEntry, id)) {
                    return userEntry;
                }
            }
        }
        return null;
    }

    private static boolean matchesIdentifier(GuiBlacklistEntry entry, String identifier) {
        for (String id : entry.identifiers) {
            if (id.equalsIgnoreCase(identifier)) {
                return true;
            }
        }
        return false;
    }

    public static void saveUserBlacklist() {
        BlacklistConfigHandler.getUserBlacklist();
        UUIDConfigHandler.markConfigChanged();
    }  
}
