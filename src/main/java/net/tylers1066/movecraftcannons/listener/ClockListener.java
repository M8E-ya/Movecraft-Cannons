package net.tylers1066.movecraftcannons.listener;

import at.pavlov.cannons.Cannons;
import at.pavlov.cannons.cannon.CannonDesign;
import io.papermc.paper.event.player.PlayerArmSwingEvent;
import me.halfquark.squadronsreloaded.squadron.Squadron;
import me.halfquark.squadronsreloaded.squadron.SquadronCraft;
import me.halfquark.squadronsreloaded.squadron.SquadronManager;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.craft.PlayerCraft;
import net.countercraft.movecraft.craft.type.CraftType;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.tylers1066.movecraftcannons.MovecraftCannons;
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
import org.jetbrains.annotations.Nullable;

public class ClockListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.CLOCK) {
            return;
        }

        Player player = event.getPlayer();
        PlayerCraft craft = CraftManager.getInstance().getCraftByPlayer(player);
        if (craft == null || !craft.getType().getBoolProperty(MovecraftCannons.CAN_USE_CANNONS)) {
            return;
        }

        // Check if the clock specifies a cannon type to manage
        String selectedCannonType = getSelectedCannonTypeFromItem(item);

        Squadron squad = SquadronManager.getInstance().getPlayerSquadron(player, true);
        // Handle squadron aiming and firing
        if (squad != null) {
            for (SquadronCraft squadCraft: squad.getCrafts()) {
                runAimingLogic(player, squadCraft, selectedCannonType);
            }
            return;
        }

        // e.g. to stop an aircraft carrier from aiming while its pilot is directing a squadron
        if (!SquadronManager.getInstance().getCarrierSquadrons(craft).isEmpty()) {
            return;
        }

        runAimingLogic(player, craft, selectedCannonType);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerArmSwing(PlayerArmSwingEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItem(event.getHand());
        if (item.getType() != Material.CLOCK) {
            return;
        }

        PlayerCraft craft = CraftManager.getInstance().getCraftByPlayer(player);
        if (craft == null || !craft.getType().getBoolProperty(MovecraftCannons.CAN_USE_CANNONS)) {
            return;
        }

        // Aerial crafts only fire on right-click
        if (craft.getType().getBoolProperty(CraftType.ALLOW_VERTICAL_MOVEMENT) && !craft.getType().getBoolProperty(CraftType.REQUIRE_WATER_CONTACT)) {
            return;
        }

        // e.g. to stop an aircraft carrier from firing while its pilot is directing a squadron
        if (!SquadronManager.getInstance().getCarrierSquadrons(craft).isEmpty()) {
            return;
        }

        var cannons = DetectionListener.getCannonsOnCraft(craft);
        if (cannons.isEmpty()) {
            return;
        }

        // Check if the clock specifies a cannon type to manage
        String selectedCannonType = getSelectedCannonTypeFromItem(item);

        // Filter non-selected cannons
        if (selectedCannonType != null) {
            cannons.removeIf(cannon -> !cannon.getCannonDesign().getMessageName().equals(selectedCannonType));
        }

        FiringUtils.fireCannons(player, cannons, true);
    }


    @Nullable
    private String getSelectedCannonTypeFromItem(ItemStack item) {
        String selectedCannonType = null;
        if (item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            if (meta.hasDisplayName()) {
                String name = PlainTextComponentSerializer.plainText().serialize(meta.displayName());
                for (CannonDesign design: Cannons.getPlugin().getDesignStorage().getCannonDesignList()) {
                    String messageName = design.getMessageName();
                    if (name.equalsIgnoreCase(messageName)) {
                        selectedCannonType = messageName;
                        break;
                    }
                }
            }
        }
        return selectedCannonType;
    }

    private void runAimingLogic(Player pilot, Craft craft, @Nullable String selectedCannonType) {
        AimingUtils.aimCannonsOnCraft(craft, pilot, selectedCannonType);

        // Aerial crafts will also fire, as well as aim
        var craftCannons = DetectionListener.getCannonsOnCraft(craft);
        if (craft.getType().getBoolProperty(CraftType.ALLOW_VERTICAL_MOVEMENT) && !craft.getType().getBoolProperty(CraftType.REQUIRE_WATER_CONTACT)) {
            FiringUtils.fireCannons(pilot, craftCannons, true);
        }
    }
}
