package net.tylers1066.movecraftcannons.utils;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.utils.CombatUtil;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.util.MathUtils;
import org.bukkit.entity.Player;

public class MovecraftUtils {

    public static boolean isFriendly(Resident resident, Craft craft) {
        if (craft.getAudience() == null) {
            return false;
        }

        Player pilot = (Player) craft.getAudience();
        Resident pilotResident;
        Town pilotTown;
        Town residentTown;
        try {
            pilotResident = TownyAPI.getInstance().getResident(pilot.getUniqueId());
            if (pilotResident == null) {
                return false;
            }
            pilotTown = pilotResident.getTown();
            residentTown = resident.getTown();
        }
        catch (NotRegisteredException ex) {
            return false;
        }

        return CombatUtil.isSameTown(resident, pilotResident) || CombatUtil.isSameNation(residentTown, pilotTown) || CombatUtil.isAlly(residentTown, pilotTown);
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
