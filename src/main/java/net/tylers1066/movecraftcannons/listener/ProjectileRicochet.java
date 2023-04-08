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

public class ProjectileRicochet implements Listener {

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

        BlockFace blockFaceHit = event.getHitBlockFace();
        if (blockFaceHit == null) {
            return;
        }

        Vector vectdeflect = flyingProjectile.getVelocity().clone();

        // Ignore slow cannonballs
        if (vectdeflect.length() < 0.6) {
            return;
        }

        Vector cannonballdirectionvector = flyingProjectile.getVelocity().clone().normalize();
        Vector centroidlocation = impactBlock.toVector().clone().add(blockFaceHit.getDirection().multiply(0.5));
        double CoR = materialCoRMap.getOrDefault(impactBlock.getBlock().getType(), 0.5D);

        // get the block matrix
        double[][][] blockmatrix = new double[5][5][5];
        for (int i = -2; i <= 2; i++) {
            for (int j = -2; j <= 2; j++) {
                for (int k = -2; k <= 2; k++) {
                    Location loc = impactBlock.clone().add(i, j, k);
                    Material mat = loc.getBlock().getType();
                    blockmatrix[i + 2][j + 2][k + 2] = (mat.isSolid()) ? 1 : 0;
                }
            }
        }

        // calculate eigenvectors and ricochet angle using the calculateRicochet method

        RealMatrix blockMatrix = new Array2DRowRealMatrix(blockmatrix);
        RealVector centroid = MatrixUtils.createRealVector(new double[] { centroidlocation.getX(), centroidlocation.getY(), centroidlocation.getZ() });
        RealMatrix covarianceMatrix = blockMatrix.subtract(centroid.outerProduct(centroid)).scalarMultiply(1.0 / 125.0);
        EigenDecomposition eig = new EigenDecomposition(covarianceMatrix);
        RealVector eigenvector = eig.getEigenvector(0);
        double ricochet = (180 - Math.toDegrees(Math.acos(cannonballdirectionvector.dotProduct(new Vector()))) / Math.PI);

        // check if ricochet angle is below 70 degrees
        if (ricochet >= 70) {
            return;
        }

        // Cancel the original impact
        event.setCancelled(true);

        // Spawn a new deflected cannonball with the modified velocity vector
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

                // modify the velocity vector based on the ricochet angle
                double angle = (180 - ricochet) * Math.PI;
                Vector rotatedVector = new Vector(vectdeflect.getX() * Math.cos(angle) - vectdeflect.getZ() * Math.sin(angle),
                        vectdeflect.getY(), vectdeflect.getX() * Math.sin(angle) + vectdeflect.getZ() * Math.cos(angle));

                MovecraftCannons.getCannonsPlugin().getProjectileManager().spawnProjectile(event.getProjectile(),
                        flyingProjectile.getShooterUID(), flyingProjectile.getSource(), flyingProjectile.getPlayerlocation(),
                        impactLoc.clone(), rotatedVector, flyingProjectile.getCannonUID(), ProjectileCause.DeflectedProjectile);
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

