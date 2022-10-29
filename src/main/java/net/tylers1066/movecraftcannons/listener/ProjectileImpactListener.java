package net.tylers1066.movecraftcannons.listener;

import at.pavlov.cannons.Cannons;
import at.pavlov.cannons.cannon.Cannon;
import at.pavlov.cannons.event.ProjectileImpactEvent;
import at.pavlov.cannons.projectile.Projectile;
import at.pavlov.cannons.projectile.ProjectileProperties;
import me.halfquark.squadronsreloaded.squadron.SquadronCraft;
import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.combat.features.tracking.DamageRecord;
import net.countercraft.movecraft.combat.features.tracking.events.CraftDamagedByEvent;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.craft.PlayerCraft;
import net.countercraft.movecraft.craft.SubCraftImpl;
import net.countercraft.movecraft.craft.type.CraftType;
import net.countercraft.movecraft.util.MathUtils;
import net.tylers1066.movecraftcannons.MovecraftCannons;
import net.tylers1066.movecraftcannons.config.Config;
import net.tylers1066.movecraftcannons.damagetype.ProjectileImpactDamage;
import net.tylers1066.movecraftcannons.utils.MovecraftUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;

public class ProjectileImpactListener implements Listener {
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void impactListener(ProjectileImpactEvent e) {
        if (!Config.EnableCannonsTracking)
            return;

        Craft craft = MathUtils.fastNearestCraftToLoc(CraftManager.getInstance().getCraftsInWorld(e.getImpactLocation().getWorld()), e.getImpactLocation());
        if (!(craft instanceof PlayerCraft playerCraft))
            return;
        if (!MathUtils.locIsNearCraftFast(craft, MathUtils.bukkit2MovecraftLoc(e.getImpactLocation())))
            return;

        UUID shooter = e.getShooterUID();
        Player cause = MovecraftCannons.getInstance().getServer().getPlayer(shooter);
        if (cause == null || !cause.isOnline())
            return;

        DamageRecord record = new DamageRecord(cause, playerCraft.getPilot(), new ProjectileImpactDamage());
        CraftDamagedByEvent event = new CraftDamagedByEvent(playerCraft, record);
        Bukkit.getPluginManager().callEvent(event);
    }

    @EventHandler(ignoreCancelled = true)
    public void antiFriendlyFireListener(ProjectileImpactEvent event) {
        // Allow observer projectiles to always work
        if (event.getProjectile().hasProperty(ProjectileProperties.OBSERVER)) {
            return;
        }

        Location impactLocation = event.getImpactLocation();

        Cannon cannon = Cannons.getPlugin().getCannon(event.getFlyingProjectile().getCannonUID());
        if (cannon == null) {
            return;
        }

        Location cannonLocation = cannon.getLocation();
        if (cannonLocation == null) {
            return;
        }

        Craft originCraft = MathUtils.fastNearestCraftToLoc(CraftManager.getInstance().getCraftsInWorld(cannonLocation.getWorld()), cannonLocation);
        // Blast did not originate from a craft - we ignore
        if (originCraft == null || !MathUtils.locationNearHitBox(originCraft.getHitBox(), cannonLocation, 1D)) {
            return;
        }

        if (MathUtils.locationNearHitBox(originCraft.getHitBox(), impactLocation, 1.0)) {
            Set<Craft> craftsAtImpactLocation = MovecraftUtils.getPlayerCraftsAtLocation(impactLocation, 1D);
            craftsAtImpactLocation.remove(originCraft);

            if (originCraft instanceof SubCraftImpl subCraft) {
                craftsAtImpactLocation.remove(subCraft.getParent());
            }

            // If there's some other craft at the location, don't cancel: the craft is in CQC
            if (!craftsAtImpactLocation.isEmpty()) {
                return;
            }

            event.setCancelled(true);
        }

        // Prevent squadron crafts from damaging each other
        Craft impactCraft = MathUtils.fastNearestCraftToLoc(CraftManager.getInstance().getCraftsInWorld(impactLocation.getWorld()), impactLocation);
        if (impactCraft == null || !MathUtils.locationNearHitBox(impactCraft.getHitBox(), impactLocation, 1D)) {
            return;
        }
        if (originCraft instanceof SquadronCraft originSquadronCraft) {
            if (impactCraft instanceof SquadronCraft impactSquadronCraft && impactSquadronCraft.getSquadron() == originSquadronCraft.getSquadron()) {
                event.setCancelled(true);
            }
            if (originSquadronCraft.getSquadron().getCarrier() == impactCraft) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void enforceProjectileCraftDamageRestriction(ProjectileImpactEvent event) {
        // Allow observer projectiles to always work
        if (event.getProjectile().hasProperty(ProjectileProperties.OBSERVER)) {
            return;
        }

        String projectileName = event.getProjectile().getProjectileId();
        Collection<String> allowedToDamageCraftTypes = Config.ProjectilesOnlyDamageCrafts.get(projectileName);
        if (allowedToDamageCraftTypes.isEmpty()) {
            return;
        }

        Set<Craft> craftsAtLocation = MovecraftUtils.getPlayerCraftsAtLocation(event.getImpactLocation(), 1D);
        for (Craft craft : craftsAtLocation) {
            String craftTypeName = craft.getType().getStringProperty(CraftType.NAME);
            if (!allowedToDamageCraftTypes.contains(craftTypeName)) {
                event.setCancelled(true);
                return;
            }
        }
    }
}
