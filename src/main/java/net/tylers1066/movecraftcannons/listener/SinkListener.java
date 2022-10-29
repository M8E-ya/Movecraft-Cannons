package net.tylers1066.movecraftcannons.listener;

import at.pavlov.cannons.Enum.BreakCause;
import at.pavlov.cannons.cannon.Cannon;
import net.countercraft.movecraft.events.CraftSinkEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class SinkListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCraftSink(CraftSinkEvent event) {
        var craftCannons = DetectionListener.getCannonsOnCraft(event.getCraft());
        for (Cannon cannon: craftCannons) {
            cannon.destroyCannon(false, false, BreakCause.ShipDestroyed);
        }
    }

}
