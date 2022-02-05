package net.tylers1066.movecraftcannons.listener;

import at.pavlov.cannons.cannon.Cannon;
import at.pavlov.cannons.event.CannonAfterCreateEvent;
import at.pavlov.cannons.event.CannonBeforeCreateEvent;
import at.pavlov.cannons.event.CannonDestroyedEvent;
import at.pavlov.cannons.event.CannonFireEvent;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.type.CraftType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.tylers1066.movecraftcannons.config.Config;
import net.tylers1066.movecraftcannons.utils.MovecraftUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import javax.print.DocFlavor;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class CannonListener implements Listener {
    private final static BlockFace[] ADJACENT = { BlockFace.DOWN, BlockFace.UP, BlockFace.EAST, BlockFace.WEST, BlockFace.NORTH, BlockFace.SOUTH };


    @EventHandler
    public void beforeCannonCreate(CannonBeforeCreateEvent event) {
        Cannon cannon = event.getCannon();
        Craft craft = MovecraftUtils.getParentCraftFromLocation(cannon.getLocation());
        if (craft == null) {
            return;
        }
        if (!Config.CraftAllowedCannons.get(craft.getType().getStringProperty(CraftType.NAME)).contains(cannon.getCannonDesign().getDesignName())) {
            sendMessageToCannonOperator(cannon, event.getPlayer(), Component.text("This cannon cannot be created on this craft.", NamedTextColor.RED));
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onCannonCreate(CannonAfterCreateEvent event) {
        Cannon cannon = event.getCannon();
        Craft craft = MovecraftUtils.getParentCraftFromLocation(cannon.getLocation());
        if (craft == null) {
            return;
        }
        if (!Config.CraftAllowedCannons.get(craft.getType().getStringProperty(CraftType.NAME)).contains(cannon.getCannonDesign().getDesignName())) {
            return;
        }
        DetectionListener.cannonsOnCraft.computeIfAbsent(craft, k -> new HashSet<>()).add(cannon);
    }

    @EventHandler
    public void onCannonDestroy(CannonDestroyedEvent event) {
        for (Set<Cannon> cannons: DetectionListener.cannonsOnCraft.values()) {
            cannons.remove(event.getCannon());
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onCannonFire(CannonFireEvent event) {
        Cannon cannon = event.getCannon();
        Craft craft = MovecraftUtils.getParentCraftFromLocation(cannon.getLocation());
        if (craft == null) {
            return;
        }
        if (!Config.CraftAllowedCannons.get(craft.getType().getStringProperty(CraftType.NAME)).contains(cannon.getCannonDesign().getDesignName())) {
            sendMessageToCannonOperator(cannon, event.getPlayer(), Component.text("This cannon cannot be fired from this craft.", NamedTextColor.RED));
            event.setCancelled(true);
        }

        // Force cannon barrel to be uncovered
        List<Location> cannonLocations = cannon.getCannonDesign().getAllCannonBlocks(cannon);
        Block muzzle = cannon.getMuzzle().getBlock();

        for (BlockFace face: ADJACENT) {
            if (!event.isCancelled()) {
                Block adjacentBlock = muzzle.getRelative(face);
                checkIfBlockCoveredAndNext(event, adjacentBlock, face, cannonLocations);
            }
        }
    }

    private void checkIfBlockCoveredAndNext(CannonFireEvent event, Block block, BlockFace originatingFace, List<Location> cannonLocations) {
        if (!cannonLocations.contains(block.getLocation()) || block.getType() != Material.STONE_BRICK_WALL) {
            return;
        }

        if (isBlockCovered(block, originatingFace.getOppositeFace())) {
            sendMessageToCannonOperator(event.getCannon(), event.getPlayer(),
                    Component.text("One of your cannon's barrel blocks is covered. Make some space for it!", NamedTextColor.RED));
            event.setCancelled(true);
            return;
        }

        for (BlockFace face: ADJACENT) {
            if (face == originatingFace.getOppositeFace()) {
                continue;
            }
            checkIfBlockCoveredAndNext(event, block.getRelative(face), face, cannonLocations);
        }
    }

    private boolean isBlockCovered(Block block, BlockFace originatingFace) {
        int covered = 0;
        for (BlockFace face : ADJACENT) {
            if (face == originatingFace) {
                continue;
            }
            Block adjacentBlock = block.getRelative(face);
            if (!adjacentBlock.getType().isAir()) {
                covered++;
            }
            if (covered > 1) {
                return true;
            }
        }
        return false;
    }

    private void sendMessageToCannonOperator(Cannon cannon, UUID operator, Component message) {
        Player player = Bukkit.getPlayer(operator);
        if (player != null && cannon.getLocation().distanceSquared(player.getLocation()) < 900) { // 30 blocks
            player.sendMessage(message);
        }
    }

}
