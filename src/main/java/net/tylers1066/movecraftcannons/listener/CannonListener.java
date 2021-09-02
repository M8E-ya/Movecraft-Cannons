package net.tylers1066.movecraftcannons.listener;

import at.pavlov.cannons.event.CannonAfterCreateEvent;
import net.countercraft.movecraft.MovecraftLocation;
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
        PlayerCraft craft = null;
        for (PlayerCraft pcraft: CraftManager.getInstance().getPlayerCraftsInWorld(world)) {
            if (MathUtils.locIsNearCraftFast(pcraft, cannonLoc)) {
                craft = pcraft;
                break;
            }
        }
        if (craft != null) {
            DetectionListener.cannonsOnCraft.computeIfAbsent(craft, k -> new HashSet<>()).add(event.getCannon());
        }
    }
}
