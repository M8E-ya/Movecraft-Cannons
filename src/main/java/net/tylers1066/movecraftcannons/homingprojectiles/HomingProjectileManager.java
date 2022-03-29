package net.tylers1066.movecraftcannons.homingprojectiles;

import at.pavlov.cannons.Cannons;
import at.pavlov.cannons.event.ProjectileImpactEvent;
import at.pavlov.cannons.projectile.FlyingProjectile;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.PlayerCraft;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.tylers1066.movecraftcannons.MovecraftCannons;
import net.tylers1066.movecraftcannons.config.Config;
import net.tylers1066.movecraftcannons.utils.MovecraftUtils;
import org.bukkit.Location;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.UUID;
import java.util.WeakHashMap;

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
                        Location targetLocation = MovecraftUtils.getCraftHitBoxMidPoint(targetCraft);

                        Projectile projectileEntity = flyingProjectile.getProjectileEntity();
                        for (FlyingProjectile otherProjectile : Cannons.getPlugin().getProjectileManager().getFlyingProjectiles().values()) {
                            if (otherProjectile != flyingProjectile
                                    && Config.CountermeasureProjectiles.contains(otherProjectile.getProjectile())
                                    && otherProjectile.distanceToProjectile(projectileEntity) <= Config.CountermeasureRange) {
                                targetLocation = otherProjectile.getProjectileEntity().getLocation();
                                break;
                            }
                        }

                        /*
                        Vector projectileDirectionVector = projectileEntity.getLocation().clone().getDirection();
                        Vector inBetweenVector = targetLocation.toVector()
                                .subtract(projectileDirectionVector)
                                .multiply(0.15);
                        Vector newVector = projectileDirectionVector.add(inBetweenVector).normalize();
                        */

                        Location floc = projectileEntity.getLocation();
                        Location ploc = targetLocation;
                        double speed = projectileEntity.getVelocity().length();
                        Vector newVector = projectileEntity.getVelocity().add(ploc.subtract(floc).toVector().normalize().multiply(0.18)).normalize().multiply(speed);

                        flyingProjectile.teleport(projectileEntity.getLocation(), newVector);
                        projectileEntity.setVelocity(newVector);
                        flyingProjectile.setTeleported(false);
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

    public static HashMap<UUID, Craft> getPlayerHomingTargetMap() {
        return playerHomingTargetMap;
    }
}
