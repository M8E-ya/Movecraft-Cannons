package net.tylers1066.movecraftcannons.config;

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

}

