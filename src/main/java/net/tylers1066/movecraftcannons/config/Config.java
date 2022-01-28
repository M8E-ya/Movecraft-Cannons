package net.tylers1066.movecraftcannons.config;

import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.type.CraftType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Config {
    public static boolean Debug = false;

    // Localisation
    public static String Locale = "en";

    // Damage Tracking
    public static boolean EnableCannonsTracking = false;

    public static HashMap<String, Integer> CannonFirepowerValues = new HashMap<>();
    public static HashMap<String, Integer> CraftFirepowerLimits = new HashMap<>();
    public static HashMap<String, List<String>> CraftAllowedCannons = new HashMap<>();
    public static HashMap<String, List<MaxCannonEntry>> CraftMaxAllowedCannons = new HashMap<>();

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

