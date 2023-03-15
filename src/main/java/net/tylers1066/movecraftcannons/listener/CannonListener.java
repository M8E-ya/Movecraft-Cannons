package net.tylers1066.movecraftcannons.listener;

import at.pavlov.cannons.API.CannonsAPI;
import at.pavlov.cannons.Cannons;
import at.pavlov.cannons.cannon.Cannon;
import at.pavlov.cannons.cannon.CannonDesign;
import at.pavlov.cannons.cannon.CannonManager;
import at.pavlov.cannons.event.CannonAfterCreateEvent;
import at.pavlov.cannons.event.CannonBeforeCreateEvent;
import at.pavlov.cannons.event.CannonDestroyedEvent;
import at.pavlov.cannons.event.CannonFireEvent;
import com.google.common.base.Enums;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.craft.PlayerCraft;
import net.countercraft.movecraft.craft.type.CraftType;
import net.countercraft.movecraft.util.BlockHighlight;
import net.countercraft.movecraft.util.MathUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.tylers1066.movecraftcannons.MovecraftCannons;
import net.tylers1066.movecraftcannons.config.Config;
import net.tylers1066.movecraftcannons.utils.MovecraftUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.stream.Collectors;

public class CannonListener implements Listener {
    private final Set<BlockFace> HORIZONTAL_BLOCKFACES_NS = EnumSet.of(BlockFace.EAST, BlockFace.WEST);

    private final Set<BlockFace> HORIZONTAL_BLOCKFACES_EW = EnumSet.of(BlockFace.NORTH, BlockFace.SOUTH);
    private final BlockFace[] ADJACENT_BLOCKFACES = { BlockFace.UP, BlockFace.EAST, BlockFace.WEST, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.DOWN };
    private final Set<Material> BARREL_MATERIALS = EnumSet.of(Material.STONE_BRICK_WALL, Material.CHAIN);

    private final Set<Material> ALL_BARREL_MATERIALS = EnumSet.noneOf(Material.class);

    public CannonListener() {
        for (CannonDesign design: Cannons.getPlugin().getDesignStorage().getCannonDesignList()) {
            ALL_BARREL_MATERIALS.addAll(design.getSchematicBlockTypeProtected().stream().map(BlockData::getMaterial).toList());
        }
    }


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
        DetectionListener.cannonsOnCraft.computeIfAbsent(craft, k -> new LinkedHashSet<>()).add(cannon);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
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
        if (!Config.CraftAllowedCannons.get(craft.getType().getStringProperty(CraftType.NAME)).contains(cannon.getCannonDesign().getDesignName())
            || !craft.getType().getBoolProperty(MovecraftCannons.CAN_USE_CANNONS)) {
            sendMessageToCannonOperator(cannon, event.getPlayer(), Component.text("This cannon cannot be fired from this craft.", NamedTextColor.RED));
            event.setCancelled(true);
        }

        // Force cannon barrel to be uncovered
        List<Location> cannonLocations = cannon.getCannonDesign().getAllCannonBlocks(cannon);
        Block muzzle = cannon.getMuzzle().getBlock();

        for (BlockFace face: ADJACENT_BLOCKFACES) {
            if (!event.isCancelled()) {
                Block adjacentBlock = muzzle.getRelative(face);
                checkIfBlockCoveredAndNext(event, adjacentBlock, face, cannonLocations, event.getPlayer());
            }
        }
    }

    private void checkIfBlockCoveredAndNext(CannonFireEvent event, Block block, BlockFace originatingFace,
                                            List<Location> cannonLocations, UUID player) {
        if (!cannonLocations.contains(block.getLocation()) || !BARREL_MATERIALS.contains(block.getType())) {
            return;
        }

        if (isBlockCovered(block, originatingFace.getOppositeFace(), player)) {
            sendMessageToCannonOperator(event.getCannon(), event.getPlayer(),
                    Component.text("One of your cannon's barrel blocks is covered. Make some space for it!", NamedTextColor.RED));
            event.setCancelled(true);
            return;
        }

        for (BlockFace face: ADJACENT_BLOCKFACES) {
            if (face == originatingFace.getOppositeFace()) {
                continue;
            }
            checkIfBlockCoveredAndNext(event, block.getRelative(face), face, cannonLocations, player);
        }
    }

    private boolean isBlockCovered(Block block, BlockFace originatingFace, UUID player) {
        Set<BlockFace> horizontalBlockFaces;
        if (originatingFace == BlockFace.NORTH || originatingFace == BlockFace.SOUTH) {
            horizontalBlockFaces = HORIZONTAL_BLOCKFACES_NS;
        } else if (originatingFace == BlockFace.EAST || originatingFace == BlockFace.WEST) {
            horizontalBlockFaces = HORIZONTAL_BLOCKFACES_EW;
        } else {
            horizontalBlockFaces = Collections.emptySet();
        }

        List<Block> coveringBlocks = new ArrayList<>();
        for (BlockFace face : ADJACENT_BLOCKFACES) {
            if (face == originatingFace) {
                continue;
            }

            Block adjacentBlock = block.getRelative(face);
            Block aboveBlock = adjacentBlock.getRelative(BlockFace.UP);
            Block aboveAboveBlock = aboveBlock.getRelative(BlockFace.UP);

            if (isBlockCovered(adjacentBlock, player)) {
                coveringBlocks.add(adjacentBlock);
            }

            else if (isBlockCovered(aboveBlock, player)) {
                coveringBlocks.add(aboveBlock);
            }

            else if (isBlockCovered(aboveAboveBlock, player)) {
                coveringBlocks.add(aboveAboveBlock);
            }

            else if (horizontalBlockFaces.contains(face)) {
                for (int i = 0; i < 4; i++) {
                    Block relativeHorizontalBlock = block.getRelative(face, i);
                    if (isBlockCovered(relativeHorizontalBlock, player)) {
                        tempHighlightBlock(relativeHorizontalBlock, player);
                        return true;
                    }
                    Block relativeHorizontalBlockDown = relativeHorizontalBlock.getRelative(BlockFace.DOWN);
                    if (isBlockCovered(relativeHorizontalBlockDown, player)) {
                        tempHighlightBlock(relativeHorizontalBlockDown, player);
                        return true;
                    }
                }
            }

            if (coveringBlocks.size() > 1) {
                tempHighlightBlock(coveringBlocks.get(1), player);
                return true;
            }
        }
        return false;
    }

    private boolean isBlockCovered(Block block, UUID player) {
        Material type = block.getType();
        return !type.isAir()
                && !ALL_BARREL_MATERIALS.contains(type)
                && Cannons.getPlugin().getCannonManager().getCannon(block.getLocation(), player) == null;
    }

    private void sendMessageToCannonOperator(Cannon cannon, UUID operator, Component message) {
        Player player = Bukkit.getPlayer(operator);
        if (player != null && cannon.getLocation().distanceSquared(player.getLocation()) < 900) { // 30 blocks
            player.sendMessage(message);
        }
    }

    private void tempHighlightBlock(Block block, UUID playerUUID) {
        Player player = Bukkit.getPlayer(playerUUID);
        if (player == null) {
            return;
        }

        PlayerCraft craft = CraftManager.getInstance().getCraftByPlayer(player);
        if (craft != null) {
            BlockHighlight.tempHighlightCraftBlockAt(MathUtils.bukkit2MovecraftLoc(block.getLocation()), craft);
        } else {
            player.spawnParticle(Particle.CAMPFIRE_SIGNAL_SMOKE, block.getLocation(), 1);
        }
    }
}
