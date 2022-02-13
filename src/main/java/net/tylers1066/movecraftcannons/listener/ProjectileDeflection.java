package net.tylers1066.movecraftcannons.listener;

import at.pavlov.cannons.Enum.ProjectileCause;
import at.pavlov.cannons.event.ProjectileImpactEvent;
import at.pavlov.cannons.projectile.FlyingProjectile;
import net.tylers1066.movecraftcannons.MovecraftCannons;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.EnumMap;

public class ProjectileDeflection implements Listener {

    // Higher number = more likely to deflect
    public static EnumMap<Material, Double> materialDeflectionMap = new EnumMap<>(Material.class);
    // Higher number = higher velocity deflection (0-1)
    public static EnumMap<Material, Double> materialCoRMap = new EnumMap<>(Material.class);

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onProjectileImpact(ProjectileImpactEvent event) {
        FlyingProjectile flyingProjectile = event.getFlyingProjectile();
        Location impactBlock = flyingProjectile.getImpactBlock();
        if (impactBlock == null) {
            return;
        }

        // Only apply ricochet mechanics to piloted crafts
        /*
        if (MovecraftUtils.getPlayerCraftsAtLocation(impactLoc).isEmpty()) {
            return;
        }
         */

        double deflectionVelocityFactor = 0.3;
        BlockFace blockFaceHit = event.getHitBlockFace();
        if (blockFaceHit == null) {
            return;
        }

        Vector vectdeflect = flyingProjectile.getVelocity().clone();

        // Ignore slow cannonballs
        if (vectdeflect.length() < 0.6) {
            return;
        }

        double blockDeflectionFactor = materialDeflectionMap.getOrDefault(impactBlock.getBlock().getType(), 0D);
        double projDeflectionFactor = event.getProjectile().getDeflectionFactor();
        double CoR = materialCoRMap.getOrDefault(impactBlock.getBlock().getType(), 0.5D);

        double angleOfImpact = Math.toDegrees(flyingProjectile.getVelocity().angle(blockFaceHit.getDirection()));
        double deflectionChance = ((angleOfImpact + 0.01) / 90) * (deflectionVelocityFactor / flyingProjectile.getVelocity().length()) * blockDeflectionFactor * projDeflectionFactor;

        if (Math.random() > deflectionChance) {
            return;
        }

        // Cancel the original impact
        event.setCancelled(true);

        // Spawn a new deflected cannonball
        new BukkitRunnable() {
            @Override
            public void run() {
                Vector vectdeflect = flyingProjectile.getVelocity().multiply(CoR * ((3 + Math.random()) / 4)); // Bounciness + some randomness (75% - 100%)
                Location impactLoc = flyingProjectile.getImpactLocation().subtract(flyingProjectile.getVelocity().normalize().multiply(0.3));
                impactLoc.getWorld().spawnParticle(Particle.SCRAPE, impactLoc, 10, 1, 0, 1, 0, null, true);

                switch (blockFaceHit) {
                    case UP, DOWN -> vectdeflect.setY(-vectdeflect.getY());
                    case WEST, EAST -> vectdeflect.setX(-vectdeflect.getX());
                    case NORTH, SOUTH -> vectdeflect.setZ(-vectdeflect.getZ());
                }

                MovecraftCannons.getCannonsPlugin().getProjectileManager().spawnProjectile(event.getProjectile(),
                        flyingProjectile.getShooterUID(), flyingProjectile.getSource(), flyingProjectile.getPlayerlocation(),
                        impactLoc.clone(), vectdeflect, flyingProjectile.getCannonUID(), ProjectileCause.DeflectedProjectile);
            }
        }.runTask(MovecraftCannons.getInstance());
    }

    public static EnumMap<Material, Double> getMaterialDeflectionMap() {
        return materialDeflectionMap;
    }

    public static EnumMap<Material, Double> getMaterialCoRMap() {
        return materialCoRMap;
    }
}
