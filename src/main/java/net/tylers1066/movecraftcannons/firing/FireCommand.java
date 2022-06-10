package net.tylers1066.movecraftcannons.firing;

import at.pavlov.cannons.Cannons;
import at.pavlov.cannons.cannon.Cannon;
import at.pavlov.cannons.cannon.CannonDesign;
import com.palmergames.bukkit.towny.TownyAPI;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.craft.PlayerCraft;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.tylers1066.movecraftcannons.MovecraftCannons;
import net.tylers1066.movecraftcannons.listener.DetectionListener;
import net.tylers1066.movecraftcannons.localisation.I18nSupport;
import net.tylers1066.movecraftcannons.utils.MovecraftUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class FireCommand implements TabExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use this command.", NamedTextColor.RED));
            return true;
        }

        Craft craft = MovecraftUtils.getCurrentShip(player);
        if (!(craft instanceof PlayerCraft pcraft) || !(MovecraftUtils.isFriendly(TownyAPI.getInstance().getResident(player), pcraft))) {
            player.sendMessage(Component.text(I18nSupport.getInternationalisedString("Unfriendly craft"), TextColor.color(0xffb2ab)));
            return true;
        }

        Set<Cannon> cannonsOnCraft = DetectionListener.getCannonsOnCraft(craft);
        if (cannonsOnCraft.isEmpty() || !craft.getType().getBoolProperty(MovecraftCannons.CAN_USE_CANNONS)) {
            player.sendMessage(Component.text("There are no cannons on your craft to fire.", NamedTextColor.RED));
            return true;
        }

        if (args.length == 1) {
            CannonDesign selectedDesign = Cannons.getPlugin().getCannonDesign(args[0]);
            if (selectedDesign == null) {
                player.sendMessage(Component.text(args[0] + " is not a valid cannon type.", NamedTextColor.RED));
                return true;
            }

            cannonsOnCraft.removeIf(cannon -> !cannon.getCannonDesign().getDesignName().equalsIgnoreCase(args[0]));
        }

        FiringUtils.fireCannons(player, cannonsOnCraft, true);
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        List<String> cannonsOnCraft = MovecraftUtils.getCannonTypesOnPlayerCurrentCraft((Player) sender);

        if (args.length == 1) {
            return StringUtil.copyPartialMatches(args[0], cannonsOnCraft, new ArrayList<>());
        }
        return cannonsOnCraft;
    }
}
