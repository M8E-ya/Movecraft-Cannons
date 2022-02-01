package net.tylers1066.movecraftcannons.utils;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.utils.CombatUtil;
import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.craft.PlayerCraft;
import net.countercraft.movecraft.craft.SubCraftImpl;
import net.countercraft.movecraft.util.MathUtils;
import net.tylers1066.movecraftcannons.listener.DetectionListener;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

public class MovecraftUtils {

    public static boolean isFriendly(Resident resident, PlayerCraft craft) {
        Resident pilotResident = TownyAPI.getInstance().getResident(craft.getPilot().getUniqueId());
        if (pilotResident == null) {
            return false;
        }
        if (resident.equals(pilotResident)) {
            return true;
        }

        return CombatUtil.isSameTown(resident, pilotResident)
                || CombatUtil.isSameNation(resident, pilotResident)
                || CombatUtil.isAlly(resident, pilotResident);
    }

    @Nullable
    public static Craft getCurrentShip(Player player) {
        MovecraftLocation movecraftLocation = MathUtils.bukkit2MovecraftLoc(player.getLocation());
        for (Craft craft: CraftManager.getInstance().getCraftsInWorld(player.getWorld())) {
            if (MathUtils.locIsNearCraftFast(craft, movecraftLocation)) {
                return craft;
            }
        }
        return null;
    }

    @Nullable
    public static Craft getParentCraftFromLocation(Location location) {
        MovecraftLocation mCannonLoc = MathUtils.bukkit2MovecraftLoc(location);
        for (Craft testCraft: CraftManager.getInstance().getCraftsInWorld(location.getWorld())) {
            if (!MathUtils.locIsNearCraftFast(testCraft, mCannonLoc)) {
                continue;
            }
            // If the craft is a subcraft, return its parent
            if (testCraft instanceof SubCraftImpl subcraft) {
                return subcraft.getParent();
            }
            else {
                return testCraft;
            }
        }
        return null;
    }

    @NotNull
    public static Set<Craft> getPlayerCraftsAtLocation(Location location) {
        Set<Craft> crafts = new HashSet<>();
        for (Craft craft: CraftManager.getInstance().getPlayerCraftsInWorld(location.getWorld())) {
            if (MathUtils.locationNearHitBox(craft.getHitBox(), location, 1D)) {
                crafts.add(craft);
            }
        }
        return crafts;
    }

    @NotNull
    public static List<String> getCannonTypesOnPlayerCurrentCraft(Player player) {
        Craft craft = MovecraftUtils.getCurrentShip(player);
        if (craft == null) {
            return Collections.emptyList();
        }

        List<String> cannonsOnCraft = DetectionListener.getCannonsOnCraft(craft).stream()
                .map(cannon -> cannon.getCannonDesign().getDesignName())
                .collect(Collectors.toList());

        if (cannonsOnCraft.isEmpty()) {
            return Collections.emptyList();
        }

        return cannonsOnCraft;
    }
}
