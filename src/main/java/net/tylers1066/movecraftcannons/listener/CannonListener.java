package net.tylers1066.movecraftcannons.listener;

import at.pavlov.cannons.cannon.Cannon;
import at.pavlov.cannons.event.CannonAfterCreateEvent;
import at.pavlov.cannons.event.CannonDestroyedEvent;
import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.craft.PlayerCraft;
import net.countercraft.movecraft.util.MathUtils;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.HashSet;

public class CannonListener implements Listener {

    @EventHandler
    public void onCannonCreate(CannonAfterCreateEvent event) {
        World world = event.getCannon().getWorldBukkit();
        MovecraftLocation cannonLoc = MathUtils.bukkit2MovecraftLoc(event.getCannon().getLocation());
        Craft craft = null;
        for (Craft testCraft: CraftManager.getInstance().getCraftsInWorld(world)) {
            if (MathUtils.locIsNearCraftFast(testCraft, cannonLoc)) {
                craft = testCraft;
                break;
            }
        }
        if (craft != null) {
            DetectionListener.cannonsOnCraft.computeIfAbsent(craft, k -> new HashSet<>()).add(event.getCannon());
        }
    }

    @EventHandler
    public void onCannonDestroy(CannonDestroyedEvent event) {
        for (HashSet<Cannon> cannons: DetectionListener.cannonsOnCraft.values()) {
            cannons.remove(event.getCannon());
        }
    }
}
