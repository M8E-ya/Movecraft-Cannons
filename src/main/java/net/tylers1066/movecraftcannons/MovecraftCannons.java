package net.tylers1066.movecraftcannons;

import at.pavlov.cannons.API.CannonsAPI;
import at.pavlov.cannons.Cannons;
import at.pavlov.cannons.Enum.BreakCause;
import at.pavlov.cannons.cannon.Cannon;
import at.pavlov.cannons.cannon.CannonDesign;
import at.pavlov.cannons.cannon.CannonManager;
import at.pavlov.cannons.projectile.Projectile;
import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.combat.MovecraftCombat;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.craft.type.CraftType;
import net.countercraft.movecraft.craft.type.TypeData;
import net.countercraft.movecraft.craft.type.property.BooleanProperty;
import net.countercraft.movecraft.craft.type.property.ObjectPropertyImpl;
import net.countercraft.movecraft.util.MathUtils;
import net.countercraft.movecraft.util.Tags;
import net.countercraft.movecraft.util.hitboxes.HitBox;
import net.tylers1066.movecraftcannons.aiming.AimingCommand;
import net.tylers1066.movecraftcannons.aiming.AimingListener;
import net.tylers1066.movecraftcannons.firing.FireCommand;
import net.tylers1066.movecraftcannons.config.Config;
import net.tylers1066.movecraftcannons.firing.FireSign;
import net.tylers1066.movecraftcannons.homingprojectiles.HomingProjectileManager;
import net.tylers1066.movecraftcannons.homingprojectiles.LockOnCommand;
import net.tylers1066.movecraftcannons.listener.*;
import net.tylers1066.movecraftcannons.localisation.I18nSupport;
import net.tylers1066.movecraftcannons.scoreboard.WeaponsScoreboard;
import net.tylers1066.movecraftcannons.type.MaxCannonsEntry;
import net.tylers1066.movecraftcannons.type.MaxCannonsProperty;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;


public final class MovecraftCannons extends JavaPlugin {
    private static MovecraftCannons instance;
    private static Cannons cannonsPlugin = null;
    private static CannonManager cannonManager = null;

    public static MovecraftCannons getInstance() {
        return instance;
    }

    // CCNet
    public static final NamespacedKey CAN_USE_CANNONS = new NamespacedKey("movecraft_cannons", "can_use_cannons");

