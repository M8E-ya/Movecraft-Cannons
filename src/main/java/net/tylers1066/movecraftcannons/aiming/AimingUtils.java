package net.tylers1066.movecraftcannons.aiming;

import at.pavlov.cannons.Aiming;
import at.pavlov.cannons.Cannons;
import at.pavlov.cannons.cannon.Cannon;
import at.pavlov.cannons.utils.CannonsUtil;
import net.countercraft.movecraft.combat.config.Config;
import net.countercraft.movecraft.craft.Craft;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.tylers1066.movecraftcannons.listener.DetectionListener;
import net.tylers1066.movecraftcannons.localisation.I18nSupport;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Set;
import java.util.UUID;

public class AimingUtils {

    private static final Aiming aiming = Cannons.getPlugin().getAiming();
    private static final HashMap<UUID, String> cannonPlayerSelections = new HashMap<>();

    public static void aimCannonsOnCraft(Craft craft, Player player, @Nullable String cannonType) {
        Set<Cannon> cannonList = DetectionListener.cannonsOnCraft.get(craft);
        if (cannonList == null || cannonList.isEmpty()) {
            player.sendMessage(I18nSupport.getInternationalisedString("No cannons to aim"));
            return;
        }

        if (cannonType == null && cannonPlayerSelections.containsKey(player.getUniqueId())) {
            cannonType = cannonPlayerSelections.get(player.getUniqueId());
        }

        int i = 0;
        // TODO: use per-player no-tick view distance once it has been re-implemented
        Vector targetVector = player.getTargetBlock(Config.Transparent, player.getWorld().getViewDistance() * 16).getLocation().toVector();
        GunAngles angles;

        for (Cannon cannon : cannonList) {
            if (cannonType != null && !cannon.getCannonDesign().getDesignName().equals(cannonType)) {
                continue;
            }

            Vector muzzleVector = cannon.getMuzzle().toVector();
            Vector direction = targetVector.clone().subtract(muzzleVector);

            double yaw = CannonsUtil.vectorToYaw(direction);
            double pitch = CannonsUtil.vectorToPitch(direction);

            if (!cannon.canAimPitch(pitch) || !cannon.canAimYaw(yaw)) {
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
}
