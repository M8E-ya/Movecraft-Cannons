package net.tylers1066.movecraftcannons.firing;

import at.pavlov.cannons.Cannons;
import at.pavlov.cannons.Enum.InteractAction;
import at.pavlov.cannons.cannon.Cannon;
import net.countercraft.movecraft.craft.Craft;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.tylers1066.movecraftcannons.aiming.AimingUtils;
import net.tylers1066.movecraftcannons.localisation.I18nSupport;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public class FiringUtils {
    private static final EnumSet<Material> materialSet = EnumSet.copyOf(List.of(Material.values()));

    public static void fireCannons(Player player, Set<Cannon> cannons, boolean useFiringVector) {
        Vector firingVector = getPlayerFiringVector(player);

        int numCannons = 0;
        for (Cannon cannon: cannons) {
            if (useFiringVector) {
                if (AimingUtils.cannonCanFireAtVector(cannon, firingVector)) {
                    fireCannon(cannon);
                    numCannons++;
                }
            }
            else {
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
     * This is specifically the furthest block within the player's server-side view distance
     * in the player's direction.
     *
     * @param  player the player
     * @return      a vector located where the player is looking at
     */
    @NotNull
    public static Vector getPlayerFiringVector(Player player) {
        // TODO: use per-player no-tick view distance once it has been re-implemented
        return player.getTargetBlock(materialSet, player.getViewDistance() * 16).getLocation().toVector();
    }
}
