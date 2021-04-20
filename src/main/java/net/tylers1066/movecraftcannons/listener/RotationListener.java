package net.tylers1066.movecraftcannons.listener;

import at.pavlov.cannons.cannon.Cannon;
import net.countercraft.movecraft.Rotation;
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
        if (event.getCraft().getNotificationPlayer() == null)
            return;

        Set<Cannon> cannons = MovecraftCannons.getInstance().getCannonsInCraftHitBox(event.getCraft());
        if (cannons.isEmpty()) return;

        Vector v = event.getOriginPoint().toBukkit(event.getCraft().getWorld()).toVector();
        for (Cannon c : cannons) {
            if (event.getRotation() == Rotation.CLOCKWISE) {
                c.rotateRight(v);
            }
            else if (event.getRotation() == Rotation.ANTICLOCKWISE) {
                c.rotateLeft(v);
            }
        }
    }
}
