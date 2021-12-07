package net.tylers1066.movecraftcannons.aiming;

import at.pavlov.cannons.Cannons;
import at.pavlov.cannons.Enum.InteractAction;
import at.pavlov.cannons.cannon.Cannon;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.tylers1066.movecraftcannons.listener.DetectionListener;
import net.tylers1066.movecraftcannons.localisation.I18nSupport;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;

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
                Cannons.getPlugin().getFireCannon().redstoneFiring(cannon, InteractAction.fireAutoaim);
            }
            player.sendMessage(Component.text(I18nSupport.getInternationalisedString("Firing cannons"), TextColor.color(0xc3f09e)));
        }

        else if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            AimingUtils.aimCannonsOnCraft(craft, player, null);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        AimingUtils.getPlayerCannonSelections().remove(event.getPlayer().getUniqueId());
    }
}
