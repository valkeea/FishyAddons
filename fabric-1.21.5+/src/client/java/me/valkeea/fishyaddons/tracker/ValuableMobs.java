package me.valkeea.fishyaddons.tracker;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import me.valkeea.fishyaddons.util.NearbyEntities;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;

public class ValuableMobs {
    private static boolean foundVal = false;
    private static boolean enabled = false;

    // --- Health Display ---
    private static final List<ValuableMobInfo> valuableMobList = new ArrayList<>();
    private static final List<String> TRACKED_NAMES = List.of(
        "thunder", "lord jawbus", "ragnarok", "minos inquisitor", "titanoboa",
        "wiki tiki"
    );    

    // -- Global name pattern ---
    private static final Pattern MOB_INFO_PATTERN = Pattern.compile(
        ".*\\[Lv(\\d{1,4})\\]\\s*[^\\w]*(.+?)\\s+\\d+(?:\\.\\d+)?(?:[KMB])?/\\d+(?:\\.\\d+)?(?:[KMB])?❤.*",
        Pattern.CASE_INSENSITIVE
    );

    // -- Inventory Tracking ---

    // Trigger inventory tracking
    private static final List<String> MOB_NAMES = List.of(
        "lord jawbus", "thunder", "minotaur", 
        "minos champion", "night squid"
    );

    private static final List<String> PLAYER_NAMES = List.of(
        "great white shark", "minos inquisitor", "grim reaper", 
        "minos champion", "ent", "water hydra"
    );    

    // Pattern to match full words for player entities
    private static final Pattern PLAYER_NAME_PATTERN = Pattern.compile(
        ".*\\b(" + String.join("|", PLAYER_NAMES) + ")\\b.*",
        Pattern.CASE_INSENSITIVE
    );    

    // Combined list of all relevant mob names for quick return
    private static final Set<String> ALL_VALUABLE_NAMES;
    
    static {
        ALL_VALUABLE_NAMES = java.util.stream.Stream.of(
            TRACKED_NAMES.stream(),
            PLAYER_NAMES.stream(),
            MOB_NAMES.stream()
        ).flatMap(s -> s).collect(java.util.stream.Collectors.toSet());
    }  

    private ValuableMobs() {}

    public static void refresh() {
        enabled = me.valkeea.fishyaddons.config.FishyConfig.getState(me.valkeea.fishyaddons.config.Key.HUD_HEALTH_ENABLED, false);
    }

    public static boolean displayOn() {
        return enabled;
    }
    
    public static class ValuableMobInfo {
        public final int id;
        public final String mobName;
        public final Text mobDisplayName;
        public final MobHealth health;
        public final boolean isPlayerEntity;

        public ValuableMobInfo(ArmorStandEntity armorStand, String mobName, String labelText, boolean isPlayerEntity) {
            this.id = armorStand.getId();
            this.mobName = mobName;
            this.health = extractHealth(labelText);
            this.isPlayerEntity = isPlayerEntity;
            this.mobDisplayName = setDisplayName(armorStand);
        }

        public int getId() { return id; }
        public Text getDisplayName() { return mobDisplayName; }
        public String getName() { return mobName; }
        public int getHealth() { return health.currentHealth; }
        public int getMaxHealth() { return health.maxHealth; }        

        private Text setDisplayName(ArmorStandEntity armorStand) {

            for (Text sibling : armorStand.getCustomName().getSiblings()) {
                String siblingStr = sibling.getString();
                if (siblingStr.contains(mobName)) {
                    return Text.literal(mobName).setStyle(sibling.getStyle());
                }
            }

            return armorStand.getCustomName();
        }
        
        public static MobHealth extractHealth(String labelText) {
            if (labelText == null || labelText.isEmpty()) return new MobHealth(-1, -1);

            String[] parts = labelText.replace(",", "").split(" ");
            for (String part : parts) {
                if (part.contains("/")) {
                    String[] healthParts = part.split("/");
                    if (healthParts.length == 2) {
                        try {
                            int currentHealth = parseHealthValue(healthParts[0]);
                            int maxHealth = parseHealthValue(healthParts[1].replace("❤", ""));
                            return new MobHealth(currentHealth, maxHealth);
                        } catch (NumberFormatException e) {
                            return new MobHealth(-1, -1);
                        }
                    }
                }
            }

            return new MobHealth(-1, -1);
        }
        
        private static int parseHealthValue(String healthStr) {
            if (healthStr == null || healthStr.isEmpty()) {
                throw new NumberFormatException("Empty health string");
            }
            
            healthStr = healthStr.trim();
            char lastChar = healthStr.charAt(healthStr.length() - 1);
            
            if (Character.isDigit(lastChar)) {
                return Integer.parseInt(healthStr);
            }
            
            String numberPart = healthStr.substring(0, healthStr.length() - 1);
            double baseValue = Double.parseDouble(numberPart);
            
            switch (Character.toUpperCase(lastChar)) {
                case 'K':
                    return (int) (baseValue * 1_000);
                case 'M':
                    return (int) (baseValue * 1_000_000);
                case 'B':
                    return (int) (baseValue * 1_000_000_000);
                default:
                    throw new NumberFormatException("Unknown magnitude suffix: " + lastChar);
            }
        }
        

