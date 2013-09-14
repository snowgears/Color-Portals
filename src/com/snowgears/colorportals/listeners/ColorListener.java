package com.snowgears.colorportals.listeners;

import java.util.ArrayList;
import java.util.Collections;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityInteractEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.Sign;

import com.snowgears.colorportals.ColorPortals;
import com.snowgears.colorportals.Portal;
import com.snowgears.colorportals.events.CreatePortalEvent;
import com.snowgears.colorportals.events.DestroyPortalEvent;
import com.snowgears.colorportals.utils.PortalNodeComparator;
import com.snowgears.colorportals.utils.SerializableLocation;


public class ColorListener implements Listener{
	
	
	public ColorPortals plugin = ColorPortals.plugin;
	//public HashMap<Integer,Location> portalMap = new HashMap<Integer,Location>(); //channel, location
	public ArrayList<Portal> allPortals = new ArrayList<Portal>();
	
	public ColorListener(ColorPortals instance)
    {
        plugin = ColorPortals.plugin;
        allPortals = new ArrayList<Portal>();
        plugin = instance;
    }

    public ArrayList<Portal> getAllPortals()
    {
        if(ColorPortals.portalMap.containsKey("load"))
            return ColorPortals.portalMap.get("load");
        else
            return new ArrayList<Portal>();
    }

