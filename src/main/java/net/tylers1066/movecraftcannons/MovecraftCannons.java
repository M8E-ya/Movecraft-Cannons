package net.tylers1066.movecraftcannons;

import at.pavlov.cannons.API.CannonsAPI;
import at.pavlov.cannons.Cannons;
import at.pavlov.cannons.Enum.MessageEnum;
import at.pavlov.cannons.cannon.Cannon;
import at.pavlov.cannons.cannon.CannonManager;
import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.combat.movecraftcombat.MovecraftCombat;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.craft.CraftType;
import net.countercraft.movecraft.utils.BitmapHitBox;
import net.countercraft.movecraft.utils.HitBox;
import net.countercraft.movecraft.utils.MathUtils;
import net.tylers1066.movecraftcannons.config.Config;
import net.tylers1066.movecraftcannons.listener.ProjectileImpactListener;
import net.tylers1066.movecraftcannons.listener.RotationListener;
import net.tylers1066.movecraftcannons.listener.TranslationListener;
import net.tylers1066.movecraftcannons.localisation.I18nSupport;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.*;
import java.util.logging.Level;


public final class MovecraftCannons extends JavaPlugin {
    private static MovecraftCannons instance;
    private static Cannons cannonsPlugin = null;
    private static CannonManager cannonManager = null;

    public static MovecraftCannons getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        Config.Debug = getConfig().getBoolean("Debug", false);

        // TODO other languages
        String[] languages = {"en"};
        for (String s : languages) {
            if (!new File(getDataFolder()  + "/localisation/mc-cannonslang_"+ s +".properties").exists()) {
                this.saveResource("localisation/mc-cannonslang_"+ s +".properties", false);
            }
        }
        Config.Locale = getConfig().getString("Locale", "en");
        I18nSupport.init();

        Config.EnableCannonsTracking = getConfig().getBoolean("EnableCannonsTracking", true);


        // Load cannons plugin
        Plugin cannons = getServer().getPluginManager().getPlugin("Cannons");
        if(!(cannons instanceof Cannons)) {
            getLogger().log(Level.SEVERE, I18nSupport.getInternationalisedString("Cannons plugin not found"));
        }
        cannonsPlugin = (Cannons) cannons;
        getLogger().info(I18nSupport.getInternationalisedString("Cannons plugin found"));


        if(Config.EnableCannonsTracking) {
            // Load Movecraft-Combat plugin
            Plugin mcc = getServer().getPluginManager().getPlugin("Movecraft-Combat");
            if (mcc instanceof MovecraftCombat) {
                getLogger().info(I18nSupport.getInternationalisedString("Movecraft-Combat found"));
                getServer().getPluginManager().registerEvents(new ProjectileImpactListener(), this);
            }
            else {
                getLogger().info(I18nSupport.getInternationalisedString("Movecraft-Combat not found"));
            }
        }

        for (CraftType craftType: CraftManager.getInstance().getCraftTypes()) {
            String craftName = craftType.getCraftName();
            if (!getConfig().isConfigurationSection("Limits." + craftName)) continue;
            Map<String, Object> configCraftLimits = getConfig().getConfigurationSection("Limits." + craftName).getValues(false);

            Map<String, Integer> craftLimits = new HashMap<>();
            for (final Map.Entry<String, Object> entry : configCraftLimits.entrySet()) {
                craftLimits.put(entry.getKey(), (Integer) entry.getValue());
            }
            Config.CraftCannonLimits.put(craftName, craftLimits);
        }

        getServer().getPluginManager().registerEvents(new TranslationListener(), this);
        getServer().getPluginManager().registerEvents(new RotationListener(), this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    public HashSet<Cannon> getCannons(@NotNull BitmapHitBox hitbox, @NotNull World world, @Nullable UUID uuid) {
        List<Location> shipLocations = new ArrayList<>();
        for(MovecraftLocation loc : hitbox) {
            shipLocations.add(loc.toBukkit(world));
        }
        return cannonsPlugin.getCannonsAPI().getCannons(shipLocations, uuid, true);
    }
}
