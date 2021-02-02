package net.tylers1066.movecraftcannons.listener;

import at.pavlov.cannons.cannon.Cannon;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.events.CraftPreTranslateEvent;
import net.tylers1066.movecraftcannons.MovecraftCannons;
import net.tylers1066.movecraftcannons.config.Config;
import net.tylers1066.movecraftcannons.localisation.I18nSupport;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.util.Vector;

import java.util.HashSet;

public class TranslationListener implements Listener {
    @EventHandler
    public void translateListener(CraftPreTranslateEvent event) {
        Craft craft = event.getCraft();
        if (craft.getNotificationPlayer() == null)
            return;

        HashSet<Cannon> cannons = MovecraftCannons.getInstance().getCannons(event.getCraft().getHitBox(), event.getCraft().getWorld(), event.getCraft().getNotificationPlayer().getUniqueId());
        if (cannons.isEmpty()) return;

        String craftName = craft.getType().getCraftName();

        int craftFirepower = 0;
        for (Cannon cannon: cannons) {
            String cannonName = cannon.getCannonDesign().getDesignName();

            if (!Config.CraftAllowedCannons.get(craftName).contains(cannonName)) {
                event.setCancelled(true);
                craft.getNotificationPlayer().sendMessage(String.format(I18nSupport.getInternationalisedString("Disallowed cannon"), cannonName));
                return;
            }

            craftFirepower = craftFirepower + Config.CannonFirepowerValues.get(cannonName);
        }

        if (craftFirepower > Config.CraftFirepowerLimits.get(craftName)) {
            event.setCancelled(true);
            craft.getNotificationPlayer().sendMessage(String.format(I18nSupport.getInternationalisedString("Too much firepower"), craftFirepower));
        }

        for (Cannon c: cannons) {
            c.move(new Vector(event.getDx(), event.getDy(), event.getDz()));
        }

    }
}
