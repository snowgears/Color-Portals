package com.snowgears.colorportals;


import com.snowgears.colorportals.utils.BukkitUtils;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.UUID;

public class Portal implements Comparable<Portal> {

    private Location signLocation;
    private Location warpLocation;
    private Location cartWarpLocation;
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

                if (signLocation.getBlock().getBlockData() instanceof WallSign) {
                    Sign sign = (Sign) signLocation.getBlock().getState();
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
                if (signLocation.getBlock().getBlockData() instanceof WallSign) {
                    Sign sign = (Sign) signLocation.getBlock().getState();
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

    //this is called in place of teleport, but specifically for minecarts
    public void teleportMinecart(Entity cart){
        if (this.linkedPortal == null)
            return;
        if (!ColorPortals.getPlugin().getEntityListener().entityCanBeTeleported(cart, this))
            return;

        ArrayList<Entity> passengers = new ArrayList<>();
        if(!cart.getPassengers().isEmpty()){
            for(Entity passenger : cart.getPassengers()) {
                passenger.leaveVehicle();
                passengers.add(passenger);
            }
        }
        double velocityLength = cart.getVelocity().length();

        if(ColorPortals.getPlugin().getAllowMinecarts()) {
            teleportEntity(cart);

            ColorPortals.getPlugin().getServer().getScheduler().scheduleSyncDelayedTask(ColorPortals.getPlugin(), new Runnable() {
                public void run() {
                    if (cart != null && !cart.isDead()) {
                        Vector direction = getLinkedPortal().getWarpLocation().getDirection().clone();
                        direction.multiply(velocityLength);
                        cart.setVelocity(direction);
                    }
                }
            }, 2L);

            ColorPortals.getPlugin().getServer().getScheduler().scheduleSyncDelayedTask(ColorPortals.getPlugin(), new Runnable() {
                public void run() {
                    for (Entity passenger : passengers) {
                        if (passenger != null && !passenger.isDead()) {
                            passenger.teleport(getLinkedPortal().getWarpLocation());
                            cart.addPassenger(passenger); //this does not auto teleport the player
                        }
                    }
                    warpLocation.getWorld().playSound(warpLocation, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0F, 0.5F);
                }
            }, 4L);

            linkedPortal.getWarpLocation().getWorld().playSound(linkedPortal.getWarpLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0F, 0.5F);
        }
        //only allow passengers through (without minecart)
        else{
            for(Entity passenger : passengers) {
                teleportEntity(passenger);
            }
        }
    }

    public void teleport() {
        if (this.linkedPortal == null)
            return;
        for (Entity e : warpLocation.getChunk().getEntities()) {
            if (e.getLocation().distanceSquared(this.getWarpLocation()) < 0.7) {
                this.teleportEntity(e);
            }
        }
        ColorPortals.getPlugin().getServer().getScheduler().scheduleSyncDelayedTask(ColorPortals.getPlugin(), new Runnable() {
            public void run() {
                warpLocation.getWorld().playSound(warpLocation, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0F, 0.5F);
            }
        }, 2L);
        linkedPortal.getWarpLocation().getWorld().playSound(linkedPortal.getWarpLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0F, 0.5F);
    }

    private boolean teleportEntity(Entity entity){
        if (ColorPortals.getPlugin().getEntityListener().entityCanBeTeleported(entity, this)) {
            ColorPortals.getPlugin().getEntityListener().addNoTeleportEntity(entity);
            if(entity instanceof Minecart)
                entity.teleport(this.linkedPortal.getCartWarpLocation());
            else
                entity.teleport(this.linkedPortal.getWarpLocation());
            return true;
        }
        return false;
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

    public Location getCartWarpLocation() {
        return cartWarpLocation;
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
        if(!(signBlock.getBlockData() instanceof WallSign))
            return;

        WallSign sign = (WallSign) signBlock.getBlockData();
        warpLocation = signBlock.getRelative(sign.getFacing().getOppositeFace()).getRelative(BlockFace.DOWN).getRelative(BlockFace.DOWN).getLocation();
        warpLocation.add(0.5, 0, 0.5);
        warpLocation.setYaw(ColorPortals.getPlugin().getBukkitUtils().faceToYaw(sign.getFacing()) + 180F);

        cartWarpLocation = warpLocation.clone();
        switch (sign.getFacing()){
            case NORTH:
                cartWarpLocation.setZ(cartWarpLocation.getZ()-0.5);
                break;
            case EAST:
                cartWarpLocation.setX(cartWarpLocation.getX()+0.5);
                break;
            case SOUTH:
                cartWarpLocation.setZ(cartWarpLocation.getZ()+0.5);
                break;
            case WEST:
                cartWarpLocation.setX(cartWarpLocation.getX()-0.5);
                break;
        }

        BlockFace travel;
        if (sign.getFacing() == BlockFace.NORTH || sign.getFacing() == BlockFace.SOUTH)
            travel = BlockFace.EAST;
        else
            travel = BlockFace.NORTH;

        Block midTop = signBlock.getRelative(sign.getFacing().getOppositeFace());
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
