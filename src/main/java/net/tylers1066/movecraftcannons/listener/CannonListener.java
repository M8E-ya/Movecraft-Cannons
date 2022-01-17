package net.tylers1066.movecraftcannons.listener;

import at.pavlov.cannons.cannon.Cannon;
import at.pavlov.cannons.event.CannonAfterCreateEvent;
import at.pavlov.cannons.event.CannonBeforeCreateEvent;
import at.pavlov.cannons.event.CannonDestroyedEvent;
import at.pavlov.cannons.event.CannonFireEvent;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.type.CraftType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.tylers1066.movecraftcannons.config.Config;
import net.tylers1066.movecraftcannons.utils.MovecraftUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class CannonListener implements Listener {
    @EventHandler
    public void beforeCannonCreate(CannonBeforeCreateEvent event) {
        Cannon cannon = event.getCannon();
        Craft craft = MovecraftUtils.getParentCraftFromLocation(cannon.getLocation());
        if (craft == null) {
            return;
        }
        if (!Config.CraftAllowedCannons.get(craft.getType().getStringProperty(CraftType.NAME)).contains(cannon.getCannonDesign().getDesignName())) {
            sendMessageToCannonOperator(cannon, event.getPlayer(), Component.text("This cannon cannot be created on this craft.", NamedTextColor.RED));
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onCannonCreate(CannonAfterCreateEvent event) {
        Cannon cannon = event.getCannon();
        Craft craft = MovecraftUtils.getParentCraftFromLocation(cannon.getLocation());
        if (craft == null) {
            return;
        }
        if (!Config.CraftAllowedCannons.get(craft.getType().getStringProperty(CraftType.NAME)).contains(cannon.getCannonDesign().getDesignName())) {
            return;
        }
        DetectionListener.cannonsOnCraft.computeIfAbsent(craft, k -> new HashSet<>()).add(cannon);
    }

    @EventHandler
    public void onCannonDestroy(CannonDestroyedEvent event) {
        for (Set<Cannon> cannons: DetectionListener.cannonsOnCraft.values()) {
            cannons.remove(event.getCannon());
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onCannonFire(CannonFireEvent event) {
        Cannon cannon = event.getCannon();
        Craft craft = MovecraftUtils.getParentCraftFromLocation(cannon.getLocation());
        if (craft == null) {
            return;
        }
        if (!Config.CraftAllowedCannons.get(craft.getType().getStringProperty(CraftType.NAME)).contains(cannon.getCannonDesign().getDesignName())) {
            sendMessageToCannonOperator(cannon, event.getPlayer(), Component.text("This cannon cannot be fired from this craft.", NamedTextColor.RED));
            event.setCancelled(true);
        }
    }

    private void sendMessageToCannonOperator(Cannon cannon, UUID operator, Component message) {
        Player player = Bukkit.getPlayer(operator);
        if (player != null && cannon.getLocation().distanceSquared(player.getLocation()) < 900) { // 30 blocks
            player.sendMessage(message);
        }
    }

}
