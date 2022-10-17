package net.tylers1066.movecraftcannons.homingprojectiles;

import at.pavlov.cannons.Cannons;
import at.pavlov.cannons.event.ProjectileImpactEvent;
import at.pavlov.cannons.projectile.FlyingProjectile;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.craft.PlayerCraft;
import net.countercraft.movecraft.events.CraftReleaseEvent;
import net.countercraft.movecraft.events.CraftSinkEvent;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.tylers1066.movecraftcannons.MovecraftCannons;
import net.tylers1066.movecraftcannons.config.Config;
import net.tylers1066.movecraftcannons.utils.MovecraftUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public class HomingProjectileManager implements Listener {
    private static final WeakHashMap<FlyingProjectile, HomingProjectileTarget> homingProjectileTargetMap = new WeakHashMap<>();
    private static final HashMap<UUID, Craft> playerHomingTargetMap = new HashMap<>();
    private static final BossBar targetedBossBar = BossBar.bossBar(
            Component.text("Incoming missile!", NamedTextColor.RED, TextDecoration.BOLD),
            1f,
            BossBar.Color.RED,
            BossBar.Overlay.PROGRESS,
            EnumSet.of(BossBar.Flag.DARKEN_SCREEN)
    );

    public HomingProjectileManager(MovecraftCannons plugin) {

        // Play missile warning sounds to target-locked pilots
        new BukkitRunnable() {
            @Override
            public void run() {
                for (var entry : playerHomingTargetMap.entrySet()) {
                    Player player = Bukkit.getPlayer(entry.getKey());
                    if (player == null || CraftManager.getInstance().getCraftByPlayer(player) == null) {
                        continue;
                    }

                    Craft targetCraft = entry.getValue();
                    if (targetCraft instanceof PlayerCraft pcraft) {
                        player.sendActionBar(Component.text("Target lock: " + pcraft.getPilot().getName(), NamedTextColor.YELLOW));
                    }
                }

                for (FlyingProjectile flyingProjectile : Cannons.getPlugin().getProjectileManager().getFlyingProjectiles().values()) {
                    Craft targetCraft = playerHomingTargetMap.get(flyingProjectile.getShooterUID());

                    if (targetCraft instanceof PlayerCraft pcraft) {
                        pcraft.getPilot().playSound(Sound.sound(Key.key("missile_launch_warning"),
                                Sound.Source.MASTER, 1f, 1f), Sound.Emitter.self());

                        // Show warning bossbar
                        pcraft.getPilot().showBossBar(targetedBossBar);
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                pcraft.getPilot().hideBossBar(targetedBossBar);
                            }
                        }.runTaskLater(MovecraftCannons.getInstance(), 40L);
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 40L);

        new BukkitRunnable() {
            @Override
            public void run() {
                for (FlyingProjectile flyingProjectile: Cannons.getPlugin().getProjectileManager().getFlyingProjectiles().values()) {
                    if (!Config.HomingProjectiles.containsKey(flyingProjectile.getProjectile().getProjectileId()) || !flyingProjectile.isValid()) {
                        continue;
                    }
                    HomingProjectileType homingProjectileType = Config.HomingProjectiles.get(flyingProjectile.getProjectile().getProjectileId());

                    // Check if the projectile's shooter has a target.
                    if (playerHomingTargetMap.containsKey(flyingProjectile.getShooterUID())) {
                        Craft playerTargetCraft = playerHomingTargetMap.get(flyingProjectile.getShooterUID());
                        homingProjectileTargetMap.computeIfAbsent(flyingProjectile, k -> new HomingProjectileTarget(playerTargetCraft));
                    }

                    if (!homingProjectileTargetMap.containsKey(flyingProjectile)) {
                        continue;
                    }

                    // We know the projectile has a target, so direct it.
                    Location targetLocation = homingProjectileTargetMap.get(flyingProjectile).getTargetLocation();

                    // Direct the projectile towards a countermeasure projectile if there is one nearby
                    Projectile projectileEntity = flyingProjectile.getProjectileEntity();
                    for (FlyingProjectile otherProjectile : Cannons.getPlugin().getProjectileManager().getFlyingProjectiles().values()) {
                        if (otherProjectile != flyingProjectile
                                && homingProjectileType.getCountermeasureProjectiles().contains(otherProjectile.getProjectile().getProjectileId())
                                && !otherProjectile.getShooterUID().equals(flyingProjectile.getShooterUID())
                                && projectileEntity.getLocation().distanceSquared(otherProjectile.getProjectileEntity().getLocation()) <= Math.pow(Config.CountermeasureRange, 2)) {
                            targetLocation = otherProjectile.getProjectileEntity().getLocation();
                            break;
                        }
                    }

                    Location projectileLoc = projectileEntity.getLocation();
                    double speed = projectileEntity.getVelocity().length();
                    double distance = projectileLoc.distance(targetLocation);
                    double multiplicationFactor = homingProjectileType.getTurningFactor() / Math.sqrt(Math.max(distance, 1));

                    Vector newVector = projectileEntity.getVelocity()
                            .add(targetLocation.subtract(projectileLoc).toVector().normalize().multiply(multiplicationFactor))
                            .normalize()
                            .multiply(speed);

                    // Avoid entering water if the projectile is not set as an underwater projectile
                    if (!homingProjectileType.isWater() && flyingProjectile.isInWater()) {
                        newVector = newVector.setY(3);
                    }

                    // Avoid exiting water if the projectile is set as an underwater projectile
                    else if (homingProjectileType.isWater() && !flyingProjectile.isInWater()) {
                        newVector = newVector.setY(-1);
                    }

                    flyingProjectile.teleport(projectileEntity.getLocation(), newVector);
                    projectileEntity.setVelocity(newVector);
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onProjectileImpact(ProjectileImpactEvent event) {
        homingProjectileTargetMap.remove(event.getFlyingProjectile());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        playerHomingTargetMap.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCraftSink(CraftSinkEvent event) {
        processCraftDestruction(event.getCraft());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCraftRelease(CraftReleaseEvent event) {
        processCraftDestruction(event.getCraft());
    }

    private void processCraftDestruction(Craft craft) {
        Set<UUID> playersToRemove = new HashSet<>();
        for (Map.Entry<UUID, Craft> entry: playerHomingTargetMap.entrySet()) {
            if (entry.getValue() == craft) {
                playersToRemove.add(entry.getKey());
            }
        }
        playersToRemove.forEach(uuid -> {
            playerHomingTargetMap.remove(uuid);
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                player.sendRichMessage("<red>Your target-locked craft is now invalid!");
            }
        });
    }

    public static HashMap<UUID, Craft> getPlayerHomingTargetMap() {
        return playerHomingTargetMap;
    }
}
