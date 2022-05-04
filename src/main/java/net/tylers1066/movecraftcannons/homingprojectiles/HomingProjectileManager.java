package net.tylers1066.movecraftcannons.homingprojectiles;

import at.pavlov.cannons.Cannons;
import at.pavlov.cannons.event.ProjectileImpactEvent;
import at.pavlov.cannons.projectile.FlyingProjectile;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.PlayerCraft;
import net.countercraft.movecraft.events.CraftReleaseEvent;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
    private static final WeakHashMap<FlyingProjectile, Craft> homingProjectileTargetMap = new WeakHashMap<>();
    private static final HashMap<UUID, Craft> playerHomingTargetMap = new HashMap<>();

    static {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (FlyingProjectile flyingProjectile: Cannons.getPlugin().getProjectileManager().getFlyingProjectiles().values()) {
                    if (!Config.HomingProjectiles.contains(flyingProjectile.getProjectile())) {
                        continue;
                    }

                    // Check if the projectile's shooter has a target.
                    if (playerHomingTargetMap.containsKey(flyingProjectile.getShooterUID())) {
                        Craft playerTargetCraft = playerHomingTargetMap.get(flyingProjectile.getShooterUID());
                        homingProjectileTargetMap.put(flyingProjectile, playerTargetCraft);
                        if (playerTargetCraft instanceof PlayerCraft pcraft) {
                            pcraft.getPilot().playSound(Sound.sound(Key.key("missile_launch_warning"),
                                    Sound.Source.MASTER, 1f, 1f), Sound.Emitter.self());
                        }
                    }

                    // If projectile has a target, direct it.
                    if (homingProjectileTargetMap.containsKey(flyingProjectile)) {
                        Craft targetCraft = homingProjectileTargetMap.get(flyingProjectile);
                        Location targetLocation = MovecraftUtils.getRandomBlockOnHitBox(targetCraft);

                        Projectile projectileEntity = flyingProjectile.getProjectileEntity();
                        for (FlyingProjectile otherProjectile : Cannons.getPlugin().getProjectileManager().getFlyingProjectiles().values()) {
                            if (otherProjectile != flyingProjectile
                                    && Config.CountermeasureProjectiles.contains(otherProjectile.getProjectile())
                                    && !otherProjectile.getShooterUID().equals(flyingProjectile.getShooterUID())
                                    && projectileEntity.getLocation().distanceSquared(otherProjectile.getProjectileEntity().getLocation()) <= Math.pow(Config.CountermeasureRange, 2)) {
                                targetLocation = otherProjectile.getProjectileEntity().getLocation();
                                break;
                            }
                        }

                        Location projectileLoc = projectileEntity.getLocation();

                        double speed = projectileEntity.getVelocity().length();
                        double distance = projectileLoc.distance(targetLocation);
                        if (distance <= 0.5) {
                            Cannons.getPlugin().getExplosion().detonate(flyingProjectile, flyingProjectile.getProjectileEntity(), BlockFace.UP);
                            homingProjectileTargetMap.remove(flyingProjectile);
                            flyingProjectile.getProjectileEntity().remove();
                            continue;
                        }

                        double multiplicationFactor = 1.25 / Math.sqrt(Math.max(distance, 1));

                        Vector newVector = projectileEntity.getVelocity()
                                .add(targetLocation.subtract(projectileLoc).toVector().normalize().multiply(multiplicationFactor))
                                .normalize()
                                .multiply(speed);

                        flyingProjectile.teleport(projectileEntity.getLocation(), newVector);
                        projectileEntity.setVelocity(newVector);
                    }
                }
            }
        }.runTaskTimer(MovecraftCannons.getInstance(), 1L, 1L);
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
    public void onCraftRelease(CraftReleaseEvent event) {
        Set<UUID> playersToRemove = new HashSet<>();
        for (Map.Entry<UUID, Craft> entry: playerHomingTargetMap.entrySet()) {
            if (entry.getValue() == event.getCraft()) {
                playersToRemove.add(entry.getKey());
            }
        }
        playersToRemove.forEach(uuid -> {
            playerHomingTargetMap.remove(uuid);
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                player.sendMessage(Component.text("Your target-locked craft has been released!", NamedTextColor.RED));
            }
        });
    }

    public static HashMap<UUID, Craft> getPlayerHomingTargetMap() {
        return playerHomingTargetMap;
    }
}