package net.tylers1066.movecraftcannons.listener;

import at.pavlov.cannons.cannon.Cannon;
import net.countercraft.movecraft.craft.*;
import net.countercraft.movecraft.craft.type.CraftType;
import net.countercraft.movecraft.events.CraftDetectEvent;
import net.countercraft.movecraft.events.CraftPilotEvent;
import net.countercraft.movecraft.events.CraftReleaseEvent;
import net.tylers1066.movecraftcannons.MovecraftCannons;
import net.tylers1066.movecraftcannons.config.Config;
import net.tylers1066.movecraftcannons.localisation.I18nSupport;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import javax.print.attribute.HashAttributeSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class DetectionListener implements Listener {

    public static HashMap<Craft, HashSet<Cannon>> cannonsOnCraft = new HashMap<>();

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCraftDetect(CraftDetectEvent event) {
        Craft craft = event.getCraft();
        String craftName = craft.getType().getStringProperty(CraftType.NAME);
        if (!Config.CraftAllowedCannons.containsKey(craftName) || Config.CraftAllowedCannons.get(craftName).isEmpty()) {
            return;
        }

        UUID uuid;
        // SubcraftRotate craft cannons are controlled by their parent craft
        if (craft instanceof SubCraftImpl) {
            return;
        }
        if (craft instanceof PilotedCraft pilotedCraft) {
            uuid = pilotedCraft.getPilot().getUniqueId();
        }
        else {
            return;
        }

        HashSet<Cannon> cannons = MovecraftCannons.getInstance().getCannons(craft.getHitBox(), craft.getWorld(), uuid);
        if (cannons.isEmpty()) {
            return;
        }

        int craftFirepower = 0;
        int maximumFirepower = Config.CraftFirepowerLimits.get(craftName);
        HashMap<String, Integer> cannonAmountMap = new HashMap<>();

        for (Cannon cannon: cannons) {
            String cannonName = cannon.getCannonDesign().getDesignName();
            if (!Config.CraftAllowedCannons.get(craftName).contains(cannonName)) {
                event.setFailMessage(String.format(I18nSupport.getInternationalisedString("Disallowed cannon"), cannonName));
                event.setCancelled(true);
                return;
            }
            craftFirepower += Config.CannonFirepowerValues.get(cannonName);
            cannonAmountMap.merge(cannonName, 1, Integer::sum);
        }

        if (craftFirepower > maximumFirepower) {
            event.setFailMessage(String.format(I18nSupport.getInternationalisedString("Too much firepower"), maximumFirepower, craftFirepower));
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

    public static Set<Cannon> getCannonsOnCraft(Craft craft) {
        return cannonsOnCraft.getOrDefault(craft, new HashSet<>());
    }
}
