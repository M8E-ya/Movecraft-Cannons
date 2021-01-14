package net.tylers1066.movecraftcannons.listener;

import at.pavlov.cannons.cannon.Cannon;
import net.countercraft.movecraft.events.CraftPreTranslateEvent;
import net.tylers1066.movecraftcannons.MovecraftCannons;
import net.tylers1066.movecraftcannons.config.Config;
import net.tylers1066.movecraftcannons.localisation.I18nSupport;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class TranslationListener implements Listener {
    @EventHandler
    public void translateListener(CraftPreTranslateEvent e) {
        if(e.getCraft().getNotificationPlayer() == null)
            return;

        HashSet<Cannon> cannons = MovecraftCannons.getInstance().getCannons(e.getCraft().getHitBox(), e.getCraft().getW(), e.getCraft().getNotificationPlayer().getUniqueId());
        if (cannons.isEmpty()) return;

        HashMap<String, Integer> cannonAmounts = new HashMap<>();

        for (Cannon cannon: cannons) {
            String cannonType = cannon.getCannonDesign().getDesignName();
            if (Config.CraftCannonLimits.get(e.getCraft().getType().getCraftName()).containsKey(cannonType)) {
                cannonAmounts.merge(cannonType, 1, Integer::sum);
            }
            else {
                e.setCancelled(true);
                e.getCraft().getNotificationPlayer().sendMessage(I18nSupport.getInternationalisedString("Too many cannons"));
            }
        }

        for (Map.Entry<String, Integer> entry: cannonAmounts.entrySet()) {
            if (entry.getValue() > Config.CraftCannonLimits.get(e.getCraft().getType().getCraftName()).get(entry.getKey())) {
                e.setCancelled(true);
                e.getCraft().getNotificationPlayer().sendMessage(I18nSupport.getInternationalisedString("Too many cannons"));
            }
        }

        for (Cannon c : cannons) {
            c.move(new Vector(e.getDx(), e.getDy(), e.getDz()));
        }
    }
}
