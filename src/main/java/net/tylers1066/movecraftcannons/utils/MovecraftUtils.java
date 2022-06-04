package net.tylers1066.movecraftcannons.utils;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.utils.CombatUtil;
import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.craft.*;
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
    public static Set<Craft> getPlayerCraftsAtLocation(Location location, double radius) {
        Set<Craft> crafts = new HashSet<>();
        for (Craft craft: CraftManager.getInstance().getPlayerCraftsInWorld(location.getWorld())) {
            if (MathUtils.locationNearHitBox(craft.getHitBox(), location, radius)) {
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

    public static Location getCraftHitBoxMidPoint(Craft craft) {
        return craft.getHitBox().getMidPoint().toBukkit(craft.getWorld());
    }

    public static Location getRandomBlockOnHitBox(Craft craft) {
        return craft.getHitBox().asSet().iterator().next().toBukkit(craft.getWorld());
    }

    @Nullable
    public static Craft getNearestCraftToCraft(Craft origin, Set<Craft> crafts) {
        Location originMidPoint = origin.getHitBox().getMidPoint().toBukkit(origin.getWorld());

        Craft nearestCraft = null;
        double nearestCraftDistanceSquared = Integer.MAX_VALUE;
        for (Craft testCraft: crafts) {
            if (testCraft.getWorld() != origin.getWorld()) {
                continue;
            }
            double craftDistanceSquared = originMidPoint.distanceSquared(testCraft.getHitBox().getMidPoint().toBukkit(origin.getWorld()));
            if (craftDistanceSquared < nearestCraftDistanceSquared) {
                nearestCraft = testCraft;
                nearestCraftDistanceSquared = originMidPoint.distanceSquared(testCraft.getHitBox().getMidPoint().toBukkit(origin.getWorld()));
            }
        }
        return nearestCraft;
    }
}
