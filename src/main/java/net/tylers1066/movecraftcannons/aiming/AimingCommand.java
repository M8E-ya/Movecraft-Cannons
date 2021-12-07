package net.tylers1066.movecraftcannons.aiming;

import at.pavlov.cannons.Cannons;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.object.Resident;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.PlayerCraft;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.tylers1066.movecraftcannons.localisation.I18nSupport;
import net.tylers1066.movecraftcannons.utils.MovecraftUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AimingCommand implements TabExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            return false;
        }

        Player player = (Player) sender;
        Craft craft = MovecraftUtils.getCurrentShip(player);
        if (craft == null) {
            player.sendMessage(I18nSupport.getInternationalisedString("Not on ship"));
            return false;
        }

        if (!(craft instanceof PlayerCraft)) {
            player.sendMessage(I18nSupport.getInternationalisedString("Not on ship"));
            return false;
        }

        Resident resident = TownyUniverse.getInstance().getResident(player.getUniqueId());
        if (resident == null) {
            return false;
        }

        if (!MovecraftUtils.isFriendly(resident, (PlayerCraft) craft)) {
            player.sendMessage(I18nSupport.getInternationalisedString("Unfriendly craft"));
            return false;
        }

        String cannonType = null;
        if (args.length == 1) {
            cannonType = args[0];
            if (Cannons.getPlugin().getDesignStorage().getDesignIds().contains(args[0])) {
                player.sendMessage(Component.text(String.format(I18nSupport.getInternationalisedString("Selected cannon type"), cannonType), TextColor.color(0xc3f09e)));
                AimingUtils.getPlayerCannonSelections().put(player.getUniqueId(), cannonType);
            }
            else if (cannonType.equalsIgnoreCase("all")) {
                AimingUtils.getPlayerCannonSelections().remove(player.getUniqueId());
                player.sendMessage(Component.text(I18nSupport.getInternationalisedString("Deselected cannon type"), TextColor.color(0xc3f09e)));
            }
            else {
                player.sendMessage(Component.text(String.format(I18nSupport.getInternationalisedString("Invalid cannon type"), cannonType), TextColor.color(0xc3f09e)));
                return true;
            }
        }

        AimingUtils.aimCannonsOnCraft(craft, player, cannonType);
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        if (args.length == 1) {
            ArrayList<String> tabCompletions = new ArrayList<>();
            tabCompletions.add("all");
            tabCompletions.addAll(Cannons.getPlugin().getDesignStorage().getDesignIds());
            return tabCompletions;
        }
        return Collections.emptyList();
    }
}