package net.tylers1066.movecraftcannons.homingprojectiles;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class HomingProjectileType {
    @NotNull private final String name;
    @NotNull private final List<String> countermeasureProjectileNames;
    private final double turningFactor;
    private final boolean isWater;

    public HomingProjectileType(@NotNull String name, @Nullable List<String> countermeasureProjectileNames, double turningFactor, boolean isWater) {
        this.name = name;
        this.turningFactor = turningFactor;
        this.isWater = isWater;
        this.countermeasureProjectileNames = Objects.requireNonNullElse(countermeasureProjectileNames, Collections.emptyList());
    }

    public @NotNull String getName() {
        return name;
    }

    /**
     *
     * @return a value that represents how much the projectile will 'bend' towards its target.
     * The closer this value is to 1, the more it will bend.
     */
    public double getTurningFactor() {
        return turningFactor;
    }

    /**
     *
     * @return whether the projectile can only travel underwater
     */
    public boolean isWater() {
        return isWater;
    }

    public List<String> getCountermeasureProjectiles() {
        return countermeasureProjectileNames;
    }
}
