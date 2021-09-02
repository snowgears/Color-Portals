package com.snowgears.colorportals.listeners;

import com.google.common.base.Predicates;
import com.snowgears.colorportals.ColorPortals;
import com.snowgears.colorportals.Portal;
import com.snowgears.colorportals.events.CreatePortalEvent;
import com.snowgears.colorportals.events.DestroyPortalEvent;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityInteractEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.function.Predicate;

public class EntityListener implements Listener {

    public ColorPortals plugin = ColorPortals.getPlugin();
    private HashMap<UUID, Boolean> noTeleportEntities = new HashMap<UUID, Boolean>();

    public EntityListener(ColorPortals instance) {
        plugin = instance;
    }

    @EventHandler
    public void onSignWrite(SignChangeEvent event) {
        if (event.getBlock().getBlockData() instanceof WallSign) {
            WallSign s = (WallSign) event.getBlock().getBlockData();
            BlockFace attachedFace = s.getFacing().getOppositeFace();
            Block attachedBlock = event.getBlock().getRelative(attachedFace);
            if (plugin.getBukkitUtils().isWool(attachedBlock.getType()) && plugin.getBukkitUtils().isWool(attachedBlock.getLocation().clone().add(0, -3, 0).getBlock().getType())) {
                if (!plugin.getPortalListener().frameIsComplete(event.getBlock().getLocation())) {
                    event.getPlayer().sendMessage(ChatColor.RED + "Your portal's frame is either not complete or it is missing the button and/or pressure plate.");
                    return;
                }
                DyeColor color = plugin.getBukkitUtils().getWoolColor(attachedBlock);
                String colorString = color.toString().toLowerCase();
                if (plugin.getUsePerms() && !(event.getPlayer().hasPermission("colorportals.create.*") || event.getPlayer().hasPermission("colorportals.create."+colorString))) {
                    event.getPlayer().sendMessage(ChatColor.DARK_RED + "You are not authorized to create "+colorString+" portals");
                    event.setCancelled(true);
                    return;
                }
                int maxBuildNum = plugin.getPortalHandler().getMaxPortalsPlayerCanBuild(event.getPlayer());
                if(maxBuildNum != -1) {
                    int portalsLeft = maxBuildNum - plugin.getPortalHandler().getPortalsCreated(event.getPlayer().getUniqueId());
                    if (portalsLeft < 1) {
                        event.getPlayer().sendMessage(ChatColor.DARK_RED + "You have reached your limit of "+maxBuildNum+" portals.");
                        event.setCancelled(true);
                        return;
                    }
                }

                Portal p = plugin.getPortalHandler().getPortal(attachedBlock.getRelative(attachedFace).getLocation());
                if (p != null) {
                    event.getPlayer().sendMessage(ChatColor.RED + "You can not put another sign on the top of this portal.");
                    event.setCancelled(true);
                    ItemStack dropSign = new ItemStack(event.getBlock().getType());
                    event.getBlock().setType(Material.AIR);
                    event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), dropSign);
                }

                String name = event.getLine(0);
                if (name.length() == 0) {
                    event.getPlayer().sendMessage(ChatColor.RED + "The name of the portal cannot be left blank");
                    return;
                }
                String chan = event.getLine(1);
                int channel;
                if (plugin.getBukkitUtils().isInteger(chan)) {
                    channel = Integer.parseInt(chan);
                } else {
                    event.getPlayer().sendMessage(ChatColor.RED + "The channel must be an integer in order to create a portal");
                    return;
                }
                if (channel < 0 || channel > 9999) {
                    event.getPlayer().sendMessage(ChatColor.RED + "The channel must be between 0 and 10,000.");
                    return;
                }

                if (!plugin.getPortalListener().checkPortalDistance(attachedBlock.getLocation(), event.getPlayer(), channel, color)) {
                    event.setCancelled(true);
                    return;
                }

                if (plugin.getPortalListener().portalCanBeCreated(channel, color)) {
                    int node = plugin.getPortalHandler().getPortalFamily(channel, color).size() + 1;
                    Portal portal = new Portal(event.getPlayer().getUniqueId(), name, color, channel, node, event.getBlock().getLocation());

                    CreatePortalEvent e = new CreatePortalEvent(portal, event.getPlayer());
                    Bukkit.getServer().getPluginManager().callEvent(e);

                } else {
                    event.getPlayer().sendMessage(ChatColor.RED + "There are already " + plugin.getMaxPortalsPerGroup() + " " + color + " portals on channel " + channel + ".");
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void onPlayerBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Portal portal = null;

        Block block = event.getBlock();
        if (block.getBlockData() instanceof WallSign) {
            portal = plugin.getPortalHandler().getPortal(event.getBlock().getLocation());
        } else if (plugin.getBukkitUtils().isWool(block.getType())) {
            portal = plugin.getPortalHandler().getPortalByFrameLocation(event.getBlock().getLocation());
        }
        //dont break portals if button or plate is broken
        //else if (Tag.BUTTONS.isTagged(block.getType()) || Tag.PRESSURE_PLATES.isTagged(block.getType())) {
        //    portal = plugin.getPortalHandler().getPortalByFrameLocation(event.getBlock().getLocation());
        //}

        if (portal != null) {
            String colorString = portal.getColor().toString().toLowerCase();
            if (plugin.getUsePerms() && !(player.hasPermission("colorportals.destroy.*") || player.hasPermission("colorportals.destroy."+colorString))) {
                player.sendMessage(ChatColor.DARK_RED + "You are not authorized to destroy "+colorString+" portals");
                event.setCancelled(true);
                return;
            }
            //player is authorized to destroy portals
            if (plugin.getPortalProtection()) {
                //if portal creator equals the player who broke the frame (or player is OP)
                if (portal.getCreator().equals(player.getUniqueId()) || player.isOp()) {
                    DestroyPortalEvent e = new DestroyPortalEvent(portal, event.getPlayer());
                    Bukkit.getServer().getPluginManager().callEvent(e);
                } else {
                    player.sendMessage(ChatColor.DARK_RED + "You are not authorized to destroy this portal");
                    event.setCancelled(true);
                }
            }
            //not using portal protection
            else {
                DestroyPortalEvent e = new DestroyPortalEvent(portal, event.getPlayer());
                Bukkit.getServer().getPluginManager().callEvent(e);
            }
        }

    }

    @EventHandler
    public void onPlayerButtonPress(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK)
            return;
        if (!Tag.BUTTONS.isTagged(event.getClickedBlock().getType()))
            return;

        Block portalKeyBlock = event.getClickedBlock().getRelative(BlockFace.UP);
        Portal portal = plugin.getPortalHandler().getPortalByKeyBlock(portalKeyBlock);
        if (portal == null)
            return;
        if (portal.getLinkedPortal() == null)
            return;
        String colorString = portal.getColor().toString().toLowerCase();
        if (plugin.getUsePerms() && !(event.getPlayer().hasPermission("colorportals.use.*") || event.getPlayer().hasPermission("colorportals.use."+colorString))) {
            player.sendMessage(ChatColor.DARK_RED + "You are not authorized to use "+colorString+" portals");
        }
        else{
            portal.teleport();
        }
    }

