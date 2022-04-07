package net.tylers1066.movecraftcannons.homingprojectiles;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Resident;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.craft.PlayerCraft;
import net.countercraft.movecraft.craft.SinkingCraft;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.tylers1066.movecraftcannons.utils.MovecraftUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class LockOnCommand implements TabExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players may use this command.", NamedTextColor.RED));
            return true;
        }

        Craft craft = CraftManager.getInstance().getCraftByPlayer(player);
        if (craft == null) {
            sender.sendMessage(Component.text("You must be piloting a craft to use this command.", NamedTextColor.RED));
            return true;
        }

        Resident resident = TownyAPI.getInstance().getResident(player.getUniqueId());
        if (resident == null) {
            sender.sendMessage(Component.text("Error - your resident was null.", NamedTextColor.RED));
            return true;
        }

        Craft lockonCraft;
        Player lockonPlayer = null;

        // No target specified - try to find one
        if (args.length == 0) {
            if (craft.getContacts().isEmpty()) {
                sender.sendMessage(Component.text("There are no crafts nearby to target lock.", NamedTextColor.RED));
                return true;
            }

            lockonCraft = MovecraftUtils.getNearestCraftToCraft(craft, craft.getContacts());
            if (lockonCraft instanceof SinkingCraft) {
                sender.sendMessage(Component.text("There are no crafts nearby to target lock.", NamedTextColor.RED));
                return true;
            }

            if (lockonCraft instanceof PlayerCraft playerCraft && MovecraftUtils.isFriendly(resident, playerCraft)) {
                sender.sendMessage(Component.text("The closest craft is friendly! Cannot target lock.", NamedTextColor.RED));
                return true;
            }
        }

        else if (args[0].equalsIgnoreCase("off")) {
            sender.sendMessage(Component.text("You are no longer locked on to a craft.", TextColor.color(0xc3f09e)));
            sender.playSound(Sound.sound(Key.key("radar_lock_acquired"), Sound.Source.MASTER, 3f, 1f), Sound.Emitter.self());
            HomingProjectileManager.getPlayerHomingTargetMap().remove(player.getUniqueId());
            return true;
        }

        else {
            lockonPlayer = Bukkit.getPlayer(args[0]);
            if (lockonPlayer == null) {
                sender.sendMessage(Component.text("Invalid lock-on target.", NamedTextColor.RED));
                return true;
            }

            lockonCraft = CraftManager.getInstance().getCraftByPlayer(lockonPlayer);
            if (lockonCraft == null || !craft.getContacts().contains(lockonCraft) || lockonCraft instanceof SinkingCraft) {
                sender.sendMessage(Component.text("Invalid lock-on target.", NamedTextColor.RED));
                return true;
            }
        }

        HomingProjectileManager.getPlayerHomingTargetMap().put(player.getUniqueId(), lockonCraft);

        TextComponent.Builder message = Component.text().content("Target lock acquired.");
        if (lockonPlayer != null) {
            lockonPlayer.playSound(Sound.sound(Key.key("radar_lock_warning"), Sound.Source.MASTER, 3f, 1f), Sound.Emitter.self());
            lockonPlayer.sendMessage(Component.text("You are being target-locked!", TextColor.color(0xd12634), TextDecoration.BOLD));
            message.append(Component.text(" Target: " + lockonPlayer.getName()));
        }

        message.decorate(TextDecoration.BOLD);
        message.color(TextColor.color(0xc3f09e));
        sender.sendMessage(message.build());
        sender.playSound(Sound.sound(Key.key("radar_lock_acquired"), Sound.Source.MASTER, 3f, 1f), Sound.Emitter.self());

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            return Collections.emptyList();
        }

        Resident resident = TownyAPI.getInstance().getResident(player.getUniqueId());
        if (resident == null) {
            return Collections.emptyList();
        }

        Craft craft = CraftManager.getInstance().getCraftByPlayer(player);
        if (craft == null) {
            return Collections.emptyList();
        }

        List<String> tabCompletions = craft.getContacts().stream()
                .filter(contact -> contact instanceof PlayerCraft)
                .filter(contact -> !MovecraftUtils.isFriendly(resident, (PlayerCraft) contact))
                .map(contact -> ((PlayerCraft) contact).getPilot().getName())
                .collect(Collectors.toList());
        tabCompletions.add("off");

        return tabCompletions;
    }
}
