package net.tylers1066.movecraftcannons.listener;

import at.pavlov.cannons.cannon.Cannon;
import net.countercraft.movecraft.MovecraftRotation;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.events.CraftRotateEvent;
import net.tylers1066.movecraftcannons.config.Config;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.util.Vector;


public class RotationListener implements Listener {
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void rotateListener(CraftRotateEvent event) {
        Craft craft = event.getCraft();
        if (DetectionListener.cannonsOnCraft.get(craft) == null) {
            return;
        }

        Vector v = event.getOriginPoint().toBukkit(craft.getWorld()).toVector();
        for (Cannon c: DetectionListener.cannonsOnCraft.get(craft)) {
            if (event.getRotation() == MovecraftRotation.CLOCKWISE) {
                c.rotateRight(v);
            }
            else if (event.getRotation() == MovecraftRotation.ANTICLOCKWISE) {
                c.rotateLeft(v);
            }
        }
    }
}