    @Override
    public void onLoad() {
        MaxCannonsProperty.register();

        // CCNet
        CraftType.registerProperty(new BooleanProperty("canUseCannons", CAN_USE_CANNONS, type -> true));
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
            this.setEnabled(false);
            return;
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

            Set<CraftType> craftTypes = CraftManager.getInstance().getCraftTypes();

            // Load firepower limits for each craft:
            for (CraftType craftType : craftTypes) {
                String craftName = craftType.getStringProperty(CraftType.NAME);
                if (!getConfig().isConfigurationSection("FirepowerLimits")) {
                    getLogger().log(Level.SEVERE, "Config is missing FirepowerLimits section!");
                }
                getLogger().log(Level.INFO, "Loaded firepower limits for " + craftName);
                Config.CraftFirepowerLimits.put(craftName, getConfig().getInt("FirepowerLimits." + craftName, 0));
            }

            // Load allowed cannons for each craft:
            for (CraftType craftType : craftTypes) {
                String craftName = craftType.getStringProperty(CraftType.NAME);
                if (!getConfig().isConfigurationSection("AllowedCannons")) {
                    getLogger().log(Level.SEVERE, "Config is missing AllowedCannons section!");
                    break;
                }
                getLogger().log(Level.INFO, "Loaded allowed cannons for " + craftName);
                Config.CraftAllowedCannons.put(craftName, getConfig().getStringList("AllowedCannons." + craftName));
            }

            // Assign firepower values to each cannon type:
            for (CannonDesign cannonDesign : cannonsPlugin.getDesignStorage().getCannonDesignList()) {
                String cannonName = cannonDesign.getDesignName();
                if (!getConfig().isConfigurationSection("CannonFirepower")) {
                    getLogger().log(Level.SEVERE, "Config is missing CannonFirepower section!");
                    break;
                }
                getLogger().log(Level.INFO, "Loaded firepower value for " + cannonName);
                Config.CannonFirepowerValues.put(cannonName, getConfig().getDouble("CannonFirepower." + cannonName, 0));
            }

            // Load absolute max allowed cannons for each craft:
            for (CraftType craftType : craftTypes) {
                String craftName = craftType.getStringProperty(CraftType.NAME);
                if (!getConfig().isConfigurationSection("AbsoluteMaxAllowedCannons")) {
                    getLogger().log(Level.SEVERE, "Config is missing AbsoluteMaxAllowedCannons section!");
                    break;
                }
                var craftSection = getConfig().getConfigurationSection("AbsoluteMaxAllowedCannons").getConfigurationSection(craftName);
                if (craftSection == null) {
                    getLogger().info("Found no absolute max cannon limits for " + craftName);
                    continue;
                }
                Map<String, Object> maxCannonsMap = craftSection.getValues(true);
                for (Map.Entry<String, Object> maxCannonEntry: maxCannonsMap.entrySet()) {
                    Config.addMaxCannonEntry(craftName, maxCannonEntry.getKey(), (int) maxCannonEntry.getValue());
                }
                getLogger().info("Loaded absolute max allowed cannons for " + craftName);
            }

            // Load homing projectile types
            for (String projectileName: getConfig().getStringList("HomingProjectiles")) {
                Projectile projectile = Cannons.getPlugin().getProjectileStorage().getByName(projectileName);
                if (projectile == null) {
                    getLogger().severe(projectileName + " is not a valid Cannons projectile!");
                    continue;
                }
                Config.HomingProjectiles.add(projectileName);
            }

            Config.CountermeasureRange = getConfig().getDouble("CountermeasureRange");
            getLogger().info("Set countermeasure range to " + Config.CountermeasureRange);

            // Load countermeasure projectile types
            for (String projectileName: getConfig().getStringList("CountermeasureProjectiles")) {
                Projectile projectile = Cannons.getPlugin().getProjectileStorage().getByName(projectileName);
                if (projectile == null) {
                    getLogger().severe(projectileName + " is not a valid Cannons projectile!");
                    continue;
                }
                Config.CountermeasureProjectiles.add(projectileName);
                getLogger().info(projectileName + " registered as a countermeasure projectile");
            }

            getServer().getPluginManager().registerEvents(new DetectionListener(), this);
            getServer().getPluginManager().registerEvents(new TranslationListener(), this);
            getServer().getPluginManager().registerEvents(new RotationListener(), this);
            getServer().getPluginManager().registerEvents(new SinkListener(), this);
            getServer().getPluginManager().registerEvents(new CannonListener(), this);
            getServer().getPluginManager().registerEvents(new AimingListener(), this);
            getServer().getPluginManager().registerEvents(new ClockListener(), this);
            getServer().getPluginManager().registerEvents(new FireSign(), this);
            getServer().getPluginManager().registerEvents(new WeaponsScoreboard(), this);
            getServer().getPluginManager().registerEvents(new HomingProjectileManager(), this);

            ConfigurationSection materialDeflectionSection = getConfig().getConfigurationSection("MaterialDeflectionFactors");
            if (materialDeflectionSection == null) {
                getLogger().severe("Config is missing MaterialDeflectionFactors section!");
            }
            else {
                for (Map.Entry<String, Object> deflectionEntry : materialDeflectionSection.getValues(false).entrySet()) {
                    Set<Material> materials = Tags.parseMaterials(deflectionEntry.getKey());
                    if (materials.isEmpty()) {
                        getLogger().severe("Invalid material: " + deflectionEntry.getKey());
                        continue;
                    }
                    for (Material material : materials) {
                        ProjectileDeflection.getMaterialDeflectionMap().put(material, (double) deflectionEntry.getValue());
                    }
                }
            }

            ConfigurationSection materialCoRSection = getConfig().getConfigurationSection("MaterialCoefficientOfRestitution");
            if (materialCoRSection == null) {
                getLogger().severe("Config is missing MaterialCoefficientOfRestitution section!");
            }
            else {
                for (Map.Entry<String, Object> corEntry : materialCoRSection.getValues(false).entrySet()) {
                    Set<Material> materials = Tags.parseMaterials(corEntry.getKey());
                    if (materials.isEmpty()) {
                        getLogger().severe("Invalid material: " + corEntry.getKey());
                        continue;
                    }
                    for (Material material : materials) {
                        ProjectileDeflection.getMaterialCoRMap().put(material, (double) corEntry.getValue());
                    }
                }
            }

            ConfigurationSection projectileCraftDamageSection = getConfig().getConfigurationSection("ProjectileOnlyDamageCrafts");
            if (projectileCraftDamageSection == null) {
                getLogger().severe("Config is missing ProjectileOnlyDamageCrafts section!");
            }
            else {
                for (String projectileName : projectileCraftDamageSection.getKeys(false)) {
                    if (Cannons.getPlugin().getProjectileStorage().getByName(projectileName) == null) {
                        getLogger().severe(projectileName + " is not a valid projectile!");
                        continue;
                    }

                    List<String> craftNames = projectileCraftDamageSection.getStringList(projectileName);
                    for (String craftName : craftNames) {
                        if (CraftManager.getInstance().getCraftTypeFromString(craftName) == null) {
                            getLogger().severe(craftName + " is not a valid craft type!");
                            continue;
                        }
                        Config.ProjectilesOnlyDamageCrafts.put(projectileName, craftName);
                        getLogger().info(craftName + " added to the list of craft types that " + projectileName + " can damage");
                    }
                }
            }

            getServer().getPluginManager().registerEvents(new ProjectileDeflection(), this);

            this.getCommand("aim").setExecutor(new AimingCommand());
            this.getCommand("fire").setExecutor(new FireCommand());
            this.getCommand("lockon").setExecutor(new LockOnCommand());
        }
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    public LinkedHashSet<Cannon> getCannons(@NotNull HitBox hitbox, @NotNull World world, @Nullable UUID uuid) {
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
                iter.remove();
                cannon.destroyCannon(false, false, BreakCause.Other);
                continue;
            }
            cannonLocations.add(firingTriggerLocation);
        }
        return foundCannons.stream()
                .sorted(Comparator.comparing(Cannon::getDesignID)) // Sort alphabetically
                .collect(Collectors.toCollection(LinkedHashSet::new));
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

    public static Cannons getCannonsPlugin() {
        return cannonsPlugin;
    }
}



