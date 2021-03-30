package net.tylers1066.movecraftcannons.listener;

import at.pavlov.cannons.cannon.Cannon;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.events.CraftDetectEvent;
import net.tylers1066.movecraftcannons.MovecraftCannons;
import net.tylers1066.movecraftcannons.config.Config;
import net.tylers1066.movecraftcannons.localisation.I18nSupport;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.Set;

public class DetectionListener implements Listener {

    @EventHandler
    public void onCraftDetect(CraftDetectEvent event) {
        Craft craft = event.getCraft();
        if (craft.getNotificationPlayer() == null) {
            return;
        }

        Set<Cannon> cannons = MovecraftCannons.getInstance().getCannons(craft.getHitBox(), craft.getWorld(), craft.getNotificationPlayer().getUniqueId());
        if (cannons.isEmpty()) return;

        String craftName = craft.getType().getCraftName();
        int craftFirepower = 0;
        int maximumFirepower = Config.CraftFirepowerLimits.get(craftName);

        for (Cannon cannon: cannons) {
            String cannonName = cannon.getCannonDesign().getDesignName();
            if (!Config.CraftAllowedCannons.get(craftName).contains(cannonName)) {
                event.setFailMessage(I18nSupport.getInternationalisedString("Disallowed cannon"));
                event.setCancelled(true);
                return;
            }

            craftFirepower = craftFirepower + Config.CannonFirepowerValues.get(cannonName);
            if (craftFirepower > maximumFirepower) {
                event.setFailMessage(String.format(I18nSupport.getInternationalisedString("Too much firepower"), maximumFirepower));
                event.setCancelled(true);
                return;
            }
        }

    }
}
