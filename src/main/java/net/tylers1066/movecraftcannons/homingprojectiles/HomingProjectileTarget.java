package net.tylers1066.movecraftcannons.homingprojectiles;

import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.craft.Craft;
import net.tylers1066.movecraftcannons.utils.MovecraftUtils;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

public class HomingProjectileTarget {
    private final @NotNull Craft targetCraft;
    private final @NotNull MovecraftLocation targetLocationMidpointOffset;

    public HomingProjectileTarget(@NotNull Craft craft) {
        this.targetCraft = craft;

        MovecraftLocation randomLocation = MovecraftUtils.getRandomBlockOnHitBox(craft);
        this.targetLocationMidpointOffset = craft.getHitBox().getMidPoint().subtract(randomLocation);
    }

    public Location getTargetLocation() {
        return targetCraft.getHitBox().getMidPoint().subtract(targetLocationMidpointOffset).toBukkit(targetCraft.getWorld());
    }

    public @NotNull Craft getTargetCraft() {
        return targetCraft;
    }
}
