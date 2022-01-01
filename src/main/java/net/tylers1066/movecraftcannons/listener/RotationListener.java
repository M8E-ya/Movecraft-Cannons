package net.tylers1066.movecraftcannons.listener;

import at.pavlov.cannons.cannon.Cannon;
import net.countercraft.movecraft.MovecraftRotation;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.SubCraftImpl;
import net.countercraft.movecraft.events.CraftRotateEvent;
import net.countercraft.movecraft.util.MathUtils;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.util.Vector;


public class RotationListener implements Listener {
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void rotateListener(CraftRotateEvent event) {
        Craft craft = event.getCraft();
        Vector origin = event.getOriginPoint().toBukkit(craft.getWorld()).toVector();
        MovecraftRotation rotation = event.getRotation();

        // If a SubcraftRotate craft, we need to rotate the cannon for its parents.
        if (craft instanceof SubCraftImpl subcraft) {
            for (Cannon cannon: DetectionListener.getCannonsOnCraft(subcraft.getParent())) {
                // We only want to move the cannons that are on the subcraft!
                if (MathUtils.locIsNearCraftFast(subcraft, MathUtils.bukkit2MovecraftLoc(cannon.getMuzzle()))) {
                    rotateCannon(cannon, origin, rotation);
                }
            }
        }
        else {
            rotateCannons(craft, origin, event.getRotation());
        }
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
