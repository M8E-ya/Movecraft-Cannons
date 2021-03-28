package net.tylers1066.movecraftcannons.listener;

import at.pavlov.cannons.cannon.Cannon;
import net.countercraft.movecraft.Rotation;
import net.countercraft.movecraft.events.CraftRotateEvent;
import net.tylers1066.movecraftcannons.MovecraftCannons;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.util.Vector;

import java.util.Set;


public class RotationListener implements Listener {
    @EventHandler
    public void rotateListener(CraftRotateEvent e) {
        if(e.getCraft().getNotificationPlayer() == null)
            return;

        Set<Cannon> cannons = MovecraftCannons.getInstance().getCannonsInHitBox(e.getCraft().getHitBox(), e.getCraft().getWorld());
        if (cannons.isEmpty()) return;

        Vector v = e.getOriginPoint().toBukkit(e.getCraft().getW()).toVector();
        for(Cannon c : cannons) {
            if(e.getRotation() == Rotation.CLOCKWISE) {
                c.rotateRight(v);
            }
            else if(e.getRotation() == Rotation.ANTICLOCKWISE) {
                c.rotateLeft(v);
            }
        }
    }
}
