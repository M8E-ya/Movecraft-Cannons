package net.tylers1066.movecraftcannons.homingprojectiles;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Resident;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.craft.PilotedCraft;
import net.countercraft.movecraft.craft.PlayerCraft;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
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
import java.util.Set;
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
            if (lockonCraft instanceof PlayerCraft playerCraft && MovecraftUtils.isFriendly(resident, playerCraft)) {
                sender.sendMessage(Component.text("The closest craft is friendly! Cannot target lock.", NamedTextColor.RED));
                return true;
            }
        }

        else {
            lockonPlayer = Bukkit.getPlayer(args[0]);
            if (lockonPlayer == null) {
                sender.sendMessage(Component.text("Invalid lock-on target.", NamedTextColor.RED));
                return true;
            }

            lockonCraft = CraftManager.getInstance().getCraftByPlayer(lockonPlayer);
            if (lockonCraft == null || !craft.getContacts().contains(lockonCraft)) {
                sender.sendMessage(Component.text("Invalid lock-on target.", NamedTextColor.RED));
                return true;
            }
        }

        HomingProjectileManager.getPlayerHomingTargetMap().put(player.getUniqueId(), lockonCraft);

        sender.sendMessage(Component.text("Target lock acquired.", TextColor.color(0xc3f09e), TextDecoration.BOLD));
        sender.playSound(Sound.sound(Key.key("radar_lock_acquired"), Sound.Source.MASTER, 3f, 1f), Sound.Emitter.self());
        if (lockonPlayer != null) {
            lockonPlayer.playSound(Sound.sound(Key.key("radar_lock_warning"), Sound.Source.MASTER, 3f, 1f), Sound.Emitter.self());
        }
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

        Set<Craft> contacts = craft.getContacts();
        if (contacts.isEmpty()) {
            return Collections.emptyList();
        }

        return contacts.stream()
                .filter(contact -> contact instanceof PlayerCraft)
                .filter(contact -> !MovecraftUtils.isFriendly(resident, (PlayerCraft) contact))
                .map(contact -> ((PlayerCraft) contact).getPilot().getName())
                .collect(Collectors.toList());
    }
}
