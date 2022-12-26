package net.tylers1066.movecraftcannons.scoreboard;

import at.pavlov.cannons.cannon.Cannon;
import me.halfquark.squadronsreloaded.squadron.Squadron;
import me.halfquark.squadronsreloaded.squadron.SquadronCraft;
import me.halfquark.squadronsreloaded.squadron.SquadronManager;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.PilotedCraft;
import net.countercraft.movecraft.craft.type.CraftType;
import net.countercraft.movecraft.events.CraftPilotEvent;
import net.countercraft.movecraft.events.CraftReleaseEvent;
import net.countercraft.movecraft.events.CraftSinkEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.tylers1066.movecraftcannons.MovecraftCannons;
import net.tylers1066.movecraftcannons.listener.DetectionListener;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.Iterator;

import static net.kyori.adventure.text.Component.space;
import static net.kyori.adventure.text.Component.text;
import static net.tylers1066.movecraftcannons.scoreboard.WeaponsHUD.getHullIntegrityColor;

public class SquadronsHUD implements Listener {

    public SquadronsHUD(MovecraftCannons plugin) {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Squadron squadron: SquadronManager.getInstance().getSquadronList()) {
                    updateSquadronsHUD(squadron.getPilot());
                }
            }
        }.runTaskTimerAsynchronously(plugin, 1L, 2L);
    }

    @EventHandler
    public void onCraftPilot(CraftPilotEvent event) {
        if (event.getCraft() instanceof SquadronCraft squadCraft) {
            createSquadronsHUD(squadCraft.getSquadronPilot(), squadCraft.getSquadron());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true) // run before craft is removed from sq
    public void onCraftSink(CraftSinkEvent event) {
        if (!(event.getCraft() instanceof SquadronCraft craft)) {
            return;
        }

        if (craft.getSquadron().getSize() == 0) {
            removeSquadronsHUD(craft.getSquadronPilot());
        } else {
            Team team = craft.getSquadronPilot().getScoreboard().getTeam(craft.toString());
            if (team != null) {
                team.prefix(text(craft.getType().getStringProperty(CraftType.NAME) + " (" + (craft.getSquadron().getCraftId(craft) + 1) + ")", NamedTextColor.DARK_RED, TextDecoration.STRIKETHROUGH));
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCraftRelease(CraftReleaseEvent event) {
        Craft craft = event.getCraft();
        if (!(craft instanceof PilotedCraft pcraft)) {
            return;
        }

        removeSquadronsHUD(pcraft.getPilot());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        removeSquadronsHUD(event.getPlayer());
    }

    public void createSquadronsHUD(Player pilot, Squadron squadron) {
        Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();

        Objective obj = board.registerNewObjective("SquadronHUD", "dummy", text("Squadron", NamedTextColor.WHITE, TextDecoration.BOLD));
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);

        int num = 1;
        for (SquadronCraft squadCraft: squadron.getCrafts()) {
            if (num > 15) {
                break;
            }

            Team craftTeam = board.registerNewTeam(squadCraft.toString());
            String entry = ChatColor.values()[num] + "";
            craftTeam.addEntry(entry);
            craftTeam.prefix(createLine(squadCraft));

            obj.getScore(entry).setScore(num);

            num++;
        }

        WeaponsHUD.removeWeaponsScoreboard(pilot);
        pilot.setScoreboard(board);
    }

    public void updateSquadronsHUD(Player player) {
        if (!SquadronManager.getInstance().hasSquadron(player)) {
            removeSquadronsHUD(player);
            return;
        }

        Squadron squadron = SquadronManager.getInstance().getPlayerSquadron(player, true);
        Scoreboard hud = player.getScoreboard();

        Objective obj = hud.getObjective("SquadronHUD");
        if (obj == null) {
            return;
        }
        obj.displayName(text("Squadron", NamedTextColor.WHITE, TextDecoration.BOLD));

        for (SquadronCraft squadCraft : squadron.getCrafts()) {
            Team team =  hud.getTeam(squadCraft.toString());
            if (team == null)
                continue;

            Component newLine = createLine(squadCraft);
            if (team.prefix().equals(newLine))
                continue;

            team.prefix(newLine);
        }
    }

    public void removeSquadronsHUD(Player player) {
        Scoreboard board = player.getScoreboard();
        if (board.getObjective("SquadronHUD") == null) {
            return;
        }

        player.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
    }

    private Component createLine(SquadronCraft craft) {
        int percentage = (int) (((double) craft.getTotalNonNegligibleBlocks() / (double) craft.getOrigBlockCount()) * 100);
        if (percentage == 0) {
            // The hull integrity would otherwise momentarily be 0% after the craft is piloted.
            percentage = 100;
        }

        var color = (craft.isLead()) ? NamedTextColor.AQUA : NamedTextColor.WHITE;
        var line = text()
                .append(text( craft.getType().getStringProperty(CraftType.NAME) + " (" + (craft.getSquadron().getCraftId(craft) + 1) + "): ", color)
                .append(text(percentage + "%", getHullIntegrityColor(percentage))));
        line.append(space().append(createCannonStatusLine(craft)));
        return line.build();
    }

    private Component createCannonStatusLine(SquadronCraft craft) {
        var component = text().append(text("("));
        Iterator<Cannon> cannonIterator = DetectionListener.getCannonsOnCraft(craft).iterator();
        while (cannonIterator.hasNext()) {
            Cannon cannon = cannonIterator.next();
            component.append(text(cannon.getCannonDesign().getMessageName().substring(0, 3), WeaponsHUD.getCannonColor(cannon)));
            if (cannonIterator.hasNext()) {
                component.append(text(" | "));
            }
        }
        return component.append(text(")")).build();
    }
}
