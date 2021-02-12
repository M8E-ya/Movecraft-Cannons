package net.tylers1066.movecraftcannons.aiming;

import at.pavlov.cannons.Aiming;
import at.pavlov.cannons.Cannons;
import at.pavlov.cannons.cannon.Cannon;
import at.pavlov.cannons.utils.CannonsUtil;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Resident;
import net.countercraft.movecraft.craft.Craft;
import net.tylers1066.movecraftcannons.MovecraftCannons;
import net.tylers1066.movecraftcannons.localisation.I18nSupport;
import net.tylers1066.movecraftcannons.utils.MovecraftUtils;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;

public class AimingCommand implements CommandExecutor {

    public Aiming aiming = Cannons.getPlugin().getAiming();

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            return false;
        }

        Player player = (Player) sender;
        Craft craft = MovecraftUtils.getCurrentShip(player);
        if (craft == null) {
            player.sendMessage(I18nSupport.getInternationalisedString("Not on ship"));
            return false;
        }

        Resident resident = TownyAPI.getInstance().getResident(player.getUniqueId());
        if (resident == null) {
            return false;
        }

        if (!MovecraftUtils.isFriendly(resident, craft)) {
            player.sendMessage(I18nSupport.getInternationalisedString("Unfriendly craft"));
            return false;
        }

        HashSet<Cannon> cannonList = (HashSet<Cannon>) MovecraftCannons.getInstance().getExistingCannonsOnCraft(craft);
        cannonList.removeIf(cannon -> cannon.getCannonDirection() != getCardinalDirection(player));
        if (cannonList.isEmpty()) {
            player.sendMessage(I18nSupport.getInternationalisedString("No cannons to aim"));
            return false;
        }

        int i = 0;
        Location eyeLocation = player.getEyeLocation();
        GunAngles angles;

        for (Cannon cannon : cannonList) {
            if (!cannon.canAimPitch(eyeLocation.getPitch())) {
                continue;
            }
            if (!cannon.canAimYaw(eyeLocation.getYaw())) {
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
        return true;
    }

    public static BlockFace getCardinalDirection(Player player) {
        float yaw = player.getEyeLocation().getYaw();
        yaw = (yaw % 360 + 360) % 360; // True modulo, as Java's modulo is weird for negative values
        if (yaw > 135 || yaw < -135) {
            return BlockFace.NORTH;
        } else if (yaw < -45) {
            return BlockFace.EAST;
        } else if (yaw > 45) {
            return BlockFace.WEST;
        } else {
            return BlockFace.SOUTH;
        }
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
    private GunAngles getGunAngle(Cannon cannon, double yaw, double pitch)
    {
        double horizontal = yaw - CannonsUtil.directionToYaw(cannon.getCannonDirection()) - cannon.getTotalHorizontalAngle();
        horizontal = horizontal % 360;
        while (horizontal < -180)
            horizontal = horizontal + 360;

        return new GunAngles(horizontal, -pitch - cannon.getTotalVerticalAngle());
    }
}