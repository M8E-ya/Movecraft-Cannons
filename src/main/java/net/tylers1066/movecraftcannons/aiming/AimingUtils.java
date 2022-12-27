package net.tylers1066.movecraftcannons.aiming;

import at.pavlov.cannons.Aiming;
import at.pavlov.cannons.Cannons;
import at.pavlov.cannons.cannon.Cannon;
import at.pavlov.cannons.utils.CannonsUtil;
import net.countercraft.movecraft.combat.features.directors.Directors;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.util.MathUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.tylers1066.movecraftcannons.MovecraftCannons;
import net.tylers1066.movecraftcannons.config.Config;
import net.tylers1066.movecraftcannons.listener.DetectionListener;
import net.tylers1066.movecraftcannons.localisation.I18nSupport;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class AimingUtils {

    private static final Aiming aiming = Cannons.getPlugin().getAiming();
    private static final HashMap<UUID, String> cannonPlayerSelections = new HashMap<>();

    public static void aimCannonsOnCraft(Craft craft, Player player, @Nullable String cannonType) {
        Set<Cannon> cannonList = DetectionListener.cannonsOnCraft.get(craft);
        if (cannonList == null || cannonList.isEmpty() || !craft.getType().getBoolProperty(MovecraftCannons.CAN_USE_CANNONS)) {
            player.sendActionBar(Component.text(I18nSupport.getInternationalisedString("No cannons to aim"), TextColor.color(0xd6524b)));
            return;
        }

        if (cannonType == null && cannonPlayerSelections.containsKey(player.getUniqueId())) {
            cannonType = cannonPlayerSelections.get(player.getUniqueId());
        }

        int i = 0;
        Vector targetVector = getPlayerTargetVector(player);
        GunAngles angles;

        for (Cannon cannon : cannonList) {
            if (cannonType != null && !cannon.getCannonDesign().getMessageName().equals(cannonType)) {
                continue;
            }

            Vector muzzleVector = cannon.getMuzzle().toVector();
            Vector direction = targetVector.clone().subtract(muzzleVector);

            double yaw = CannonsUtil.vectorToYaw(direction);
            double pitch = CannonsUtil.vectorToPitch(direction);

            if (!cannonCanFireAtVector(cannon, targetVector)) {
                continue;
            }

            angles = getGunAngle(cannon, yaw, pitch);
            cannon.setVerticalAngle(angles.getVertical());
            cannon.setHorizontalAngle(angles.getHorizontal());

            aiming.showAimingVector(cannon, player);
            CannonsUtil.playSound(cannon.getMuzzle(), cannon.getCannonDesign().getSoundAdjust());
            i++;
        }
        TextColor color = (i == 0) ? TextColor.color(0xffb2ab) : TextColor.color(0xc3f09e);
        player.sendActionBar(Component.text(String.format(I18nSupport.getInternationalisedString("Changed aim"), i), color));
    }

    public static HashMap<UUID, String> getPlayerCannonSelections() {
        return cannonPlayerSelections;
    }

    @NotNull
    public static Vector getPlayerTargetVector(Player player) {
        Location target = getTargetBlock(player, Directors.Transparent, player.getSendViewDistance() * 16).getLocation();
        Craft craft = CraftManager.getInstance().getCraftByPlayer(player);
        // View blocked by own craft: use non-convergent aiming
        if (craft != null && craft.getHitBox().contains(MathUtils.bukkit2MovecraftLoc(target))) {
            return player.getEyeLocation().toVector();
        }
        return target.toVector();
    }

    public static boolean cannonCanFireAtVector(Cannon cannon, Vector targetVector) {
        if (Config.AlwaysFireWhenTriggeredCannons.contains(cannon.getCannonDesign().getDesignID())) {
            return true;
        }

        Vector muzzleVector = cannon.getMuzzle().toVector();
        Vector direction = targetVector.clone().subtract(muzzleVector);

        double yaw = CannonsUtil.vectorToYaw(direction);
        double pitch = CannonsUtil.vectorToPitch(direction);

        return cannon.canAimPitch(pitch) && cannon.canAimYaw(yaw);
    }

    private static class GunAngles
    {
        private double horizontal;
        private double vertical;

        public GunAngles(double horizontal, double vertical)
        {
            this.setHorizontal(horizontal);
            this.setVertical(vertical);
        }

        public double getHorizontal() {
            return horizontal;
        }

        public void setHorizontal(double horizontal) {
            this.horizontal = horizontal;
        }

        public double getVertical() {
            return vertical;
        }

        public void setVertical(double vertical) {
            this.vertical = vertical;
        }
    }


    /**
     * evaluates the difference between actual cannon direction and the given direction
     * @param cannon operated cannon
     * @param yaw yaw of the direction to aim
     * @param pitch pitch of the direction to aim
     * @return new cannon aiming direction
     */
    private static GunAngles getGunAngle(Cannon cannon, double yaw, double pitch)
    {
        double horizontal = yaw - CannonsUtil.directionToYaw(cannon.getCannonDirection());
        horizontal = horizontal % 360;
        while (horizontal < -180)
            horizontal = horizontal + 360;

        return new GunAngles(horizontal, -pitch);
    }

    // See CraftLivingEntity#getLineOfSight
    private static Block getTargetBlock(Player player, Set<Material> transparent, int maxDistance) {
        if (transparent == null) {
            transparent = EnumSet.of(Material.AIR, Material.CAVE_AIR, Material.VOID_AIR);
        }
        maxDistance = Math.min(maxDistance, player.getSendViewDistance() * 16);
        Block block = null;
        Iterator<Block> itr = new BlockIterator(player, maxDistance);
        while (itr.hasNext()) {
            block = itr.next();
            Material material = block.getType();
            if (!transparent.contains(material)) {
                break;
            }
        }
        return block;
    }
}
