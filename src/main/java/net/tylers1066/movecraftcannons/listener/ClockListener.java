package net.tylers1066.movecraftcannons.listener;

import at.pavlov.cannons.Cannons;
import me.halfquark.squadronsreloaded.squadron.Squadron;
import me.halfquark.squadronsreloaded.squadron.SquadronCraft;
import me.halfquark.squadronsreloaded.squadron.SquadronManager;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.craft.type.CraftType;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.tylers1066.movecraftcannons.aiming.AimingUtils;
import net.tylers1066.movecraftcannons.firing.FiringUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class ClockListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInteract(PlayerInteractEvent event) {
        ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.CLOCK) {
            return;
        }

        Player player = event.getPlayer();
        Craft craft = CraftManager.getInstance().getCraftByPlayer(player);
        if (craft == null) {
            return;
        }

        // Check if the clock specifies a cannon type to manage
        String selectedCannonType = null;
        if (item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            if (meta.hasDisplayName()) {
                String name = PlainTextComponentSerializer.plainText().serialize(meta.displayName());
                for (String designName: Cannons.getPlugin().getDesignStorage().getDesignIds()) {
                    if (name.equalsIgnoreCase(designName)) {
                        selectedCannonType = designName;
                        break;
                    }
                }
            }
        }

        // Left-click - fire
        if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK) {
            // Aerial crafts only fire on right-click
            if (craft.getType().getBoolProperty(CraftType.ALLOW_VERTICAL_MOVEMENT) && !craft.getType().getBoolProperty(CraftType.REQUIRE_WATER_CONTACT)) {
                return;
            }

            var cannons = DetectionListener.getCannonsOnCraft(craft);
            if (cannons.isEmpty()) {
                return;
            }

            // Filter non-selected cannons
            if (selectedCannonType != null) {
                String finalSelectedCannonType = selectedCannonType;
                cannons.removeIf(cannon -> !cannon.getCannonDesign().getDesignName().equals(finalSelectedCannonType));
            }

            FiringUtils.fireCannons(player, cannons, true);
        }

        // Right-click - aim
        else if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            AimingUtils.aimCannonsOnCraft(craft, player, selectedCannonType);

            // Aerial crafts will also fire, as well as aim
            if (craft.getType().getBoolProperty(CraftType.ALLOW_VERTICAL_MOVEMENT) && !craft.getType().getBoolProperty(CraftType.REQUIRE_WATER_CONTACT)) {
                var cannons = DetectionListener.getCannonsOnCraft(craft);
                FiringUtils.fireCannons(player, cannons, true);
            }

            if (SquadronManager.getInstance().hasSquadron(player)) {
                Squadron squad = SquadronManager.getInstance().getPlayerSquadron(player, true);
                if (squad == null)
                    return;
                for (SquadronCraft squadCraft: squad.getCrafts()) {
                    var squadCraftCannons = DetectionListener.getCannonsOnCraft(squadCraft);
                    FiringUtils.fireCannons(player, squadCraftCannons, true);
                }
            }
        }
    }


}
