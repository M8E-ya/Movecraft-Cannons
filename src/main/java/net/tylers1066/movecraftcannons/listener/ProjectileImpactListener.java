package net.tylers1066.movecraftcannons.listener;

import at.pavlov.cannons.event.ProjectileImpactEvent;
import net.countercraft.movecraft.combat.tracking.DamageManager;
import net.countercraft.movecraft.craft.*;
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
        if(!Config.EnableCannonsTracking)
            return;


        Craft craft = CraftManager.getInstance().fastNearestCraftToLoc(e.getImpactLocation());
        if(craft == null || !(craft instanceof PlayerCraft))
            return;

        if(!MathUtils.locIsNearCraftFast(craft, MathUtils.bukkit2MovecraftLoc(e.getImpactLocation())))
            return;

        UUID shooter = e.getShooterUID();
        Player cause = MovecraftCannons.getInstance().getServer().getPlayer(shooter);
        if(cause == null || !cause.isOnline())
            return;

        PlayerCraft playerCraft = (PlayerCraft) craft;
        DamageManager.getInstance().addDamageRecord(playerCraft, cause, new ProjectileImpactDamage());
    }

    @EventHandler(ignoreCancelled = true)
    public void antiFriendlyFireListener(ProjectileImpactEvent event) {
        Location impactLocation = event.getImpactLocation();
        Player cannoneer = Bukkit.getPlayer(event.getShooterUID());
        if (cannoneer == null) {
            return;
        }

        Craft craft = CraftManager.getInstance().getCraftByPlayer(cannoneer);
        if (craft == null) {
            // The cannon operator may be part of the crew, but not as a pilot:
            craft = MovecraftUtils.getCurrentShip(cannoneer);
            if (craft == null) {
                return;
            }
        }

        if (MathUtils.locationNearHitBox(craft.getHitBox(), impactLocation, 1.0)) {
            Set<Craft> craftsAtImpactLocation = MovecraftUtils.getPlayerCraftsAtLocation(impactLocation);
            craftsAtImpactLocation.remove(craft);
            if (craft instanceof SubCraftImpl subCraft) {
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
