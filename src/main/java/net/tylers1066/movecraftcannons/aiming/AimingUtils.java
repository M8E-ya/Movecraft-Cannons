package net.tylers1066.movecraftcannons.aiming;

import at.pavlov.cannons.Aiming;
import at.pavlov.cannons.Cannons;
import at.pavlov.cannons.cannon.Cannon;
import at.pavlov.cannons.utils.CannonsUtil;
import net.countercraft.movecraft.craft.Craft;
import net.tylers1066.movecraftcannons.listener.DetectionListener;
import net.tylers1066.movecraftcannons.localisation.I18nSupport;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Set;

public class AimingUtils {

    private static final Aiming aiming = Cannons.getPlugin().getAiming();

    public static void aimCannonsOnCraft(Craft craft, Player player) {
        Set<Cannon> cannonList = DetectionListener.cannonsOnCraft.get(craft);
        //cannonList.removeIf(cannon -> cannon.getCannonDirection() != player.getFacing());
        if (cannonList == null || cannonList.isEmpty()) {
            player.sendMessage(I18nSupport.getInternationalisedString("No cannons to aim"));
            return;
        }

        int i = 0;
        Location eyeLocation = player.getEyeLocation();
        GunAngles angles;

        for (Cannon cannon : cannonList) {
            if (!cannon.canAimPitch(eyeLocation.getPitch()) || !cannon.canAimYaw(eyeLocation.getYaw())) {
                continue;
            }
            angles = getGunAngle(cannon, eyeLocation.getYaw(), eyeLocation.getPitch());
            cannon.setVerticalAngle(angles.getVertical());
            cannon.setHorizontalAngle(angles.getHorizontal());

            aiming.showAimingVector(cannon, player);
            CannonsUtil.playSound(cannon.getMuzzle(), cannon.getCannonDesign().getSoundAdjust());
            i++;
        }
        player.sendMessage(String.format(I18nSupport.getInternationalisedString("Changed aim"), i));
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