    @EventHandler
    public void onPlayerSignClick(PlayerInteractEvent event){
        if(event.getAction() == Action.RIGHT_CLICK_BLOCK){
            Portal portal = plugin.getPortalHandler().getPortal(event.getClickedBlock().getLocation());
            if(portal != null){
                portal.printInfo(event.getPlayer());
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPlayerActivatePressurePlate(PlayerInteractEvent event) {
        if (!plugin.getWalkOnActivation())
            return;
        Player player = event.getPlayer();
        if (event.getAction() != Action.PHYSICAL)
            return;

        Block portalKeyBlock = event.getClickedBlock().getRelative(BlockFace.UP).getRelative(BlockFace.UP);
        Portal portal = plugin.getPortalHandler().getPortalByKeyBlock(portalKeyBlock);
        if (portal == null)
            return;
        if (portal.getLinkedPortal() == null)
            return;

        String colorString = portal.getColor().toString().toLowerCase();
        if (plugin.getUsePerms() && !(event.getPlayer().hasPermission("colorportals.use.*") || event.getPlayer().hasPermission("colorportals.use."+colorString))) {
            player.sendMessage(ChatColor.DARK_RED + "You are not authorized to use "+colorString+" portals");
        }
        else{
            portal.teleport();
        }
    }

    @EventHandler
    public void onEntityActivatePressurePlate(EntityInteractEvent event) {
        if (!plugin.getWalkOnActivation())
            return;

        Block portalKeyBlock = event.getBlock().getRelative(BlockFace.UP).getRelative(BlockFace.UP);
        Portal portal = plugin.getPortalHandler().getPortalByKeyBlock(portalKeyBlock);
        if (portal == null)
            return;
        if (portal.getLinkedPortal() == null)
            return;

        portal.teleport();
    }

    @EventHandler
    public void onRedstoneChange(BlockRedstoneEvent event) {
        if (!plugin.getWalkOnActivation())
            return;

        Block b = event.getBlock();
        if(b.getType() == Material.DETECTOR_RAIL)
        {
            //only fire the event if going ON to a detector, not coming OFF one
            if(event.getOldCurrent() != 0)
                return;

            //Check if rail is part of a portal
            Block portalKeyBlock = b.getRelative(BlockFace.UP).getRelative(BlockFace.UP);
            Portal portal = plugin.getPortalHandler().getPortalByKeyBlock(portalKeyBlock);
            if (portal == null)
                return;
            if (portal.getLinkedPortal() == null)
                return;

            //Getting nearby minecarts
            World w = b.getWorld();
            Predicate minecartFilter = Predicates.instanceOf(Minecart.class);
            Collection<Entity> minecarts = w.getNearbyEntities(b.getLocation(),1.5,1.5,1.5,minecartFilter);

            if(minecarts.size() == 0)
            {
                //If no minecraft were found, ignore.
                return;
            }

            Minecart cart = (Minecart) minecarts.iterator().next();
            portal.teleportMinecart(cart);
        }
    }

    @EventHandler
    public void onExplosion(EntityExplodeEvent event) {
        final ArrayList<Block> blocksToDestroy = new ArrayList<Block>(50);

        //save all potential portal blocks (for sake of time during explosion)
        Iterator<Block> blockIterator = event.blockList().iterator();
        while (blockIterator.hasNext()) {

            Block block = blockIterator.next();
            Portal portal = null;
            if(block.getBlockData() instanceof WallSign){
                portal = plugin.getPortalHandler().getPortal(block.getLocation());
            }
            else if (plugin.getBukkitUtils().isWool(block.getType()) || Tag.BUTTONS.isTagged(block.getType()) || Tag.PRESSURE_PLATES.isTagged(block.getType())) {
                portal = plugin.getPortalHandler().getPortalByFrameLocation(block.getLocation());
            }

            if (portal != null) {
                if (plugin.getPortalProtection()) {
                    blockIterator.remove();
                } else {
                    portal.remove();
                }
            }
        }
    }

    @EventHandler
    public void onArrowHit(EntityInteractEvent event) {
        if (!(event.getEntity() instanceof Arrow))
            return;
        if (!Tag.WOODEN_BUTTONS.isTagged(event.getBlock().getType()))
            return;
        Arrow shot = (Arrow) event.getEntity();
        if (!(shot.getShooter() instanceof Player))
            return;
        Player player = (Player) shot.getShooter();

        Block portalKeyBlock = event.getBlock().getRelative(BlockFace.UP);
        Portal portal = plugin.getPortalHandler().getPortalByKeyBlock(portalKeyBlock);
        if (portal == null)
            return;
        if (portal.getLinkedPortal() == null)
            return;

        String colorString = portal.getColor().toString().toLowerCase();
        if (plugin.getUsePerms() && !(player.hasPermission("colorportals.use.*") || player.hasPermission("colorportals.use."+colorString))) {
            player.sendMessage(ChatColor.DARK_RED + "You are not authorized to use "+colorString+" portals");
        }
        else{
            portal.teleport();
        }
    }

    public void addNoTeleportEntity(final Entity entity) {
        noTeleportEntities.put(entity.getUniqueId(), true);
        plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
            public void run() {
                noTeleportEntities.remove(entity.getUniqueId());
            }
        }, 5L);
    }

    public boolean entityCanBeTeleported(Entity entity, Portal fromPortal) {
        if(noTeleportEntities.get(entity.getUniqueId()) != null)
            return false;

        if(plugin.getAllowMobs() == false && entity instanceof Mob)
            return false;

        if(plugin.getAllowItems() == false && entity instanceof Item)
            return false;

        if(entity instanceof Player){
            Player player = (Player)entity;
            String colorString = fromPortal.getColor().toString().toLowerCase();
            if (plugin.getUsePerms() && !(player.hasPermission("colorportals.use.*") ||player.hasPermission("colorportals.use."+colorString))) {
                player.sendMessage(ChatColor.DARK_RED + "You are not authorized to use "+colorString+" portals");
                return false;
            }
        }

        return true;
    }
}
