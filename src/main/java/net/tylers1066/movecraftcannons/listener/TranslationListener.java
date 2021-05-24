package net.tylers1066.movecraftcannons.listener;

import at.pavlov.cannons.cannon.Cannon;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.events.CraftPreTranslateEvent;
import net.tylers1066.movecraftcannons.MovecraftCannons;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.util.Vector;

import java.util.Set;

public class TranslationListener implements Listener {
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void translateListener(CraftPreTranslateEvent event) {
        Craft craft = event.getCraft();
        if (craft.getNotificationPlayer() == null)
            return;

        Set<Cannon> cannons = MovecraftCannons.getInstance().getCannons(craft.getHitBox(), craft.getWorld(), craft.getNotificationPlayer().getUniqueId());
        for (Cannon c: cannons) {
            c.move(new Vector(event.getDx(), event.getDy(), event.getDz()));
        }

    }
}
