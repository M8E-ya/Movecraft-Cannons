package net.tylers1066.movecraftcannons.config;


import java.util.HashMap;
import java.util.Map;

public class Config {
    public static boolean Debug = false;

    // Localisation
    public static String Locale = "en";

    // Damage Tracking
    public static boolean EnableCannonsTracking = false;

    public static HashMap<String, Map<String, Integer>> CraftCannonLimits = new HashMap<>();
}

