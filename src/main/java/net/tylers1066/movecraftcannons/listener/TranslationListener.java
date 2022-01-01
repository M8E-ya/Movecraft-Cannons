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
        Vector vector = new Vector(event.getDx(), event.getDy(), event.getDz());
        moveCannons(craft, vector);
    }

    private void moveCannons(Craft craft, Vector vector) {
        for (Cannon cannon: DetectionListener.getCannonsOnCraft(craft)) {
            cannon.move(vector);
        }
    }
}
