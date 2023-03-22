package net.tylers1066.movecraftcannons.scoreboard;

import at.pavlov.cannons.cannon.Cannon;
import at.pavlov.cannons.event.CannonDestroyedEvent;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.craft.PilotedCraft;
import net.countercraft.movecraft.craft.PlayerCraft;
import net.countercraft.movecraft.craft.type.CraftType;
import net.countercraft.movecraft.events.CraftDetectEvent;
import net.countercraft.movecraft.events.CraftReleaseEvent;
import net.countercraft.movecraft.events.CraftSinkEvent;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.tylers1066.movecraftcannons.MovecraftCannons;
import net.tylers1066.movecraftcannons.listener.DetectionListener;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.*;

import java.util.*;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed;

public class WeaponsHUD implements Listener {
    private static final Map<UUID, BossBar> weaponScoreboardPlayers = new HashMap<>();

    public WeaponsHUD(MovecraftCannons plugin) {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Map.Entry<UUID, BossBar> entry: weaponScoreboardPlayers.entrySet()) {
                    Player player = Bukkit.getPlayer(entry.getKey());
                    if (player == null) {
                        continue;
                    }

                    updateWeaponsScoreboard(player, entry.getValue());
                }
            }
        }.runTaskTimerAsynchronously(plugin, 1L, 2L);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCraftPilot(CraftDetectEvent event) {
        Craft craft = event.getCraft();
        if (!(craft instanceof PlayerCraft pcraft)) {
            return;
        }

        LinkedHashSet<Cannon> cannons = DetectionListener.getCannonsOnCraft(craft);
        if (!cannons.isEmpty()) {
            createWeaponsScoreboard(pcraft, cannons);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCraftSink(CraftSinkEvent event) {
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

            Team team = pcraft.getPilot().getScoreboard().getTeam(cannon.getUID().toString().substring(0, 15));
            if (team != null) {
                team.prefix(text(cannon.getCannonDesign().getMessageName(), NamedTextColor.DARK_RED, TextDecoration.STRIKETHROUGH));
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCraftRelease(CraftReleaseEvent event) {
        Craft craft = event.getCraft();
        if (!(craft instanceof PlayerCraft pcraft)) {
            return;
        }

        removeWeaponsScoreboard(pcraft.getPilot());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        removeWeaponsScoreboard(event.getPlayer());
    }

    public void createWeaponsScoreboard(PilotedCraft craft, LinkedHashSet<Cannon> cannons) {
        Player pilot = craft.getPilot();
        Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();

        Objective obj = board.registerNewObjective("CraftHUD", Criteria.DUMMY, text("Weapons", NamedTextColor.WHITE, TextDecoration.BOLD));
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

        BossBar bossBar = createBossBar(craft);
        pilot.showBossBar(bossBar);

        weaponScoreboardPlayers.put(pilot.getUniqueId(), bossBar);
        pilot.setScoreboard(board);
    }

    public void updateWeaponsScoreboard(Player player, BossBar bossBar) {
        PilotedCraft craft = CraftManager.getInstance().getCraftByPlayer(player);
        if (craft == null || player != craft.getPilot())
            return;

        Scoreboard board = player.getScoreboard();

        Objective obj = board.getObjective("CraftHUD");
        if (obj == null) {
            return;
        }

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

            team.prefix(newLine);
        }

        updateBossBar(craft, bossBar, getHullIntegrity(craft), getSinkingHullIntegrity(craft));
    }

    public static void removeWeaponsScoreboard(Player player) {
        Scoreboard board = player.getScoreboard();
        if (board.getObjective("CraftHUD") == null) {
            return;
        }

        player.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
        BossBar bossBar = weaponScoreboardPlayers.remove(player.getUniqueId());
        player.hideBossBar(bossBar);
    }

    private Component createCannonLine(Cannon cannon) {
        var line = text()
                .append(text(cannon.getCannonDesign().getMessageName(), getCannonColor(cannon)))
                .append(text(" (" + shortenBlockFace(cannon.getCannonDirection()) + ")", NamedTextColor.WHITE));

        if (cannon.isLoaded() && cannon.getChargesRemaining() > 1) {
            line.append(text(" - " + cannon.getChargesRemaining() + " charges", NamedTextColor.WHITE));
        }

        return line.build();
    }

    private Component createHullIntegrityLine(Craft craft) {
        int hullIntegrity = getHullIntegrity(craft);
        var line = text()
                .append(text("Hull Integrity: ", NamedTextColor.WHITE, TextDecoration.BOLD))
                .append(text(hullIntegrity + "%", getHullIntegrityColor(hullIntegrity, getSinkingHullIntegrity(craft))));
        return line.build();
    }

    public static TextColor getCannonColor(Cannon cannon) {
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

    public static NamedTextColor getHullIntegrityColor(int percentage, double sinkPercentage) {
        if (percentage >= sinkPercentage + 20) {
            return NamedTextColor.GREEN;
        }
        else if (percentage >= sinkPercentage + 10) {
            return NamedTextColor.YELLOW;
        }
        return NamedTextColor.RED;
    }

    public static int getHullIntegrity(Craft craft) {
        int percentageBlocksLeft;
        double overallSinkPercent = craft.getType().getDoubleProperty(CraftType.OVERALL_SINK_PERCENT);

        if (overallSinkPercent == 0) {
            percentageBlocksLeft = (int) Math.round(craft.getCachedFlyBlockPercent());
        } else {
            percentageBlocksLeft = (int) Math.round((((double) craft.getTotalNonNegligibleBlocks() / (double) craft.getOrigBlockCount()) * 100));
        }

        if (percentageBlocksLeft == 0) {
            // The hull integrity would otherwise momentarily be 0% after the craft is piloted.
            percentageBlocksLeft = 100;
        }

        return percentageBlocksLeft;
    }

    // Find the percentage at which the craft will really sink
    public static double getSinkingHullIntegrity(Craft craft) {
        double sinkingPercentage;
        double overallSinkPercent = craft.getType().getDoubleProperty(CraftType.OVERALL_SINK_PERCENT);

        if (overallSinkPercent == 0) {
            sinkingPercentage = craft.getType().getDoubleProperty(CraftType.SINK_PERCENT);
        } else {
            sinkingPercentage = overallSinkPercent;
        }
        return sinkingPercentage;
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

    private BossBar createBossBar(Craft craft) {
        return BossBar.bossBar(
                MiniMessage.miniMessage().deserialize("Hull Integrity: <integrity> | Fuel: <range> blocks", TagResolver.resolver(
                        parsed("integrity", String.valueOf(getHullIntegrity(craft))),
                        parsed("range", String.valueOf(craft.getCachedFuelRange())))
                ),
                1f,
                BossBar.Color.GREEN,
                BossBar.Overlay.NOTCHED_20
        );
    }

    private void updateBossBar(Craft craft, BossBar bossBar, int hullIntegrity, double sinkingHullIntegrity) {
        BossBar.Color colour = getBossBarColour(getHullIntegrityColor(hullIntegrity, sinkingHullIntegrity));
        bossBar.color(colour);
        if (colour == BossBar.Color.RED) {
            bossBar.addFlag(BossBar.Flag.DARKEN_SCREEN);
        }
        bossBar.progress(1 - (float) ((float) (100 - hullIntegrity) / (100 - sinkingHullIntegrity)));
        bossBar.name(getBossBarText(craft));
    }

    private Component getBossBarText(Craft craft) {
        return MiniMessage.miniMessage().deserialize("<integrity> <white>| <b>Fuel</b>: <aqua><range></aqua> blocks",
                TagResolver.resolver(
                        Placeholder.component("integrity", createHullIntegrityLine(craft)),
                        Placeholder.parsed("range", String.valueOf(craft.getCachedFuelRange()))
                )
        );
    }

    private BossBar.Color getBossBarColour(NamedTextColor hullIntegrityColour) {
        if (hullIntegrityColour == NamedTextColor.RED) {
            return BossBar.Color.RED;
        } else if (hullIntegrityColour == NamedTextColor.YELLOW) {
            return BossBar.Color.YELLOW;
        } else if (hullIntegrityColour == NamedTextColor.GREEN) {
            return BossBar.Color.GREEN;
        } else {
            return BossBar.Color.WHITE;
        }
    }
}
