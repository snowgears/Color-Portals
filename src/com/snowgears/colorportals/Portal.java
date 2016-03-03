package com.snowgears.colorportals;


import com.snowgears.colorportals.utils.BukkitUtils;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.material.Sign;

import java.util.*;


public class Portal implements Comparable<Portal> {

    private Location signLocation;
    private Location warpLocation;
    private Collection<Location> occupiedLocations;

    private UUID creator;
    private String name;
    private int channel;
    private int node;
    private DyeColor color;
    private Portal linkedPortal;

    public Portal(UUID creator, String name, DyeColor color, int channel, int node, Location signLocation) {
        this.creator = creator;
        this.name = name;
        this.color = color;
        this.channel = channel;
        this.node = node;
        this.signLocation = signLocation;

        defineLocations();
    }

    public void updateSign() {

        ColorPortals.getPlugin().getServer().getScheduler().scheduleSyncDelayedTask(ColorPortals.getPlugin(), new Runnable() {
            public void run() {

                if (signLocation.getBlock().getType() == Material.WALL_SIGN) {
                    org.bukkit.block.Sign sign = (org.bukkit.block.Sign) signLocation.getBlock().getState();
                    sign.setLine(0, name);
                    sign.setLine(1, channel + "." + node);

                    if (linkedPortal != null) {
                        sign.setLine(2, ChatColor.GREEN + "Warps To:");
                        sign.setLine(3, linkedPortal.getName());
                    } else {
                        sign.setLine(2, "");
                        sign.setLine(3, ChatColor.GRAY + "INACTIVE");
                    }
                    sign.update(true);
                }

            }
        }, 2L);
    }

    public void remove() {
        ColorPortals.getPlugin().getServer().getScheduler().scheduleSyncDelayedTask(ColorPortals.getPlugin(), new Runnable() {
            public void run() {
                //first update the sign (if the sign is still on the portal)
                if (signLocation.getBlock().getType() == Material.WALL_SIGN) {
                    org.bukkit.block.Sign sign = (org.bukkit.block.Sign) signLocation.getBlock().getState();
                    sign.setLine(0, ChatColor.RED + "PORTAL");
                    sign.setLine(1, ChatColor.RED + "DESTROYED");
                    sign.setLine(2, "");
                    sign.setLine(3, "");
                    sign.update(true);
                }
            }
        }, 2L);
        //then calculate which portals are before and after this portal
        ArrayList<Portal> portalFamily = ColorPortals.getPlugin().getPortalHandler().getPortalFamily(this);

        int beforeRemovedIndex = portalFamily.indexOf(this) - 1;
        if (beforeRemovedIndex < 0)
            beforeRemovedIndex = portalFamily.size() - 1;
        int afterRemovedIndex = portalFamily.indexOf(this) + 1;
        if (afterRemovedIndex >= portalFamily.size())
            afterRemovedIndex = 0;

        Portal beforeRemoved = portalFamily.get(beforeRemovedIndex);
        Portal afterRemoved = portalFamily.get(afterRemovedIndex);

        if (beforeRemovedIndex == afterRemovedIndex) {
            if (this.getLinkedPortal() != null) {
                this.getLinkedPortal().setLinkedPortal(null);
            }
            ColorPortals.getPlugin().getPortalHandler().deregisterPortal(this);
            return;
        }

        beforeRemoved.setLinkedPortal(afterRemoved);

        //then update the nodes of the affected portals in the chain
        for (int i = afterRemovedIndex; i < portalFamily.size(); i++) {
            portalFamily.get(i).setNode(i);
        }
        //finally remove the portal from the portal handler
        ColorPortals.getPlugin().getPortalHandler().deregisterPortal(this);
    }

    public void teleport() {
        if (this.linkedPortal == null)
            return;
        for (Entity e : warpLocation.getChunk().getEntities()) {
            if (e.getLocation().distanceSquared(this.getWarpLocation()) < 0.7) {
                if (ColorPortals.getPlugin().getEntityListener().entityCanBeTeleported(e)) {
                    ColorPortals.getPlugin().getEntityListener().addNoTeleportEntity(e);
                    e.teleport(this.linkedPortal.getWarpLocation());
                }
            }
        }
        ColorPortals.getPlugin().getServer().getScheduler().scheduleSyncDelayedTask(ColorPortals.getPlugin(), new Runnable() {
            public void run() {
                warpLocation.getWorld().playSound(warpLocation, Sound.ENTITY_ENDERMEN_TELEPORT, 1.0F, 0.5F);
            }
        }, 2L);
        linkedPortal.getWarpLocation().getWorld().playSound(linkedPortal.getWarpLocation(), Sound.ENTITY_ENDERMEN_TELEPORT, 1.0F, 0.5F);
    }

    public Collection<Location> getOccupiedLocations() {
        return occupiedLocations;
    }

    public Collection<Block> getOccupiedBlocks() {
        ArrayList<Block> occupiedBlocks = new ArrayList<Block>(occupiedLocations.size());
        for (Location loc : occupiedLocations) {
            occupiedBlocks.add(loc.getBlock());
        }
        return occupiedBlocks;
    }

