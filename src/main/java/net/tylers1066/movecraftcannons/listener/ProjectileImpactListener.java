package net.tylers1066.movecraftcannons.listener;

import at.pavlov.cannons.Cannons;
import at.pavlov.cannons.event.ProjectileImpactEvent;
import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.combat.features.tracking.DamageRecord;
import net.countercraft.movecraft.combat.features.tracking.events.CraftDamagedByEvent;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.craft.PlayerCraft;
import net.countercraft.movecraft.craft.SubCraftImpl;
import net.countercraft.movecraft.util.MathUtils;
import net.tylers1066.movecraftcannons.MovecraftCannons;
import net.tylers1066.movecraftcannons.config.Config;
import net.tylers1066.movecraftcannons.damagetype.ProjectileImpactDamage;
import net.tylers1066.movecraftcannons.utils.MovecraftUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.Set;
import java.util.UUID;

public class ProjectileImpactListener implements Listener {
    @EventHandler
    public void impactListener(ProjectileImpactEvent e) {
        if (!Config.EnableCannonsTracking)
            return;

        Craft craft = CraftManager.getInstance().fastNearestCraftToLoc(e.getImpactLocation());
        if (!(craft instanceof PlayerCraft))
            return;
        if (!MathUtils.locIsNearCraftFast(craft, MathUtils.bukkit2MovecraftLoc(e.getImpactLocation())))
            return;

        UUID shooter = e.getShooterUID();
        Player cause = MovecraftCannons.getInstance().getServer().getPlayer(shooter);
        if (cause == null || !cause.isOnline())
            return;

        PlayerCraft playerCraft = (PlayerCraft) craft;
        DamageRecord record = new DamageRecord(cause, playerCraft.getPilot(), new ProjectileImpactDamage());
        CraftDamagedByEvent event = new CraftDamagedByEvent(playerCraft, record);
        Bukkit.getPluginManager().callEvent(event);
    }

    @EventHandler(ignoreCancelled = true)
    public void antiFriendlyFireListener(ProjectileImpactEvent event) {
        Location impactLocation = event.getImpactLocation();

        Craft originCraft = null;
        Location cannonLocation = Cannons.getPlugin().getCannon(event.getFlyingProjectile().getCannonUID()).getLocation();
        if (cannonLocation == null) {
            return;
        }

        for (Craft craft: CraftManager.getInstance().getCraftsInWorld(event.getFlyingProjectile().getWorld())) {
            if (MathUtils.locationNearHitBox(craft.getHitBox(), cannonLocation, 1D)) {
                originCraft = craft;
                break;
            }
        }
        // Blast did not originate from a craft - we ignore
        if (originCraft == null) {
            return;
        }

        if (MathUtils.locationNearHitBox(originCraft.getHitBox(), impactLocation, 1.0)) {
            Set<Craft> craftsAtImpactLocation = MovecraftUtils.getPlayerCraftsAtLocation(impactLocation);
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
    }
}
