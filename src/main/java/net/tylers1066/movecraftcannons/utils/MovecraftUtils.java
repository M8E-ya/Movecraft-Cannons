package net.tylers1066.movecraftcannons.utils;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.utils.CombatUtil;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.utils.MathUtils;
import org.bukkit.entity.Player;

public class MovecraftUtils {

    public static boolean isFriendly(Resident resident, Craft craft) {
        if (craft.getNotificationPlayer() == null) {
            return false;
        }

        Resident pilot;
        Town pilotTown;
        Town residentTown;
        try {
            pilot = TownyAPI.getInstance().getResident(craft.getNotificationPlayer().getUniqueId());
            if (pilot == null) return false;
            pilotTown = pilot.getTown();
            residentTown = resident.getTown();
        }
        catch (NotRegisteredException ex) {
            return false;
        }

        return CombatUtil.isSameTown(resident, pilot) || CombatUtil.isSameNation(residentTown, pilotTown) || CombatUtil.isAlly(residentTown, pilotTown);
    }

    public static Craft getCurrentShip(Player player) {
        for (Craft craft: CraftManager.getInstance().getCraftsInWorld(player.getWorld())) {
            if (MathUtils.locationInHitBox(craft.getHitBox(), player.getLocation())) {
                return craft;
            }
        }
        return null;
    }

}
