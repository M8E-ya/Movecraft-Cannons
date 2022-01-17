package net.tylers1066.movecraftcannons.aiming;

import at.pavlov.cannons.Cannons;
import at.pavlov.cannons.Enum.InteractAction;
import at.pavlov.cannons.cannon.Cannon;
import com.palmergames.bukkit.towny.TownyAPI;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.craft.PlayerCraft;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.tylers1066.movecraftcannons.listener.DetectionListener;
import net.tylers1066.movecraftcannons.localisation.I18nSupport;
import net.tylers1066.movecraftcannons.utils.MovecraftUtils;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
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
            var cannons = DetectionListener.cannonsOnCraft.get(craft);
            if (cannons == null) {
                return;
            }
            for (Cannon cannon: cannons) {
                Cannons.getPlugin().getFireCannon().redstoneFiring(cannon, InteractAction.fireAutoaim);
            }
            player.sendActionBar(Component.text(I18nSupport.getInternationalisedString("Firing cannons"), TextColor.color(0xc3f09e)));
        }

        else if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            AimingUtils.aimCannonsOnCraft(craft, player, null);
        }
    }

    @EventHandler
    public void onRightClick(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        Block block = event.getClickedBlock();
        if (!Tag.SIGNS.isTagged(block.getType())) {
            return;
        }
        BlockState state = block.getState(false);
        if (!(state instanceof Sign sign)) {
            return;
        }

        if (!PlainTextComponentSerializer.plainText().serialize(sign.lines().get(0)).equalsIgnoreCase("Aiming Director")) {
            return;
        }

        Craft craft = MovecraftUtils.getCurrentShip(player);
        if (!(craft instanceof PlayerCraft pcraft) || !(MovecraftUtils.isFriendly(TownyAPI.getInstance().getResident(player), pcraft))) {
            player.sendMessage(Component.text(I18nSupport.getInternationalisedString("Unfriendly craft"), TextColor.color(0xffb2ab)));
            return;
        }

        String cannonType = PlainTextComponentSerializer.plainText().serialize(sign.lines().get(1));
        if (Cannons.getPlugin().getDesignStorage().getDesignIds().contains(cannonType)) {
            AimingUtils.getPlayerCannonSelections().put(player.getUniqueId(), cannonType);
            player.sendMessage(Component.text(String.format(I18nSupport.getInternationalisedString("Selected cannon type"), cannonType), TextColor.color(0xc3f09e)));
        }
        else if (cannonType.equalsIgnoreCase("all")) {
            AimingUtils.getPlayerCannonSelections().remove(player.getUniqueId());
            player.sendMessage(Component.text(I18nSupport.getInternationalisedString("Deselected cannon type"), TextColor.color(0xc3f09e)));
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        AimingUtils.getPlayerCannonSelections().remove(event.getPlayer().getUniqueId());
    }
}
