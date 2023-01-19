package net.tylers1066.movecraftcannons.listener;

import at.pavlov.cannons.cannon.Cannon;
import me.halfquark.squadronsreloaded.squadron.SquadronCraft;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.PilotedCraft;
import net.countercraft.movecraft.craft.SubCraft;
import net.countercraft.movecraft.craft.type.CraftType;
import net.countercraft.movecraft.events.CraftDetectEvent;
import net.countercraft.movecraft.events.CraftReleaseEvent;
import net.countercraft.movecraft.util.MathUtils;
import net.tylers1066.movecraftcannons.MovecraftCannons;
import net.tylers1066.movecraftcannons.config.Config;
import net.tylers1066.movecraftcannons.localisation.I18nSupport;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class DetectionListener implements Listener {
    public static Map<Craft, LinkedHashSet<Cannon>> cannonsOnCraft = new ConcurrentHashMap<>();

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCraftDetect(CraftDetectEvent event) {
        Craft craft = event.getCraft();
        String craftName = craft.getType().getStringProperty(CraftType.NAME);
        if (!Config.CraftAllowedCannons.containsKey(craftName)) {
            return;
        }

        UUID uuid;
        // Squadron crafts: set cannon owner UUIDs to squadron pilot
        if (craft instanceof SquadronCraft squadronCraft) {
            uuid = squadronCraft.getSquadron().getPilot().getUniqueId();
        }
        // Subcrafts: only include cannons that are inside the subcraft
        else if (craft instanceof SubCraft subCraft) {
            LinkedHashSet<Cannon> subCraftCannons = new LinkedHashSet<>();
            for (Cannon cannon: getCannonsOnCraft(subCraft.getParent())) {
                if (subCraft.getHitBox().contains(MathUtils.bukkit2MovecraftLoc(cannon.getCannonDesign().getFiringTrigger(cannon)))) {
                    subCraftCannons.add(cannon);
                }
            }
            if (!subCraftCannons.isEmpty()) {
                cannonsOnCraft.put(subCraft, subCraftCannons);
            }
            return;
        }
        else if (craft instanceof PilotedCraft pilotedCraft) {
            uuid = pilotedCraft.getPilot().getUniqueId();
        }
        else {
            return;
        }

        LinkedHashSet<Cannon> cannons = MovecraftCannons.getInstance().getCannons(craft.getHitBox(), craft.getWorld(), uuid);
        if (cannons.isEmpty()) {
            return;
        }

        double craftFirepower = 0;
        int maximumFirepower = Config.CraftFirepowerLimits.get(craftName);
        HashMap<String, Integer> cannonAmountMap = new HashMap<>();

        for (Cannon cannon: cannons) {
            String cannonName = cannon.getCannonDesign().getDesignName();
            if (!Config.CraftAllowedCannons.get(craftName).contains(cannonName)) {
                event.setFailMessage(String.format(I18nSupport.getInternationalisedString("Disallowed cannon"), cannon.getCannonDesign().getMessageName()));
                event.setCancelled(true);
                return;
            }
            cannon.setOwner(uuid);
            craftFirepower += Config.CannonFirepowerValues.get(cannonName);
            cannonAmountMap.merge(cannonName, 1, Integer::sum);
        }

        if (craftFirepower > maximumFirepower) {
            event.setFailMessage(String.format(I18nSupport.getInternationalisedString("Too much firepower"), maximumFirepower, (int) craftFirepower));
            event.setCancelled(true);
        }

        for (var entry: cannonAmountMap.entrySet()) {
            int max = Config.getMaxAllowedCannonOnCraft(craft, entry.getKey());
            int count = entry.getValue();
            if (max > -1 && count > max) {
                event.setFailMessage(String.format(I18nSupport.getInternationalisedString("Max cannons exceeded"), entry.getKey(), count, max));
                event.setCancelled(true);
            }
        }

        cannonsOnCraft.put(craft, cannons);
    }

    @EventHandler
    public void onRelease(CraftReleaseEvent event) {
        cannonsOnCraft.remove(event.getCraft());
    }

    /**
     * Returns a copy of the set of cannons associated with a given craft
     * @param craft - the craft to get cannons for
     * @return a copy of the set of cannons associated with the craft, ordered alphabetically according to their
     *         design name.
     */
    @NotNull
    public static LinkedHashSet<Cannon> getCannonsOnCraft(Craft craft) {
        if (cannonsOnCraft.containsKey(craft)) {
            return new LinkedHashSet<>(cannonsOnCraft.get(craft));
        }
        return new LinkedHashSet<>();
    }
}
