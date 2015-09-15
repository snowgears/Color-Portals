package com.snowgears.colorportals.listeners;

import com.snowgears.colorportals.ColorPortals;
import com.snowgears.colorportals.Portal;
import com.snowgears.colorportals.events.CreatePortalEvent;
import com.snowgears.colorportals.events.DestroyPortalEvent;
import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.material.Sign;

import java.util.ArrayList;


public class PortalListener implements Listener {


    public ColorPortals plugin = ColorPortals.getPlugin();

    public PortalListener(ColorPortals instance) {
        plugin = instance;
    }

    @EventHandler
    public void onPortalCreate(CreatePortalEvent event) {
        Portal portal = event.getPortal();
        plugin.getPortalHandler().registerPortal(portal);
        event.getPlayer().sendMessage(ChatColor.GRAY + "You have added a " + portal.getColor().toString().toLowerCase() + " portal on channel " + portal.getChannel());
        plugin.getPortalHandler().savePortals();
    }

    @EventHandler
    public void onPortalDestroy(DestroyPortalEvent event) {
        event.getPortal().remove();
        plugin.getPortalHandler().savePortals();
    }

    @EventHandler
    public void signDetachCheck(BlockPhysicsEvent event) {
        Block b = event.getBlock();
        if (b.getType() == Material.WALL_SIGN) {
            Portal portal = plugin.getPortalHandler().getPortal(b.getLocation());
            if (portal != null) {
                BlockFace face = ((Sign) b.getState().getData()).getAttachedFace();
                if (!event.getBlock().getRelative(face).getType().isSolid()) {
                    if (plugin.getPortalProtection()) {
                        event.setCancelled(true);
                    } else {
                        DestroyPortalEvent e = new DestroyPortalEvent(portal, null);
                        plugin.getServer().getPluginManager().callEvent(e);
                    }
                }
            }
        }
    }

    public boolean portalCanBeCreated(Integer channel, DyeColor color) {
        return !(plugin.getMaxPortalsPerGroup() != 0 && plugin.getPortalHandler().getPortalFamily(channel, color).size() >= plugin.getMaxPortalsPerGroup());
    }

    public boolean frameIsComplete(Location signLocation) {

        Sign sign = (Sign) signLocation.getBlock().getState().getData();
        Block keyBlock = signLocation.getBlock().getRelative(sign.getAttachedFace());
        DyeColor color = plugin.getBukkitUtils().getWoolColor(keyBlock);
        if (color == null)
            return false;

        BlockFace travel;
        if (sign.getFacing() == BlockFace.NORTH || sign.getFacing() == BlockFace.SOUTH)
            travel = BlockFace.EAST;
        else
            travel = BlockFace.NORTH;

        Block topLeft = keyBlock.getRelative(travel);
        Block topRight = keyBlock.getRelative(travel.getOppositeFace());
        Block leftMid = topLeft.getRelative(BlockFace.DOWN);
        Block rightMid = topRight.getRelative(BlockFace.DOWN);
        Block lowerLeft = leftMid.getRelative(BlockFace.DOWN);
        Block lowerRight = rightMid.getRelative(BlockFace.DOWN);
        Block bottomLeft = lowerLeft.getRelative(BlockFace.DOWN);
        Block bottomRight = lowerRight.getRelative(BlockFace.DOWN);
        Block bottomMid = keyBlock.getRelative(BlockFace.DOWN).getRelative(BlockFace.DOWN).getRelative(BlockFace.DOWN);

        Block[] frameBlocks = {keyBlock, topLeft, topRight, leftMid, rightMid, lowerLeft, lowerRight, bottomLeft, bottomRight, bottomMid};

        for (Block block : frameBlocks) {
            DyeColor blockColor = plugin.getBukkitUtils().getWoolColor(block);
            if (blockColor != color)
                return false;
        }

        Block buttonBlock = keyBlock.getRelative(BlockFace.DOWN);
        if (!(buttonBlock.getType() == Material.STONE_BUTTON || buttonBlock.getType() == Material.WOOD_BUTTON))
            return false;
        Block plateBlock = bottomMid.getRelative(BlockFace.UP);
        return plateBlock.getType() == Material.STONE_PLATE || plateBlock.getType() == Material.WOOD_PLATE || plateBlock.getType() == Material.IRON_PLATE || plateBlock.getType() == Material.GOLD_PLATE;

    }

    public boolean checkPortalDistance(Location currentLoc, Player player, int channel, DyeColor color) {
        if (plugin.getMinDistance() != 0 || plugin.getMaxDistance() != 0) {
            if (plugin.getUsePerms() && !player.hasPermission("colorportals.nodistance")) {
                ArrayList<Portal> portals = plugin.getPortalHandler().getPortalFamily(channel, color);
                if (portals.size() != 0) {
                    Portal toConnect = portals.get(portals.size() - 1);
                    Location connectLoc = toConnect.getSignLocation();
                    int distance;
                    //portals are in the same world
                    if (connectLoc.getWorld().equals(currentLoc.getWorld()))
                        distance = (int) currentLoc.distance(connectLoc);
                    else
                        distance = -1;

                    if (plugin.getMinDistance() != 0) {
                        if (distance != -1 && distance < plugin.getMinDistance()) {
                            player.sendMessage(ChatColor.RED + "This portal is " + ChatColor.WHITE + (plugin.getMinDistance() - distance) + ChatColor.RED + " blocks too close to the previous portal in the chain.");
                            return false;
                        }
                    }
                    if (plugin.getMaxDistance() != 0) {
                        if (distance == -1) {
                            player.sendMessage(ChatColor.RED + "This portal cannot be created because there is a maximum distance of " + ChatColor.WHITE + plugin.getMaxDistance() + ChatColor.RED + " blocks and this portal is a different world than the previous portal in the chain.");
                            return false;
                        } else if (distance > plugin.getMaxDistance()) {
                            player.sendMessage(ChatColor.RED + "This portal is " + ChatColor.WHITE + (distance - plugin.getMaxDistance()) + ChatColor.RED + " blocks too far away from the previous portal in the chain.");
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }
}