    @EventHandler
    public void onSignWrite(SignChangeEvent event)
    {
        if(event.getBlock().getType() == Material.WALL_SIGN)
        {
            Sign s = (Sign)event.getBlock().getState().getData();
            Block attachedBlock = event.getBlock().getRelative(s.getAttachedFace());
            if(attachedBlock.getType() == Material.WOOL && attachedBlock.getLocation().add(0, -3, 0).getBlock().getType() == Material.WOOL)
            {
                if(ColorPortals.usePerms && !event.getPlayer().hasPermission("colorportals.create"))
                {
                    event.getPlayer().sendMessage(ChatColor.DARK_RED+"You are not authorized to create portals");
                    event.setCancelled(true);
                    return;
                }
                for(int i = 0; i < allPortals.size(); i++)
                    if(((Portal)allPortals.get(i)).getLocation().equals(attachedBlock.getLocation().add(0, -2, 0).getBlock().getLocation()))
                    {
                        event.getPlayer().sendMessage(ChatColor.RED + "You cannot put a sign a portal with an existing sign.");
                        event.setCancelled(true);
                        event.getBlock().setType(Material.AIR);
                        event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), new ItemStack(Material.SIGN));
                        return;
                    }

                String name = event.getLine(0);
                if(name.length() == 0)
                {
                    event.getPlayer().sendMessage(ChatColor.RED + "The name of the portal cannot be left blank");
                    return;
                }
                String chan = event.getLine(1);
                int channel = -1;
                if(isInteger(chan))
                {
                    channel = Integer.parseInt(chan);
                } else
                {
                    event.getPlayer().sendMessage(ChatColor.RED + "The channel must be an integer in order to create a portal");
                    return;
                }
                if(channel < 0 || channel > 9999){
                	event.getPlayer().sendMessage(ChatColor.RED + "The channel must be between 0 and 10,000.");
                    return;
                }
                ArrayList<SerializableLocation> serFrameLocs = defineFrameLocations(s.getFacing(), attachedBlock);
                	if(frameIsComplete(serFrameLocs) == false){
                		event.getPlayer().sendMessage(ChatColor.RED+"The portal cannot be created because the frame is incomplete.");
                		return;
                	}
                String color = getColor(attachedBlock);
                if(ColorPortals.usePerms && !event.getPlayer().hasPermission("colorportals.nodistance")){
                	if(ColorPortals.minDistance != 0 || ColorPortals.maxDistance != 0){
                		ArrayList<Portal> portals = getSimilarPortals(channel, color);
                		if(portals.size() != 0){
                			Portal toConnect = portals.get(portals.size()-1);
                			Location connectLoc = toConnect.getLocation();
                			int distance = 0;
                			//portals are in the same world
                			if(connectLoc.getWorld().equals(event.getBlock().getWorld()))
                				distance = (int)event.getBlock().getLocation().distance(connectLoc);
                			else
                				distance = -1;
                		
                			if(ColorPortals.minDistance != 0){
                				if(distance != -1 && distance < ColorPortals.minDistance){
                					event.getPlayer().sendMessage(ChatColor.RED+"This portal is "+ChatColor.WHITE+(ColorPortals.minDistance - distance)+ChatColor.RED+" blocks too close to the previous portal in the chain.");
                					return;
                				}
                			}
                			if(ColorPortals.maxDistance != 0){
                				if(distance == -1){
                					event.getPlayer().sendMessage(ChatColor.RED+"This portal cannot be created because there is a maximum distance of "+ ChatColor.WHITE+ColorPortals.maxDistance+ChatColor.RED+" blocks and this portal is a different world than the previous portal in the chain.");
                					return;
                				}
                				else if(distance > ColorPortals.maxDistance){
                					event.getPlayer().sendMessage(ChatColor.RED+"This portal is "+ChatColor.WHITE+(distance - ColorPortals.maxDistance)+ChatColor.RED+" blocks too far away from the previous portal in the chain.");
                					return;
                				}
                			}
                		}
                	}
                }
                event.setLine(2, "");
                event.setLine(3, ChatColor.GRAY + "INACTIVE");
                SerializableLocation serLoc = new SerializableLocation(attachedBlock.getLocation().add(0, -2, 0).getBlock().getLocation());
                SerializableLocation signLoc = new SerializableLocation(event.getBlock().getLocation());
                
                if(portalCanBeCreated(Integer.valueOf(channel), color))
                {
                    Portal portal = new Portal(name, Integer.valueOf(channel), serLoc, color, signLoc, serFrameLocs, false);
                    portal.setNode(getSimilarPortals(portal).size()+1);
                	event.setLine(1, portal.getChannel()+"."+portal.getNode());
                	
                    CreatePortalEvent e = new CreatePortalEvent(portal, event.getPlayer(), s);
                    Bukkit.getServer().getPluginManager().callEvent(e);

                } else
                {
                    event.getPlayer().sendMessage(ChatColor.RED+"There are already "+ColorPortals.maxPortalsPerGroup+" "+color+ " portals on channel "+channel+".");
                    event.setCancelled(true);
                }
            }
        }
    }
    
    @EventHandler
    public void onPortalCreate(CreatePortalEvent event){
    	Portal portal = event.getPortal();
    	if( ! (allPortals.contains(portal)))
    		allPortals.add(portal);
    	event.getPlayer().sendMessage(ChatColor.GRAY+"You have added a "+portal.getColor()+" portal on channel "+portal.getChannel());
        linkPortalToChain(portal, event.getSign().getData());
    }
    
    @EventHandler
    public void onPortalDestroy(DestroyPortalEvent event){
    	Portal portal = event.getPortal();
        
    	//portal was destroyed but the sign is still on it
    	if(portal.getSignLocation().getBlock().getType() == Material.WALL_SIGN){
    		org.bukkit.block.Sign portalSign = (org.bukkit.block.Sign)portal.getSignLocation().getBlock().getState();
			portalSign.setLine(2, "");
			portalSign.setLine(3, ChatColor.DARK_RED+"DESTROYED");
			portalSign.update(true);
    	}
    	removePortalFromChain(portal);
		return;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event)
    {
        if(event.getBlock().getType() == Material.WALL_SIGN){
        	Sign s = (Sign)event.getBlock().getState().getData();
        	Block attachedBlock = event.getBlock().getRelative(s.getAttachedFace());
        	if(attachedBlock.getType() != Material.WOOL)
        		return;
        	for(int i = 0; i < allPortals.size(); i++)
        		if(attachedBlock.getLocation().add(0, -2, 0).getBlock().getLocation().equals((allPortals.get(i)).getLocation()))
        		{
        			if(ColorPortals.usePerms && !event.getPlayer().hasPermission("colorportals.destroy"))
        			{
        				event.getPlayer().sendMessage(ChatColor.DARK_RED+"You are not authorized to destroy portals");
        				event.setCancelled(true);
        				return;
        			}
        			DestroyPortalEvent e = new DestroyPortalEvent(allPortals.get(i), event.getPlayer());
        			Bukkit.getServer().getPluginManager().callEvent(e);
            }
        }
        else if(event.getBlock().getType() == Material.WOOL){
        	Location loc = event.getBlock().getLocation();
        	Portal brokenPortal = null;
        	
        	OUTERMOST : for(Portal p : allPortals){
        		for(Location l : p.getFrameLocations()){
        			//the wool broken is part of a portal
        			if(l.equals(loc)){
        				brokenPortal = p;
        				break OUTERMOST;
        			}
        		}
        	}
        	if(brokenPortal == null)
        		return;
        	
        	//deactivate portal here
        	DestroyPortalEvent e = new DestroyPortalEvent(brokenPortal, event.getPlayer());
			Bukkit.getServer().getPluginManager().callEvent(e);     	
        }

    }

    //DEAL WITH THIS LATER. MUST FIND A WAY TO CALL DESTROYPORTALEVENT FROM EXPLOSION
    @EventHandler
    public void onExplosion(EntityExplodeEvent event)
    {
        for(int i=0; i<event.blockList().size(); i++)
        {
            if(event.blockList().get(i).getType() == Material.WALL_SIGN)
            {
            	event.blockList().remove(i);

            }
        }

    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event)
    {
        Player player = event.getPlayer();
        if(event.getAction() != Action.RIGHT_CLICK_BLOCK)
            return;
        if( ! (event.getClickedBlock().getType() == Material.STONE_BUTTON || event.getClickedBlock().getType() == Material.WOOD_BUTTON))
            return;
        if( ! (event.getClickedBlock().getRelative(BlockFace.DOWN).getType() == Material.STONE_PLATE || event.getClickedBlock().getRelative(BlockFace.DOWN).getType() == Material.WOOD_PLATE))
            return;
        Block underPlate = event.getClickedBlock().getRelative(BlockFace.DOWN).getRelative(BlockFace.DOWN);
        if(underPlate.getType() != Material.WOOL)
            return;
        Portal fromPortal = null;
        for(int i = 0; i < allPortals.size(); i++)
        {
            if(allPortals.get(i).getLocation().equals(event.getClickedBlock().getRelative(BlockFace.DOWN).getLocation())){
            	fromPortal = (Portal)allPortals.get(i);
            	break;
            }
        }

        if(fromPortal == null)
            return;
        if(fromPortal.getLinkedPortal() == null)
            return;
        if(!ColorPortals.usePerms || player.hasPermission("colorportals.use"))
        {
            Location loc = fromPortal.getLinkedPortal().getLocation();
            Location adjustedLoc = loc.add(0.5, 0, 0.5);
            Sign toSign = (Sign)fromPortal.getLinkedPortal().getSignLocation().getBlock().getState().getData();
            adjustedLoc.setYaw(faceToYaw(toSign.getFacing())+180F);
            int entityCount = 0;
            //player has clicked the button while standing in the portal
            if(player.getEyeLocation().getBlock().getLocation().equals(event.getClickedBlock().getLocation()))
            {
                for(Entity entity : player.getNearbyEntities(5, 5, 5))
                {
                    if(entity.getLocation().getBlock().getLocation().equals(event.getClickedBlock().getRelative(BlockFace.DOWN).getLocation()))
                    {
                        entity.teleport(adjustedLoc);
                        entityCount++;
                    }
                }
                if(entityCount > 0){
                	event.getClickedBlock().getWorld().playSound(event.getClickedBlock().getLocation(), Sound.ENDERMAN_TELEPORT, 1.0F, 0.5F);
                    loc.getWorld().playSound(loc, Sound.ENDERMAN_TELEPORT, 1.0F, 0.5F);
                }

                player.teleport(adjustedLoc);
                event.getClickedBlock().getWorld().playSound(event.getClickedBlock().getLocation(), Sound.ENDERMAN_TELEPORT, 1.0F, 0.5F);
                player.getWorld().playSound(loc, Sound.ENDERMAN_TELEPORT, 1.0F, 0.5F);
            } else
            	//player has clicked the button while standing near the portal
            {
                for(Entity entity : player.getNearbyEntities(10, 10, 10))
                {
                    if(entity.getLocation().getBlock().getLocation().equals(event.getClickedBlock().getRelative(BlockFace.DOWN).getLocation()))
                    {
                        entity.teleport(adjustedLoc);
                        entityCount++;
                    }
                }
                if(entityCount > 0){
                	event.getClickedBlock().getWorld().playSound(event.getClickedBlock().getLocation(), Sound.ENDERMAN_TELEPORT, 1.0F, 0.5F);
                    loc.getWorld().playSound(loc, Sound.ENDERMAN_TELEPORT, 1.0F, 0.5F);
                }

            }
        } else
        {
            player.sendMessage(ChatColor.DARK_RED+"You are not authorized to use portals");
        }
    }
    
    @EventHandler
    public void onEntityInteract(EntityInteractEvent event){
    	if( !(event.getEntity() instanceof Arrow))
    		return;
    	if(event.getBlock().getType() != Material.WOOD_BUTTON)
    		return;
    	Arrow shot = (Arrow)event.getEntity();
    	if( ! (shot.getShooter() instanceof Player))
    		return;
    	Player player = (Player)shot.getShooter();

        if( ! (event.getBlock().getRelative(BlockFace.DOWN).getType() == Material.STONE_PLATE || event.getBlock().getRelative(BlockFace.DOWN).getType() == Material.WOOD_PLATE))
            return;
        Block underPlate = event.getBlock().getRelative(BlockFace.DOWN).getRelative(BlockFace.DOWN);
        if(underPlate.getType() != Material.WOOL)
            return;
        Portal fromPortal = null;
        for(int i = 0; i < allPortals.size(); i++)
        {
            if(allPortals.get(i).getLocation().equals(event.getBlock().getRelative(BlockFace.DOWN).getLocation())){
            	fromPortal = (Portal)allPortals.get(i);
            	break;
            }
        }

        if(fromPortal == null)
            return;
        if(fromPortal.getLinkedPortal() == null)
            return;
        
        if(!ColorPortals.usePerms || player.hasPermission("colorportals.use"))
        {
            Location loc = fromPortal.getLinkedPortal().getLocation();
            Location adjustedLoc = loc.add(0.5, 0, 0.5);
            Sign toSign = (Sign)fromPortal.getLinkedPortal().getSignLocation().getBlock().getState().getData();
            adjustedLoc.setYaw(faceToYaw(toSign.getFacing())+180F);
            
            int entityCount = 0;
                for(Entity entity : event.getEntity().getNearbyEntities(10, 10, 10))
                {
                    if(entity.getLocation().getBlock().getLocation().equals(event.getBlock().getRelative(BlockFace.DOWN).getLocation()))
                    {
                        entity.teleport(adjustedLoc);
                        entityCount++;
                    }
                }
                if(entityCount > 0){
                	event.getBlock().getWorld().playSound(event.getBlock().getLocation(), Sound.ENDERMAN_TELEPORT, 1.0F, 0.5F);
                    loc.getWorld().playSound(loc, Sound.ENDERMAN_TELEPORT, 1.0F, 0.5F);
                }

            }
        else
            player.sendMessage(ChatColor.DARK_RED+"You are not authorized to use portals");
    }

    @EventHandler
    public void signDetachCheck(BlockPhysicsEvent event)
    {
        Block b = event.getBlock();
        if(b.getType() == Material.WALL_SIGN)
        {
            Sign s = (Sign)b.getState().getData();
            Block attachedBlock = b.getRelative(s.getAttachedFace());
            Block checkLoc = attachedBlock.getRelative(BlockFace.DOWN).getRelative(BlockFace.DOWN);
            if(attachedBlock.getType() == Material.AIR)
            {
                for(int i = 0; i < allPortals.size(); i++)
                    if(checkLoc.getLocation().equals(allPortals.get(i).getLocation()))
                    {
                    	DestroyPortalEvent e = new DestroyPortalEvent(allPortals.get(i), null);
            			Bukkit.getServer().getPluginManager().callEvent(e);
                    }

            }
        }
    }
    
    //this will return an arraylist of all similar portals with the given portal included
    public ArrayList<Portal> getSimilarPortals(Portal portal){
    	ArrayList<Portal> similarPortals = new ArrayList<Portal>();
    	for(Portal checkedPortal : allPortals){
    		if(checkedPortal.getChannel().toString().equals(portal.getChannel().toString()) && (checkedPortal.getColor().equalsIgnoreCase(portal.getColor()))){
    			similarPortals.add(checkedPortal);
    		}
    	}
    	//sort the portals by node
    	Collections.sort(similarPortals, new PortalNodeComparator());
    	return similarPortals;
    }
    public ArrayList<Portal> getSimilarPortals(Integer channel, String color){
    	ArrayList<Portal> similarPortals = new ArrayList<Portal>();
    	for(Portal checkedPortal : allPortals){
    		if(checkedPortal.getChannel().toString().equals(channel.toString()) && (checkedPortal.getColor().equalsIgnoreCase(color))){
    			similarPortals.add(checkedPortal);
    		}
    	}
    	//sort the portals by node
    	Collections.sort(similarPortals, new PortalNodeComparator());
    	return similarPortals;
    }

    public void linkPortalToChain(Portal provided, byte direction)
    {
    	final ArrayList<Portal> similarPortals = getSimilarPortals(provided);
    	if(similarPortals.size() == 1)
    		return;
    	
        for(int i=0; i<similarPortals.size(); i++){
        	if(i == similarPortals.size()-1){
        		final int place = i;
        		plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() { 
    				public void run() { 
    					similarPortals.get(place).setLinkedPortal(similarPortals.get(0));
    	        		org.bukkit.block.Sign sign = (org.bukkit.block.Sign)similarPortals.get(place).getSignLocation().getBlock().getState();
    	                sign.setLine(0, similarPortals.get(place).getName());
    	                sign.setLine(1, ""+similarPortals.get(place).getChannel()+"."+similarPortals.get(place).getNode());
    	                sign.setLine(2, ChatColor.GREEN+"Warps To:");
    	                sign.setLine(3, similarPortals.get(place).getLinkedPortal().getName());
    	                sign.update(true);
    					} 
    			}, 2L); 
        	}
        	else{
        		similarPortals.get(i).setLinkedPortal(similarPortals.get(i+1));
                org.bukkit.block.Sign sign = (org.bukkit.block.Sign)similarPortals.get(i).getSignLocation().getBlock().getState();
                sign.setLine(0, similarPortals.get(i).getName());
                sign.setLine(1, ""+similarPortals.get(i).getChannel()+"."+similarPortals.get(i).getNode());
                sign.setLine(2, ChatColor.GREEN+"Warps To:");
                sign.setLine(3, similarPortals.get(i).getLinkedPortal().getName());
                org.bukkit.block.Sign linkedSign = (org.bukkit.block.Sign)similarPortals.get(i).getLinkedPortal().getSignLocation().getBlock().getState();
                linkedSign.setLine(2, ChatColor.GREEN+"Warps To:");
                linkedSign.setLine(3, similarPortals.get(i).getName());
                linkedSign.update(true);
                sign.update(true);
        	}
        }
    }
    
    public void removePortalFromChain(Portal removedPortal)
    {
    	ArrayList<Portal> similarPortals = getSimilarPortals(removedPortal);
    	//sort the portals by node
    	Collections.sort(similarPortals, new PortalNodeComparator());
    	if(similarPortals.size() == 1){
    		allPortals.remove(removedPortal);
    		return;
    	}
    	else if(similarPortals.size() == 2){
    		removedPortal.getLinkedPortal().setNode(1);
    		allPortals.remove(removedPortal);
    		removedPortal.getLinkedPortal().setLinkedPortal(null);
            org.bukkit.block.Sign sign = (org.bukkit.block.Sign)removedPortal.getLinkedPortal().getSignLocation().getBlock().getState();
            sign.setLine(0, removedPortal.getLinkedPortal().getName());
            sign.setLine(1, ""+removedPortal.getLinkedPortal().getChannel()+"."+removedPortal.getLinkedPortal().getNode());
            sign.setLine(2, "");
            sign.setLine(3, ChatColor.GRAY+"INACTIVE");
            sign.update(true);
            return;
    	}
    	
    	Portal previousPortal = null;
    	Portal nextPortal = null;
    	
    	int removedSpot = similarPortals.indexOf(removedPortal);
    	
    	if(removedSpot == 0){
    		previousPortal = similarPortals.get(similarPortals.size()-1); //may need to be -2
    		nextPortal = similarPortals.get(removedSpot+1);
    	}
    	else if(removedSpot == similarPortals.size()-1){
    		nextPortal = similarPortals.get(0);
    		previousPortal = similarPortals.get(removedSpot-1);
    	}
    	else{
    		nextPortal = similarPortals.get(removedSpot+1);
    		previousPortal = similarPortals.get(removedSpot-1);
    	}
    	
    	//update signs for the nodes of all portals
    	for(int i= removedSpot+1; i<similarPortals.size(); i++){
    		similarPortals.get(i).setNode(similarPortals.get(i).getNode()-1);
    		org.bukkit.block.Sign sign = (org.bukkit.block.Sign)similarPortals.get(i).getSignLocation().getBlock().getState();
    		sign.setLine(1, ""+similarPortals.get(i).getChannel()+"."+similarPortals.get(i).getNode());
    		sign.update(true);
    	}
    	
    	previousPortal.setLinkedPortal(nextPortal);
    	
    	//update signs of previousPortal and nextPortal
        org.bukkit.block.Sign signPrevious = (org.bukkit.block.Sign)previousPortal.getSignLocation().getBlock().getState();
        signPrevious.setLine(0, previousPortal.getName());
        signPrevious.setLine(1, ""+previousPortal.getChannel()+"."+previousPortal.getNode());
        signPrevious.setLine(2, ChatColor.GREEN+"Warps To:");
        signPrevious.setLine(3, previousPortal.getLinkedPortal().getName());
        signPrevious.update(true);
        
        allPortals.remove(removedPortal);
    }

    public boolean portalCanBeCreated(Integer channel, String color)
    {
        if(getSimilarPortals(channel, color).size() >= ColorPortals.maxPortalsPerGroup)
                return false;
        return true;
    }

    public boolean isInteger(String s)
    {
        try
        {
            Integer.parseInt(s);
        }
        catch(NumberFormatException e)
        {
            return false;
        }
        return true;
    }

    public String getColor(Block block)
    {
        if(block.getType() != Material.WOOL)
            return "none";
        String returned = "none";
        switch(block.getData())
        {
        case 0: // '\0'
            returned = "white";
            break;

        case 1: // '\001'
            returned = "orange";
            break;

        case 2: // '\002'
            returned = "magenta";
            break;

        case 3: // '\003'
            returned = "aqua";
            break;

        case 4: // '\004'
            returned = "yellow";
            break;

        case 5: // '\005'
            returned = "lime";
            break;

        case 6: // '\006'
            returned = "pink";
            break;

        case 7: // '\007'
            returned = "charcoal";
            break;

        case 8: // '\b'
            returned = "gray";
            break;

        case 9: // '\t'
            returned = "cyan";
            break;

        case 10: // '\n'
            returned = "purple";
            break;

        case 11: // '\013'
            returned = "blue";
            break;

        case 12: // '\f'
            returned = "brown";
            break;

        case 13: // '\r'
            returned = "green";
            break;

        case 14: // '\016'
            returned = "red";
            break;

        case 15: // '\017'
            returned = "black";
            break;
        }
        return returned;
    }
    
    public boolean frameIsComplete(ArrayList<SerializableLocation> frameLocs){
    	String color = getColor(frameLocs.get(0).deserialize().getBlock());
    	for(SerializableLocation sLoc : frameLocs){
    		if( ! (color.equals(getColor(sLoc.deserialize().getBlock()))))
    			return false;
    	}
    	return true;
    }

    public byte determineDataOfDirection(BlockFace bf)
    {
        if(bf.equals(BlockFace.NORTH))
            return 2;
        if(bf.equals(BlockFace.SOUTH))
            return 5;
        if(bf.equals(BlockFace.WEST))
            return 3;
        return ((byte)(!bf.equals(BlockFace.EAST) ? 0 : 4));
    }
    
    public ArrayList<SerializableLocation> defineFrameLocations(BlockFace signDirection, Block keyBlock){
    	ArrayList<SerializableLocation> frameLocs = new ArrayList<SerializableLocation>();
    	BlockFace togo = BlockFace.SELF;
    	
    	if(signDirection == BlockFace.NORTH || signDirection == BlockFace.SOUTH)
    		togo = BlockFace.EAST;
    	else
    		togo = BlockFace.NORTH;
    	
    	Block topLeft = keyBlock.getRelative(togo);
    	Block topRight = keyBlock.getRelative(togo.getOppositeFace());
    	Block leftMid = topLeft.getRelative(BlockFace.DOWN);
    	Block rightMid = topRight.getRelative(BlockFace.DOWN);
    	Block lowerLeft = leftMid.getRelative(BlockFace.DOWN);
    	Block lowerRight = rightMid.getRelative(BlockFace.DOWN);
    	Block bottomLeft = lowerLeft.getRelative(BlockFace.DOWN);
    	Block bottomRight = lowerRight.getRelative(BlockFace.DOWN);
    	Block lowerMid = keyBlock.getRelative(BlockFace.DOWN).getRelative(BlockFace.DOWN).getRelative(BlockFace.DOWN);
    		
    	frameLocs.add(new SerializableLocation(keyBlock.getLocation()));
    	frameLocs.add(new SerializableLocation(topLeft.getLocation()));
    	frameLocs.add(new SerializableLocation(topRight.getLocation()));
    	frameLocs.add(new SerializableLocation(leftMid.getLocation()));
    	frameLocs.add(new SerializableLocation(rightMid.getLocation()));
    	frameLocs.add(new SerializableLocation(lowerLeft.getLocation()));
    	frameLocs.add(new SerializableLocation(lowerRight.getLocation()));
    	frameLocs.add(new SerializableLocation(bottomLeft.getLocation()));
    	frameLocs.add(new SerializableLocation(bottomRight.getLocation()));
    	frameLocs.add(new SerializableLocation(lowerMid.getLocation()));

    	return frameLocs;
    }
    
    public float faceToYaw(BlockFace bf){
		if(bf.equals(BlockFace.NORTH))
			return 0F;
		else if(bf.equals(BlockFace.EAST))
			return 90F;
		else if(bf.equals(BlockFace.SOUTH))
			return 180F;
		else if(bf.equals(BlockFace.WEST))
			return 270F;
		return 0F;
	}
}
