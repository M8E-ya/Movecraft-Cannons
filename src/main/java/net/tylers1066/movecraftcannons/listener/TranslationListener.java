package net.tylers1066.movecraftcannons.listener;

import at.pavlov.cannons.cannon.Cannon;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.events.CraftPostTranslateEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.util.Vector;

public class TranslationListener implements Listener {
    @EventHandler
    public void translateListener(CraftPostTranslateEvent event) {
        Craft craft = event.getCraft();
        if (DetectionListener.cannonsOnCraft.get(craft) == null) {
            return;
        }

        for (Cannon c: DetectionListener.cannonsOnCraft.get(craft)) {
            c.move(new Vector(event.getDx(), event.getDy(), event.getDz()));
        }
    }
}
