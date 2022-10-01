package net.tylers1066.movecraftcannons.config;

import at.pavlov.cannons.projectile.Projectile;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.type.CraftType;
import net.tylers1066.movecraftcannons.homingprojectiles.HomingProjectileType;
import org.openjdk.jmh.util.HashMultimap;
import org.openjdk.jmh.util.HashsetMultimap;

import java.util.*;

public class Config {
    public static boolean Debug = false;

    // Localisation
    public static String Locale = "en";

    // Damage Tracking
    public static boolean EnableCannonsTracking = false;

    // Cannons
    public static final Map<String, Double> CannonFirepowerValues = new HashMap<>();
    public static final Map<String, Integer> CraftFirepowerLimits = new HashMap<>();
    public static final Map<String, List<String>> CraftAllowedCannons = new HashMap<>();
    public static final Map<String, List<MaxCannonEntry>> CraftMaxAllowedCannons = new HashMap<>();

    // Projectiles
    public static final Map<String, HomingProjectileType> HomingProjectiles = new HashMap<>();
    public static double CountermeasureRange = 0;
    public static final Multimap<String, String> ProjectilesOnlyDamageCrafts = MultimapBuilder.hashKeys().hashSetValues().build();


    public static int getMaxAllowedCannonOnCraft(Craft craft, String cannonType) {
        String craftName = craft.getType().getStringProperty(CraftType.NAME);
        if (!CraftMaxAllowedCannons.containsKey(craftName)) {
            return -1;
        }
        var maxCannonEntries = CraftMaxAllowedCannons.get(craftName);
        for (MaxCannonEntry cannonEntry: maxCannonEntries) {
            if (cannonEntry.getCannonName().equals(cannonType)) {
                return cannonEntry.getMaxAmount();
            }
        }
        return -1;
    }

    public static void addMaxCannonEntry(String craftName, String cannonName, int maxCannonAmount) {
        var entry = new MaxCannonEntry(cannonName, maxCannonAmount);
        CraftMaxAllowedCannons.computeIfAbsent(craftName, k -> new ArrayList<>()).add(entry);
    }

}

record MaxCannonEntry(String cannonName, int maxAmount) {
    public String getCannonName() {
        return cannonName;
    }

    public int getMaxAmount() {
        return maxAmount;
    }
}

