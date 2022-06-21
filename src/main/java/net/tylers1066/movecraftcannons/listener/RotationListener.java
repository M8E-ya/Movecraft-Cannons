package net.tylers1066.movecraftcannons.listener;

import at.pavlov.cannons.cannon.Cannon;
import net.countercraft.movecraft.MovecraftRotation;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.events.CraftPostRotateEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.util.Vector;


public class RotationListener implements Listener {
    @EventHandler
    public void rotateListener(CraftPostRotateEvent event) {
        Craft craft = event.getCraft();
        Vector origin = event.getOriginPoint().toBukkit(craft.getWorld()).toVector();
        rotateCannons(craft, origin, event.getRotation());
    }

    private void rotateCannons(Craft craft, Vector origin, MovecraftRotation rotation) {
        for (Cannon cannon: DetectionListener.getCannonsOnCraft(craft)) {
            rotateCannon(cannon, origin, rotation);
        }
    }

    private void rotateCannon(Cannon cannon, Vector origin, MovecraftRotation rotation) {
        if (rotation == MovecraftRotation.CLOCKWISE) {
            cannon.rotateRight(origin);
        } else if (rotation == MovecraftRotation.ANTICLOCKWISE) {
            cannon.rotateLeft(origin);
        }
    }
}
