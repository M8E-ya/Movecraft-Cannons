package net.tylers1066.movecraftcannons.aiming;

import at.pavlov.cannons.Aiming;
import at.pavlov.cannons.Cannons;
import at.pavlov.cannons.cannon.Cannon;
import at.pavlov.cannons.utils.CannonsUtil;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.object.Resident;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.PlayerCraft;
import net.tylers1066.movecraftcannons.MovecraftCannons;
import net.tylers1066.movecraftcannons.listener.DetectionListener;
import net.tylers1066.movecraftcannons.localisation.I18nSupport;
import net.tylers1066.movecraftcannons.utils.MovecraftUtils;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public class AimingCommand implements CommandExecutor {

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

        AimingUtils.aimCannonsOnCraft(craft, player);
        return true;
    }

}