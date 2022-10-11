package net.tylers1066.movecraftcannons.firing;

import at.pavlov.cannons.Cannons;
import at.pavlov.cannons.cannon.Cannon;
import at.pavlov.cannons.cannon.CannonDesign;
import com.palmergames.bukkit.towny.TownyAPI;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.PlayerCraft;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.tylers1066.movecraftcannons.MovecraftCannons;
import net.tylers1066.movecraftcannons.listener.DetectionListener;
import net.tylers1066.movecraftcannons.localisation.I18nSupport;
import net.tylers1066.movecraftcannons.utils.MovecraftUtils;
import org.bukkit.Bukkit;
import org.bukkit.Tag;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.Rotatable;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.Set;

public class FireSign implements Listener {

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || !Tag.SIGNS.isTagged(event.getClickedBlock().getType())) {
            return;
        }

        BlockState state = event.getClickedBlock().getState(false);
        if (!(state instanceof Sign sign)) {
            return;
        }

        if (!PlainTextComponentSerializer.plainText().serialize(sign.lines().get(0)).equalsIgnoreCase("Fire")) {
            return;
        }

        Player player = event.getPlayer();
        Craft craft = MovecraftUtils.getCurrentShip(player);
        if (!(craft instanceof PlayerCraft pcraft) || !craft.getType().getBoolProperty(MovecraftCannons.CAN_USE_CANNONS) ||  !(MovecraftUtils.isFriendly(TownyAPI.getInstance().getResident(player), pcraft))) {
            player.sendMessage(Component.text(I18nSupport.getInternationalisedString("Unfriendly craft"), TextColor.color(0xffb2ab)));
            return;
        }

        Set<Cannon> cannonsOnCraft = DetectionListener.getCannonsOnCraft(craft);

        String givenCannonName = PlainTextComponentSerializer.plainText().serialize(sign.lines().get(1));
        String selectedCannonType = null;
        for (CannonDesign design: Cannons.getPlugin().getDesignStorage().getCannonDesignList()) {
            String designName = design.getMessageName();
            if (designName.equalsIgnoreCase(givenCannonName)) {
                selectedCannonType = designName;
                break;
            }
        }

        if (selectedCannonType != null) {
            String finalSelectedCannonType = selectedCannonType;
            cannonsOnCraft.removeIf(cannon -> !cannon.getCannonDesign().getMessageName().equals(finalSelectedCannonType));
        }

        // We will only fire cannons whose direction matches the direction opposite to the sign's text-facing side.
        BlockFace facing;
        BlockData signData = sign.getBlockData();
        if (signData instanceof Rotatable rotatable) {
            facing = rotatable.getRotation().getOppositeFace();
        }
        else if (signData instanceof Directional directional) {
            facing = directional.getFacing().getOppositeFace();
        }
        else {
            return;
        }

        FiringUtils.fireCannons(player, cannonsOnCraft, facing);
    }
}
