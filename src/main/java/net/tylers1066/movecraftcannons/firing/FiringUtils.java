package net.tylers1066.movecraftcannons.firing;

import at.pavlov.cannons.Cannons;
import at.pavlov.cannons.Enum.InteractAction;
import at.pavlov.cannons.cannon.Cannon;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.util.MathUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.tylers1066.movecraftcannons.aiming.AimingUtils;
import net.tylers1066.movecraftcannons.localisation.I18nSupport;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public class FiringUtils {
    private static final Set<Material> materialSet = EnumSet.allOf(Material.class);

    public static void fireCannons(Player player, Set<Cannon> cannons, boolean useFiringVector) {
        Vector firingVector = getPlayerFiringVector(player);

        int numCannons = 0;
        for (Cannon cannon: cannons) {
            if (useFiringVector && AimingUtils.cannonCanFireAtVector(cannon, firingVector)) {
                fireCannon(cannon);
                numCannons++;
            }
            else if (!useFiringVector) {
                fireCannon(cannon);
            }
        }

        player.sendActionBar(Component.text(String.format(I18nSupport.getInternationalisedString("Firing cannons"), numCannons), TextColor.color(0xc3f09e)));
    }

    public static void fireCannons(Player player, Set<Cannon> cannons, BlockFace direction) {
        int numCannons = 0;
        for (Cannon cannon: cannons) {
            if (cannon.getCannonDirection() == direction) {
                fireCannon(cannon);
                numCannons++;
            }
        }

        player.sendActionBar(Component.text(String.format(I18nSupport.getInternationalisedString("Firing cannons"), numCannons), TextColor.color(0xc3f09e)));
    }

    private static void fireCannon(Cannon cannon) {
        Cannons.getPlugin().getFireCannon().redstoneFiring(cannon, InteractAction.fireAutoaim);
    }

    /**
     * Returns the block that the player is looking at as a vector.
     * <p>
     * This is specifically the furthest block within the player's client-side view
     * in the player's direction.
     *
     * @param  player the player
     * @return      a vector located where the player is looking at
     */
    @NotNull
    public static Vector getPlayerFiringVector(Player player) {
        Location target = player.getTargetBlock(materialSet, player.getSendViewDistance() * 16).getLocation();
        Craft craft = CraftManager.getInstance().getCraftByPlayer(player);
        // View blocked by own craft: use non-convergent aiming
        if (craft != null && craft.getHitBox().contains(MathUtils.bukkit2MovecraftLoc(target))) {
            return player.getEyeLocation().toVector();
        }
        return target.toVector();
    }
}
