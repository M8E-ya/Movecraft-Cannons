package net.tylers1066.movecraftcannons.utils;

import at.pavlov.cannons.cannon.Cannon;
import at.pavlov.cannons.utils.CannonsUtil;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.utils.CombatUtil;
import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.craft.PlayerCraft;
import net.countercraft.movecraft.util.MathUtils;
import net.tylers1066.movecraftcannons.aiming.AimingCommand;
import net.tylers1066.movecraftcannons.listener.DetectionListener;
import net.tylers1066.movecraftcannons.localisation.I18nSupport;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Set;

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
        MovecraftLocation movecraftLocation = MathUtils.bukkit2MovecraftLoc(player.getLocation());
        for (Craft craft: CraftManager.getInstance().getCraftsInWorld(player.getWorld())) {
            if (MathUtils.locIsNearCraftFast(craft, movecraftLocation)) {
                return craft;
            }
        }
        return null;
    }


}