    public void printInfo(Player player){
        player.sendMessage(ChatColor.GOLD + "Portal: " + this.getName());
        player.sendMessage(ChatColor.GRAY + "   - Color: " + this.getColor().toString() + ", Channel: " + this.getChannel());
        player.sendMessage(ChatColor.GRAY + "   - Node: " + this.getNode() + " out of " + ColorPortals.getPlugin().getPortalHandler().getPortalFamily(this).size());
        String creatorName = Bukkit.getOfflinePlayer(this.getCreator()).getName();
        player.sendMessage(ChatColor.GRAY + "   - Creator: " + creatorName);
        player.sendMessage(ChatColor.GREEN + "   - Warps To:");
        if (this.getLinkedPortal() == null) {
            player.sendMessage(ChatColor.GRAY + "      - No warp location set");
            return;
        }
        player.sendMessage(ChatColor.GRAY + "      - Name: " + this.getLinkedPortal().getName());
        //portals warp to the same world
        if (this.getWarpLocation().getWorld().toString().equals(this.getLinkedPortal().getWarpLocation().getWorld().toString())) {
            HashMap<BlockFace, Integer> cardinalDistances = BukkitUtils.getCardinalDistances(this.getWarpLocation(), this.getLinkedPortal().getWarpLocation());
            String cardinalMessage = "";
            for (BlockFace direction : cardinalDistances.keySet()) {
                cardinalMessage += direction.toString() + ": " + cardinalDistances.get(direction) + " blocks, ";
            }
            cardinalMessage = cardinalMessage.substring(0, cardinalMessage.length() - 2);
            player.sendMessage(ChatColor.GRAY + "      - " + cardinalMessage);
        } else {
            player.sendMessage(ChatColor.GRAY + "      - Location: " + this.getLinkedPortal().getWarpLocation().getWorld().toString() + " (a different world)");
        }
        player.sendMessage(ChatColor.GRAY + "      - Biome: " + this.getLinkedPortal().getWarpLocation().getWorld().getBiome(this.getLinkedPortal().getWarpLocation().getBlockX(), this.getLinkedPortal().getWarpLocation().getBlockZ()).toString());
    }

    public UUID getCreator() {
        return creator;
    }

    public Location getSignLocation() {
        return signLocation;
    }

    public Location getWarpLocation() {
        return warpLocation;
    }

    public String getName() {
        return name;
    }

    public DyeColor getColor() {
        return color;
    }

    public int getChannel() {
        return channel;
    }

    public int getNode() {
        return node;
    }

    public void setNode(int n) {
        node = n;
    }

    public Portal getLinkedPortal() {
        return linkedPortal;
    }

    public void setLinkedPortal(Portal p) {
        linkedPortal = p;
        if (linkedPortal != null)
            linkedPortal.updateSign();
        else
            this.node = 1;
        this.updateSign();
    }

    @Override
    public int compareTo(Portal other) {

        int i = other.color.compareTo(color);
        if (i != 0) return i;

        i = Integer.valueOf(channel).compareTo(Integer.valueOf(other.channel));
        if (i != 0) return i;

        return Integer.valueOf(node).compareTo(Integer.valueOf(other.node));
    }

    private void defineLocations() {
        Block signBlock = signLocation.getBlock();
        Sign sign = (Sign) signBlock.getState().getData();
        warpLocation = signBlock.getRelative(sign.getAttachedFace()).getRelative(BlockFace.DOWN).getRelative(BlockFace.DOWN).getLocation();
        warpLocation.add(0.5, 0, 0.5);
        warpLocation.setYaw(ColorPortals.getPlugin().getBukkitUtils().faceToYaw(sign.getFacing()) + 180F);


        BlockFace travel;
        if (sign.getFacing() == BlockFace.NORTH || sign.getFacing() == BlockFace.SOUTH)
            travel = BlockFace.EAST;
        else
            travel = BlockFace.NORTH;

        Block midTop = signBlock.getRelative(sign.getAttachedFace());
        Block midUpper = midTop.getRelative(BlockFace.DOWN);
        Block midLower = midTop.getRelative(BlockFace.DOWN).getRelative(BlockFace.DOWN);
        Block midBottom = midTop.getRelative(BlockFace.DOWN).getRelative(BlockFace.DOWN).getRelative(BlockFace.DOWN);

        Block leftTop = midTop.getRelative(travel);
        Block leftUpper = midUpper.getRelative(travel);
        Block leftLower = midLower.getRelative(travel);
        Block leftBottom = midBottom.getRelative(travel);

        Block rightTop = midTop.getRelative(travel.getOppositeFace());
        Block rightUpper = midUpper.getRelative(travel.getOppositeFace());
        Block rightLower = midLower.getRelative(travel.getOppositeFace());
        Block rightBottom = midBottom.getRelative(travel.getOppositeFace());

        occupiedLocations = new ArrayList<Location>();
        occupiedLocations.add(signLocation);
        occupiedLocations.add(midTop.getLocation());
        occupiedLocations.add(midUpper.getLocation());
        occupiedLocations.add(midLower.getLocation());
        occupiedLocations.add(midBottom.getLocation());
        occupiedLocations.add(leftTop.getLocation());
        occupiedLocations.add(leftUpper.getLocation());
        occupiedLocations.add(leftLower.getLocation());
        occupiedLocations.add(leftBottom.getLocation());
        occupiedLocations.add(rightTop.getLocation());
        occupiedLocations.add(rightUpper.getLocation());
        occupiedLocations.add(rightLower.getLocation());
        occupiedLocations.add(rightBottom.getLocation());
    }
}
