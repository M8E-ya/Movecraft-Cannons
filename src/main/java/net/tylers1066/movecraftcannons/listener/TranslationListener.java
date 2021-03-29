package net.tylers1066.movecraftcannons.listener;

import at.pavlov.cannons.cannon.Cannon;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.events.CraftPreTranslateEvent;
import net.tylers1066.movecraftcannons.MovecraftCannons;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.util.Vector;

import java.util.Set;

public class TranslationListener implements Listener {
    @EventHandler
    public void translateListener(CraftPreTranslateEvent e) {
        Craft craft = e.getCraft();
        if (craft.getNotificationPlayer() == null)
            return;

        Set<Cannon> cannons = MovecraftCannons.getInstance().getCannonsInCraftHitBox(e.getCraft());

        for (Cannon c: cannons) {
            c.move(new Vector(e.getDx(), e.getDy(), e.getDz()));
        }

    }
}
