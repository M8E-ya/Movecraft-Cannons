package net.tylers1066.movecraftcannons.scoreboard;

import at.pavlov.cannons.cannon.Cannon;
import at.pavlov.cannons.event.CannonDestroyedEvent;
import net.countercraft.movecraft.craft.*;
import net.countercraft.movecraft.events.CraftPilotEvent;
import net.countercraft.movecraft.events.CraftReleaseEvent;
import net.countercraft.movecraft.events.CraftSinkEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.tylers1066.movecraftcannons.MovecraftCannons;
import net.tylers1066.movecraftcannons.listener.DetectionListener;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

public class WeaponsScoreboard implements Listener {
    private final Set<UUID> hasWeaponsScoreboard = new HashSet<>();

    public WeaponsScoreboard() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (UUID uuid: hasWeaponsScoreboard) {
                    Player player = Bukkit.getPlayer(uuid);
                    if (player == null)
                        continue;

                    updateWeaponsScoreboard(player);
                }
            }
        }.runTaskTimerAsynchronously(MovecraftCannons.getInstance(), 1L, 2L);
    }

    @EventHandler
    public void onCraftPilot(CraftPilotEvent event) {
        Craft craft = event.getCraft();
        if (!(craft instanceof PlayerCraft pcraft)) {
            return;
        }

        LinkedHashSet<Cannon> cannons = DetectionListener.getCannonsOnCraft(craft);
        if (!cannons.isEmpty()) {
            createWeaponsScoreboard(pcraft, cannons);
        }
    }

    @EventHandler
    public void onCraftRelease(CraftSinkEvent event) {
        Craft craft = event.getCraft();
        if (!(craft instanceof PlayerCraft pcraft)) {
            return;
        }

        removeWeaponsScoreboard(pcraft.getPilot());
    }

    @EventHandler(priority = EventPriority.NORMAL) // Run before we remove the cannon from the ship
    public void onCannonDestroy(CannonDestroyedEvent event) {
        Cannon cannon = event.getCannon();
        for (var entry: DetectionListener.cannonsOnCraft.entrySet()) {

            if (!(entry.getKey() instanceof PlayerCraft pcraft) || !entry.getValue().contains(cannon)) {
                continue;
            }

            Team team = pcraft.getPilot().getScoreboard().getTeam(cannon.getUID().toString());
            if (team != null) {
                team.prefix(Component.text(cannon.getCannonDesign().getMessageName(), NamedTextColor.DARK_RED));
            }
        }
    }

    @EventHandler
    public void onCraftRelease(CraftReleaseEvent event) {
        Craft craft = event.getCraft();
        if (!(craft instanceof PlayerCraft pcraft)) {
            return;
        }

        removeWeaponsScoreboard(pcraft.getPilot());
    }

    public void createWeaponsScoreboard(PilotedCraft craft, LinkedHashSet<Cannon> cannons) {
        Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective obj = board.registerNewObjective("CraftHUD", "dummy", createHullIntegrityLine(craft));
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);

        int num = 1;
        for (Cannon cannon: cannons) {
            if (num > 15)
                break;

            Team cannonTeam = board.registerNewTeam(cannon.getUID().toString().substring(0, 15));
            String entry = ChatColor.values()[num] + "";
            cannonTeam.addEntry(entry);
            cannonTeam.prefix(createCannonLine(cannon));

            obj.getScore(entry).setScore(num);

            num++;
        }

        hasWeaponsScoreboard.add(craft.getPilot().getUniqueId());
        craft.getPilot().setScoreboard(board);
    }

    public void updateWeaponsScoreboard(Player player) {
        PilotedCraft craft = CraftManager.getInstance().getCraftByPlayer(player);
        if (craft == null || player != craft.getPilot())
            return;

        Scoreboard board = player.getScoreboard();

        Objective obj = board.getObjective("CraftHUD");
        if (obj == null) {
            return;
        }
        obj.displayName(createHullIntegrityLine(craft));

        LinkedHashSet<Cannon> cannons = DetectionListener.getCannonsOnCraft(craft);
        if (cannons.isEmpty()) {
            return;
        }

        for (Cannon cannon: cannons) {
            Team team =  board.getTeam(cannon.getUID().toString().substring(0, 15));
            if (team == null)
                continue;

            Component newLine = createCannonLine(cannon);
            if (team.prefix().equals(newLine))
                continue;

            team.prefix(createCannonLine(cannon));
        }
    }

    public void removeWeaponsScoreboard(Player player) {
        Scoreboard board = player.getScoreboard();
        if (board.getObjective("CraftHUD") != null) {
            player.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
        }
        hasWeaponsScoreboard.remove(player.getUniqueId());
    }

    private Component createCannonLine(Cannon cannon) {
        var line = Component.text()
                .append(Component.text(cannon.getCannonDesign().getMessageName(), getCannonColor(cannon)))
                .append(Component.text(" (" + shortenBlockFace(cannon.getCannonDirection()) + ")", NamedTextColor.WHITE));

        if (cannon.isLoaded() && cannon.getChargesRemaining() > 1) {
            line.append(Component.text(" - " + cannon.getChargesRemaining() + " charges", NamedTextColor.WHITE));
        }

        return line.build();
    }

    private Component createHullIntegrityLine(Craft craft) {
        int percentage = (int) (((double) craft.getTotalNonNegligibleBlocks() / (double) craft.getOrigBlockCount()) * 100);

        var line = Component.text()
                .append(Component.text("Hull Integrity: ", NamedTextColor.WHITE, TextDecoration.BOLD))
                .append(Component.text(percentage + "%", getHullIntegrityColor(percentage)));

        return line.build();
    }

    private TextColor getCannonColor(Cannon cannon) {
        // Destroyed
        if (!cannon.isValid()) {
            return NamedTextColor.RED;
        }
        // Firing
        else if (cannon.isFiring()) {
            return NamedTextColor.AQUA;
        }
        // Loading / on cooldown
        else if (cannon.isLoading() || cannon.barrelTooHot()) {
            return NamedTextColor.YELLOW;
        }
        // Loaded
        else if (cannon.isLoaded()) {
            return NamedTextColor.GREEN;
        }
        // Unloaded
        else {
            return TextColor.color(0xeb9d9d);
        }
    }

    private TextColor getHullIntegrityColor(int percentage) {
        if (percentage <= 100 && percentage >= 80) {
            return NamedTextColor.GREEN;
        }
        else if (percentage < 80 && percentage >= 70) {
            return NamedTextColor.YELLOW;
        }
        return NamedTextColor.RED;
    }


    private String shortenBlockFace(BlockFace face) {
        return switch (face) {
            case NORTH -> "N";
            case SOUTH -> "S";
            case WEST -> "W";
            case EAST -> "E";
            default -> face.name();
        };
    }
}
