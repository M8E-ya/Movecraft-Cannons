package net.tylers1066.movecraftcannons.utils;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.utils.CombatUtil;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.craft.PlayerCraft;
import net.countercraft.movecraft.util.MathUtils;
import org.bukkit.entity.Player;

public class MovecraftUtils {

    public static boolean isFriendly(Resident resident, PlayerCraft craft) {
        Resident pilotResident = TownyAPI.getInstance().getResident(craft.getPlayer().getUniqueId());
        if (pilotResident == null) {
            return false;
        }
        if (resident.equals(pilotResident)) {
            return true;
        }

        return CombatUtil.isSameTown(resident, pilotResident)
                || CombatUtil.isSameNation(resident, pilotResident)
                || CombatUtil.isAlly(resident.getName(), pilotResident.getName());
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