        public static class MobHealth {
            public final int currentHealth;
            public final int maxHealth;

            public MobHealth(int currentHealth, int maxHealth) {
                this.currentHealth = currentHealth;
                this.maxHealth = maxHealth;
            }
        }

    }

    public static void onEntityAdded(net.minecraft.entity.Entity entity) {
        if (entity instanceof PlayerEntity player) {
            String displayName = NearbyEntities.extractDisplayName(player);

            if (isValuablePlayerEntity(displayName, player)) {
                foundVal = true;
                InventoryTracker.onValuableFound();
            }
        }
    }

    /**
     * Called from NearbyEntities tick scan after collecting all tracked or valuable entities
     */
    public static void update(java.util.List<ArmorStandEntity> valArmorStands) {
        if (valArmorStands.isEmpty()) {
            if (foundVal) {
                foundVal = false;
                InventoryTracker.onValuableGone();
                valuableMobList.clear();
            }
            return;
        }

        // Cleanup
        valuableMobList.removeIf(vmi -> {
            boolean stillExists = valArmorStands.stream()
                .anyMatch(armorStand -> armorStand.getId() == vmi.getId() && NearbyEntities.lookingAt(armorStand));

            return !stillExists;
        });

        if (!foundVal) {
            foundVal = true;
            InventoryTracker.onValuableFound();
        }
    }  

    /**
     * Checks if an armor stand represents a valuable mob to add to the passed list
     */
    public static boolean isValArmorstand(String name, ArmorStandEntity armorStand) {
        if (name == null || name.isEmpty()) return false;

        return isValuableMobLabel(name, armorStand);
    }    

    /**
     * Checks if a label represents a trackable mob
     */
    public static boolean isValuableMobLabel(String labelText, ArmorStandEntity armorStand) {
        if (labelText == null || labelText.isEmpty()) return false;
        
        if (isValuablePlayerEntity(labelText, armorStand)) {
            return true;
        }
        
        String lowerLabel = labelText.toLowerCase();
        boolean hasValuableName = false;
        
        for (String valuableName : ALL_VALUABLE_NAMES) {
            if (lowerLabel.contains(valuableName)) {
                hasValuableName = true;
                break;
            }
        }
        
        if (!hasValuableName) {
            return false;
        }

        var infoMatcher = MOB_INFO_PATTERN.matcher(labelText);
        if (infoMatcher.matches()) {
            return checkIfTracked(infoMatcher.group(2).trim(), labelText, armorStand);
        }
        
        return false;
    }

    private static boolean checkIfTracked(String mobName, String labelText, ArmorStandEntity armorStand) {
        var cleanedMobName = NearbyEntities.cutObfuscation(mobName);
        var lowerCleanedName = cleanedMobName.toLowerCase();

        // Health display
        boolean isTrackedMob = false;
        for (String trackedName : TRACKED_NAMES) {
            if (lowerCleanedName.contains(trackedName)) {
                isTrackedMob = true;
                break;
            }
        }

        if (isTrackedMob && NearbyEntities.lookingAt(armorStand)) {
            displayUpdate(labelText, armorStand, cleanedMobName, false);
        }            

        // Inventory tracking
        boolean triggersInventoryTracking = false;
        for (String valuableMob : MOB_NAMES) {
            if (lowerCleanedName.contains(valuableMob)) {
                triggersInventoryTracking = true;
                break;
            }
        }

        return isTrackedMob || triggersInventoryTracking;
    }

    private static void displayUpdate(String labelText, ArmorStandEntity armorStand, String mobName, boolean isPlayerEntity) {
        
        for (ValuableMobInfo vmi : valuableMobList) {
            if (vmi.getId() == armorStand.getId()) {
                valuableMobList.remove(vmi);
                valuableMobList.add(new ValuableMobInfo(armorStand, mobName, labelText, isPlayerEntity));
                return;
            }
        }
        valuableMobList.add(new ValuableMobInfo(armorStand, mobName, labelText, isPlayerEntity));
    }


    private static boolean isValuablePlayerEntity(String labelText, Entity armorStand) {
        String lowerLabel = labelText.toLowerCase();
        
        boolean isSimpleMatch = false;
        var playerNameMatcher = PLAYER_NAME_PATTERN.matcher(lowerLabel);

        if (playerNameMatcher.find()) {
            isSimpleMatch = true;
        }
        
        if (isSimpleMatch && armorStand instanceof ArmorStandEntity armorStandEntity) {
            displayUpdate(labelText, armorStandEntity, labelText.trim(), true);
        }
        return isSimpleMatch;
    }

    public static List<ValuableMobInfo> getValuableMobs() {
        return new ArrayList<>(valuableMobList);
    }

    public static boolean isMobAlive(String mobName) {
        for (ValuableMobInfo vmi : valuableMobList) {
            if (vmi.getName().equalsIgnoreCase(mobName)) {
                return vmi.getHealth() > 0;
            }
        }
        return false;
    }
}