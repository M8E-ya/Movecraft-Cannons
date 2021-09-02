package net.tylers1066.movecraftcannons;

import at.pavlov.cannons.API.CannonsAPI;
import at.pavlov.cannons.Cannons;
import at.pavlov.cannons.cannon.Cannon;
import at.pavlov.cannons.cannon.CannonDesign;
import at.pavlov.cannons.cannon.CannonManager;
import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.combat.movecraftcombat.MovecraftCombat;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.craft.CraftType;
import net.countercraft.movecraft.util.MathUtils;
import net.tylers1066.movecraftcannons.aiming.AimingCommand;
import net.countercraft.movecraft.util.hitboxes.HitBox;
import net.tylers1066.movecraftcannons.config.Config;
import net.tylers1066.movecraftcannons.listener.*;
import net.tylers1066.movecraftcannons.localisation.I18nSupport;
import org.bukkit.Location;
import org.bukkit.World;
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
            if (!new File(getDataFolder() + "/localisation/mc-cannonslang_" + s + ".properties").exists()) {
                this.saveResource("localisation/mc-cannonslang_" + s + ".properties", false);
            }
        }
        Config.Locale = getConfig().getString("Locale", "en");
        I18nSupport.init();

        Config.EnableCannonsTracking = getConfig().getBoolean("EnableCannonsTracking", true);


        // Load cannons plugin
        Plugin cannons = getServer().getPluginManager().getPlugin("Cannons");
        if (!(cannons instanceof Cannons)) {
            getLogger().log(Level.SEVERE, I18nSupport.getInternationalisedString("Cannons plugin not found"));
        }
        cannonsPlugin = (Cannons) cannons;
        getLogger().info(I18nSupport.getInternationalisedString("Cannons plugin found"));


        if (Config.EnableCannonsTracking) {
            // Load Movecraft-Combat plugin
            Plugin mcc = getServer().getPluginManager().getPlugin("Movecraft-Combat");
            if (mcc instanceof MovecraftCombat) {
                getLogger().info(I18nSupport.getInternationalisedString("Movecraft-Combat found"));
                getServer().getPluginManager().registerEvents(new ProjectileImpactListener(), this);
            } else {
                getLogger().info(I18nSupport.getInternationalisedString("Movecraft-Combat not found"));
            }
        }

        Set<CraftType> craftTypes = CraftManager.getInstance().getCraftTypes();

        // Load firepower limits for each craft:
        for (CraftType craftType : craftTypes) {
            String craftName = craftType.getCraftName();
            if (!getConfig().isConfigurationSection("FirepowerLimits")) {
                getLogger().log(Level.SEVERE, "Config is missing FirepowerLimits section!");
            }
            getLogger().log(Level.INFO, "Loaded firepower limits for " + craftName);
            Config.CraftFirepowerLimits.put(craftName, getConfig().getInt("FirepowerLimits." + craftName, 0));
        }

        // Load allowed cannons for each craft:
        for (CraftType craftType : craftTypes) {
            String craftName = craftType.getCraftName();
            if (!getConfig().isConfigurationSection("AllowedCannons")) {
                getLogger().log(Level.SEVERE, "Config is missing AllowedCannons section!");
            }
            getLogger().log(Level.INFO, "Loaded allowed cannons for " + craftName);
            Config.CraftAllowedCannons.put(craftName, getConfig().getStringList("AllowedCannons." + craftName));
        }

        // Assign firepower values to each cannon type:
        for (CannonDesign cannonDesign : cannonsPlugin.getDesignStorage().getCannonDesignList()) {
            String cannonName = cannonDesign.getDesignName();
            if (!getConfig().isConfigurationSection("CannonFirepower")) {
                getLogger().log(Level.SEVERE, "Config is missing CannonFirepower section!");
            }
            getLogger().log(Level.INFO, "Loaded firepower value for " + cannonName);
            Config.CannonFirepowerValues.put(cannonName, getConfig().getInt("CannonFirepower." + cannonName, 0));
        }

        getServer().getPluginManager().registerEvents(new DetectionListener(), this);
        getServer().getPluginManager().registerEvents(new TranslationListener(), this);
        getServer().getPluginManager().registerEvents(new RotationListener(), this);
        getServer().getPluginManager().registerEvents(new CannonListener(), this);
        this.getCommand("aim").setExecutor(new AimingCommand());
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    public HashSet<Cannon> getCannons(@NotNull HitBox hitbox, @NotNull World world, @Nullable UUID uuid) {
        List<Location> shipLocations = new ArrayList<>();
        for (MovecraftLocation loc : hitbox) {
            shipLocations.add(loc.toBukkit(world));
        }

        // Remove ghost cannons
        HashSet<Cannon> foundCannons = cannonsPlugin.getCannonsAPI().getCannons(shipLocations, uuid, true);
        foundCannons.removeIf(cannon -> cannon.getCannonDesign().getFiringTrigger(cannon).getBlock().getType() != cannon.getCannonDesign().getSchematicBlockTypeRightClickTrigger().getMaterial());

        // Remove duplicate cannons
        List<Location> cannonLocations = new ArrayList<>();
        Iterator<Cannon> iter = foundCannons.iterator();
        while (iter.hasNext()) {
            Cannon cannon = iter.next();
            Location firingTriggerLocation = cannon.getCannonDesign().getFiringTrigger(cannon).getBlock().getLocation();
            if (cannonLocations.contains(firingTriggerLocation)) {
                cannon.setValid(false);
                iter.remove();
                continue;
            }
            cannonLocations.add(firingTriggerLocation);
        }
        return foundCannons;
    }

    public Set<Cannon> getCannonsInHitBox(HitBox hitBox, World world) {
        Set<Cannon> foundCannons = new HashSet<>();
        for (Cannon can : CannonsAPI.getCannonsInBox(hitBox.getMidPoint().toBukkit(world), hitBox.getXLength(), hitBox.getYLength(), hitBox.getZLength())) {
            for (Location barrelLoc : can.getCannonDesign().getBarrelBlocks(can)) {
                if (!hitBox.contains(MathUtils.bukkit2MovecraftLoc(barrelLoc))) {
                    continue;
                }
                foundCannons.add(can);
                break;
            }
        }

        foundCannons.removeIf(cannon -> cannon.getCannonDesign().getFiringTrigger(cannon).getBlock().getType() != cannon.getCannonDesign().getSchematicBlockTypeRightClickTrigger().getMaterial());
        return foundCannons;
    }

    public Set<Cannon> getCannonsInCraftHitBox(Craft c) {
        return getCannonsInHitBox(c.getHitBox(), c.getWorld());
    }

}



