package net.tylers1066.movecraftcannons.listener;

import at.pavlov.cannons.cannon.Cannon;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.events.CraftDetectEvent;
import net.countercraft.movecraft.events.CraftPilotEvent;
import net.countercraft.movecraft.events.CraftReleaseEvent;
import net.tylers1066.movecraftcannons.MovecraftCannons;
import net.tylers1066.movecraftcannons.config.Config;
import net.tylers1066.movecraftcannons.localisation.I18nSupport;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import javax.print.attribute.HashAttributeSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class DetectionListener implements Listener {

    public static HashMap<Craft, Set<Cannon>> cannonsOnCraft = new HashMap<>();

    /*
    CraftDetectEvent is currently not called in Movecraft 8.
    See TranslationListener for a workaround.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCraftDetect(CraftDetectEvent event) {
        Craft craft = event.getCraft();
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
        }

        cannonsOnCraft.put(craft, cannons);
    }

    @EventHandler
    public void onRelease(CraftReleaseEvent event) {
        cannonsOnCraft.remove(event.getCraft());
    }
}
