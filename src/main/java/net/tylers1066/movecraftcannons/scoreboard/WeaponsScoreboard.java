package net.tylers1066.movecraftcannons.scoreboard;

import at.pavlov.cannons.cannon.Cannon;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.craft.PilotedCraft;
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
        if (!(craft instanceof PilotedCraft pcraft)) {
            return;
        }

        LinkedHashSet<Cannon> cannons = DetectionListener.getCannonsOnCraft(craft);
        if (!cannons.isEmpty()) {
            createWeaponsScoreboard(pcraft.getPilot(), cannons);
        }
    }

    @EventHandler
    public void onCraftRelease(CraftSinkEvent event) {
        Craft craft = event.getCraft();
        if (!(craft instanceof PilotedCraft pcraft)) {
            return;
        }

        removeWeaponsScoreboard(pcraft.getPilot());
    }

    @EventHandler
    public void onCraftRelease(CraftReleaseEvent event) {
        Craft craft = event.getCraft();
        if (!(craft instanceof PilotedCraft pcraft)) {
            return;
        }

        removeWeaponsScoreboard(pcraft.getPilot());
    }

    public void createWeaponsScoreboard(Player player, LinkedHashSet<Cannon> cannons) {
        Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective obj = board.registerNewObjective("Weapons", "dummy", Component.text("Weapons", NamedTextColor.GRAY, TextDecoration.BOLD));
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);

        int num = 1;
        for (Cannon cannon: cannons) {
            if (num > 13)
                break;

            Team cannonTeam = board.registerNewTeam(cannon.getUID().toString());
            String entry = ChatColor.values()[num] + "";
            cannonTeam.addEntry(entry);
            cannonTeam.prefix(createCannonLine(cannon));

            obj.getScore(entry).setScore(num);

            num++;
        }

        hasWeaponsScoreboard.add(player.getUniqueId());
        player.setScoreboard(board);
    }

    public void updateWeaponsScoreboard(Player player) {
        PilotedCraft craft = CraftManager.getInstance().getCraftByPlayer(player);
        if (craft == null || player != craft.getPilot())
            return;

        Scoreboard board = player.getScoreboard();

        LinkedHashSet<Cannon> cannons = DetectionListener.getCannonsOnCraft(craft);
        if (cannons.isEmpty()) {
            return;
        }

        for (Cannon cannon: cannons) {
            Team team =  board.getTeam(cannon.getUID().toString());
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
        if (board.getObjective("Weapons") != null) {
            player.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
        }
        hasWeaponsScoreboard.remove(player.getUniqueId());
    }

    private Component createCannonLine(Cannon cannon) {
        var line = Component.text()
                .append(Component.text(cannon.getCannonDesign().getMessageName(), getCannonColor(cannon)))
                .append(Component.text(" (" + shortenBlockFace(cannon.getCannonDirection()) + ")", NamedTextColor.WHITE));

        if (cannon.isLoaded() && cannon.getChargesRemaining() > 1) {
            line.append(Component.text(" - " + cannon.getChargesRemaining() + " charge(s)", NamedTextColor.WHITE));
        }

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
