package net.tylers1066.movecraftcannons.listener;

import at.pavlov.cannons.cannon.Cannon;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.events.CraftPostTranslateEvent;
import net.countercraft.movecraft.events.CraftPreTranslateEvent;
import net.tylers1066.movecraftcannons.MovecraftCannons;
import net.tylers1066.movecraftcannons.config.Config;
import net.tylers1066.movecraftcannons.localisation.I18nSupport;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Set;
import java.util.UUID;

public class TranslationListener implements Listener {
    public static HashMap<Craft, Set<Cannon>> cannonsOnCraft = new HashMap<>();

    /*
    This is a temporary workaround for CraftDetectEvent not being called in Movecraft 8.
    We discover cannons at the craft's pre-translation stage, rather than detection stage.
    It is critical for performance that the we minimize the number of cannon lookups. We therefore do not initiate
    the procedure if we already have the craft's cannons.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void preTranslateListener(CraftPreTranslateEvent event) {
        Craft craft = event.getCraft();
        if (cannonsOnCraft.containsKey(craft)) {
            return;
        }

        if (craft.getNotificationPlayer() == null) {
            return;
        }

        UUID pilotUUID = craft.getNotificationPlayer().getUniqueId();
        Set<Cannon> cannons = MovecraftCannons.getInstance().getCannons(craft.getHitBox(), craft.getWorld(), pilotUUID);
        if (cannons.isEmpty()) {
            return;
        }

        String craftName = craft.getType().getCraftName();
        int craftFirepower = 0;
        int maximumFirepower = Config.CraftFirepowerLimits.get(craftName);

        for (Cannon cannon: cannons) {
            String cannonName = cannon.getCannonDesign().getDesignName();
            if (!Config.CraftAllowedCannons.get(craftName).contains(cannonName)) {
                event.setFailMessage(String.format(I18nSupport.getInternationalisedString("Disallowed cannon"), cannonName));
                event.setCancelled(true);
                return;
            }
            craftFirepower = craftFirepower + Config.CannonFirepowerValues.get(cannonName);
        }

        if (craftFirepower > maximumFirepower) {
            event.setFailMessage(String.format(I18nSupport.getInternationalisedString("Too much firepower"), maximumFirepower, craftFirepower));
            event.setCancelled(true);
            return;
        }

        cannonsOnCraft.put(craft, cannons);
    }


    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void translateListener(CraftPostTranslateEvent event) {
        Craft craft = event.getCraft();
        if (craft.getAudience() == null) {
            return;
        }

        if (cannonsOnCraft.get(craft) == null) {
            return;
        }

        for (Cannon c: cannonsOnCraft.get(craft)) {
            c.move(new Vector(event.getDx(), event.getDy(), event.getDz()));

        }
    }
}
