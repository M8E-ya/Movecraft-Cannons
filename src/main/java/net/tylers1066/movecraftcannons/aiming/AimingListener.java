package net.tylers1066.movecraftcannons.aiming;

import at.pavlov.cannons.Cannons;
import at.pavlov.cannons.Enum.InteractAction;
import at.pavlov.cannons.cannon.Cannon;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.tylers1066.movecraftcannons.listener.DetectionListener;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public class AimingListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getItem() == null || event.getItem().getType() != Material.CLOCK) {
            return;
        }

        Player player = event.getPlayer();
        Craft craft = CraftManager.getInstance().getCraftByPlayer(player);
        if (craft == null) {
            return;
        }

        if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK) {
            for (Cannon cannon: DetectionListener.cannonsOnCraft.get(craft)) {
                Cannons.getPlugin().getFireCannon().playerFiring(cannon, player, InteractAction.fireAutoaim);
            }
            player.sendMessage(Component.text("Attempting to fire already-loaded cannons!", TextColor.color(0xc3f09e)));
        }

        else if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            AimingUtils.aimCannonsOnCraft(craft, player);
        }
    }
}
