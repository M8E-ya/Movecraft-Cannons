package net.tylers1066.movecraftcannons.listener;

import at.pavlov.cannons.cannon.Cannon;
import net.countercraft.movecraft.Rotation;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.events.CraftRotateEvent;
import net.tylers1066.movecraftcannons.MovecraftCannons;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.util.Vector;

import java.util.Set;


public class RotationListener implements Listener {
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void rotateListener(CraftRotateEvent event) {
        Craft craft = event.getCraft();
        if (craft.getNotificationPlayer() == null)
            return;

        if (DetectionListener.cannonsOnCraft.get(craft) == null) {
            return;
        }

        Vector v = event.getOriginPoint().toBukkit(craft.getWorld()).toVector();
        for (Cannon c: DetectionListener.cannonsOnCraft.get(craft)) {
            if (event.getRotation() == Rotation.CLOCKWISE) {
                c.rotateRight(v);
            }
            else if (event.getRotation() == Rotation.ANTICLOCKWISE) {
                c.rotateLeft(v);
            }
        }
    }
}
