package com.snowgears.colorportals;

/**
 * This class handles all of the basic functions in the managing of Portals.
 * - Managing HashMap of Portals
 *    - Adding Portals
 *    - Removing Portals
 * - Saving data file
 * - Loading data file
 */

import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;


public class PortalHandler {

    public ColorPortals plugin = ColorPortals.getPlugin();

    private HashMap<Location, Portal> allPortals = new HashMap<Location, Portal>();
    private HashMap<UUID, Integer> createdPortalAmounts = new HashMap<UUID, Integer>();

    public PortalHandler(ColorPortals instance) {
        plugin = instance;
    }

    public Portal getPortal(Location loc) {
        return allPortals.get(loc);
    }

    public void registerPortal(Portal portal) {
        allPortals.put(portal.getSignLocation(), portal);
        if(createdPortalAmounts.containsKey(portal.getCreator())){
            int amt = createdPortalAmounts.get(portal.getCreator());
            createdPortalAmounts.put(portal.getCreator(), amt + 1);
        }
        else{
            createdPortalAmounts.put(portal.getCreator(), 1);
        }

        ArrayList<Portal> portalFamily = this.getPortalFamily(portal);
        if (portalFamily.size() == 1) {
            portal.setLinkedPortal(null);
            return;
        }
        Portal lastPortal = portalFamily.get(portalFamily.size() - 2);
        lastPortal.setLinkedPortal(portal);
        portal.setLinkedPortal(portalFamily.get(0));
    }

    //this method should only be called from the portal class when removing portals
    public void deregisterPortal(Portal portal) {
        if(createdPortalAmounts.containsKey(portal.getCreator())){
            int amt = createdPortalAmounts.get(portal.getCreator());
            createdPortalAmounts.put(portal.getCreator(), amt - 1);
        }
        if (allPortals.containsKey(portal.getSignLocation())) {
            allPortals.remove(portal.getSignLocation());
        }
    }

    public Collection<Portal> getAllPortals() {
        return allPortals.values();
    }

    public int getPortalsCreated(UUID player){
        if(createdPortalAmounts.containsKey(player))
            return createdPortalAmounts.get(player);
        return 0;
    }

    public Portal getPortalByFrameLocation(Location location) {
        Location loc;
        for (int x = -1; x < 2; x++) {
            for (int y = 0; y < 4; y++) {
                for (int z = -1; z < 2; z++) {
                    loc = location.clone().add(x, y, z);
                    if (getPortal(loc) != null) {
                        Portal portal = getPortal(loc);
                        if (portal.getOccupiedLocations().contains(location))
                            return portal;
                    }
                }
            }
        }
        return null;
    }

    //TODO bug here. Will always prioritize finding north sign first and would return null if multiple signs on portal
    public Portal getPortalByKeyBlock(Block portalKeyBlock) {
        if (portalKeyBlock.getRelative(BlockFace.NORTH).getBlockData() instanceof WallSign) {
            return plugin.getPortalHandler().getPortal(portalKeyBlock.getRelative(BlockFace.NORTH).getLocation());
        } else if (portalKeyBlock.getRelative(BlockFace.EAST).getBlockData() instanceof WallSign) {
            return plugin.getPortalHandler().getPortal(portalKeyBlock.getRelative(BlockFace.EAST).getLocation());
        } else if (portalKeyBlock.getRelative(BlockFace.SOUTH).getBlockData() instanceof WallSign) {
            return plugin.getPortalHandler().getPortal(portalKeyBlock.getRelative(BlockFace.SOUTH).getLocation());
        } else if (portalKeyBlock.getRelative(BlockFace.WEST).getBlockData() instanceof WallSign) {
            return plugin.getPortalHandler().getPortal(portalKeyBlock.getRelative(BlockFace.WEST).getLocation());
        }
        return null;
    }

    public int getNumberOfPortals() {
        return allPortals.size();
    }

    /**
     * Finds all portals with the same color and channel as the portal provided
     * Return:
     * - arraylist of all portals in the family (matching color and channel)
     */
    public ArrayList<Portal> getPortalFamily(Portal portal) {
        ArrayList<Portal> portalFamily = new ArrayList<Portal>();
        for (Portal checkedPortal : plugin.getPortalHandler().getAllPortals()) {
            if (checkedPortal.getChannel() == portal.getChannel() && checkedPortal.getColor().equals(portal.getColor())) {
                portalFamily.add(checkedPortal);
            }
        }
        Collections.sort(portalFamily);
        return portalFamily;
    }

    /**
     * Finds all portals with the same color and channel as the ones provided
     * Return:
     * - arraylist of all portals in the family (matching color and channel)
     */
    public ArrayList<Portal> getPortalFamily(Integer channel, DyeColor color) {
        ArrayList<Portal> portalFamily = new ArrayList<Portal>();
        for (Portal checkedPortal : plugin.getPortalHandler().getAllPortals()) {
            if (checkedPortal.getChannel() == channel && checkedPortal.getColor().equals(color)) {
                portalFamily.add(checkedPortal);
            }
        }
        Collections.sort(portalFamily);
        return portalFamily;
    }

    private ArrayList<Portal> orderedPortalList() {
        ArrayList<Portal> list = new ArrayList<Portal>(allPortals.values());
        Collections.sort(list);
        return list;
    }

    public void savePortals() {
        File fileDirectory = new File(plugin.getDataFolder(), "Data");
        if (!fileDirectory.exists())
            fileDirectory.mkdir();
        File portalFile = new File(fileDirectory + "/portals.yml");
        if (!portalFile.exists()) { // file doesn't exist
            try {
                portalFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else { //does exist, clear it for future saving
            PrintWriter writer = null;
            try {
                writer = new PrintWriter(portalFile);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            writer.print("");
            writer.close();
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(portalFile);
        ArrayList<Portal> portalList = orderedPortalList();

        for (Portal portal : portalList) {
            config.set("portals." + portal.getColor().toString() + "." + portal.getChannel() + "-" + portal.getNode() + ".name", portal.getName());
            config.set("portals." + portal.getColor().toString() + "." + portal.getChannel() + "-" + portal.getNode() + ".location", locationToString(portal.getSignLocation()));
            config.set("portals." + portal.getColor().toString() + "." + portal.getChannel() + "-" + portal.getNode() + ".creator", portal.getCreator().toString());
        }

        try {
            config.save(portalFile);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    public void loadPortals() {
        File fileDirectory = new File(plugin.getDataFolder(), "Data");
        if (!fileDirectory.exists())
            return;
        File portalFile = new File(fileDirectory + "/portals.yml");
        if (!portalFile.exists())
            return;

        YamlConfiguration config = YamlConfiguration.loadConfiguration(portalFile);
        loadPortalsFromConfig(config);
    }

    private void loadPortalsFromConfig(YamlConfiguration config) {

        if (config.getConfigurationSection("portals") == null)
            return;
        Set<String> allPortalColors = config.getConfigurationSection("portals").getKeys(false);

        ArrayList<Portal> portalFamily = new ArrayList<Portal>();

        //  for (String portalColor : allPortalColors) {
        for (Iterator<String> colorIterator = allPortalColors.iterator(); colorIterator.hasNext(); ) {
            String portalColor = colorIterator.next();
            Set<String> allPortalChannels = config.getConfigurationSection("portals." + portalColor).getKeys(false);
            portalFamily.clear();

            int previousChannel = 0;
            if (allPortalChannels.iterator().hasNext()) {
                String stringChannel = allPortalChannels.iterator().next();
                String[] split = stringChannel.split("-");
                previousChannel = Integer.parseInt(split[0]);
            }

            //  for (String portalChannel : allPortalChannels) {
            for (Iterator<String> channelIterator = allPortalChannels.iterator(); channelIterator.hasNext(); ) {
                String portalChannel = channelIterator.next();

                Location signLocation = locationFromString(config.getString("portals." + portalColor + "." + portalChannel + ".location"));
                Block signBlock = signLocation.getBlock();

                if (signBlock.getBlockData() instanceof WallSign) {

                    DyeColor color = DyeColor.valueOf(portalColor);

                    String[] split = portalChannel.split("-");
                    int channel = Integer.parseInt(split[0]);
                    int node = Integer.parseInt(split[1]);

                    String name = config.getString("portals." + portalColor + "." + portalChannel + ".name");

                    String creatorString = config.getString("portals." + portalColor + "." + portalChannel + ".creator");
                    UUID creator = UUID.fromString(creatorString);

                    Portal portal = new Portal(creator, name, color, channel, node, signLocation);


                    //previous portal was the last portal in the family (same color, different channel)
                    //portal working with now is the first portal of the new family
                    if (previousChannel != channel) {
                        previousChannel = channel;
                        //if family only has one portal, set link to null
                        if (portalFamily.size() == 1) {
                            portalFamily.get(0).setLinkedPortal(null);
                        }
                        //if family has more than 1 portal, link the last portal to the first portal
                        else {
                            portalFamily.get(portalFamily.size() - 1).setLinkedPortal(portalFamily.get(0));
                        }

                        //register portalFamily before resetting for the next family
                        for (Portal p : portalFamily) {
                            this.registerPortal(p);
                        }

                        //reset for next family
                        portalFamily.clear();
                        portalFamily.add(portal);
                    }
                    //portal working with now is still a member of the current family
                    else {
                        portalFamily.add(portal);

                        if (portalFamily.size() > 1) {
                            portalFamily.get(portalFamily.size() - 2).setLinkedPortal(portalFamily.get(portalFamily.size() - 1));
                        }

                        //if there are no more channels and no more colors to go through
                        if (!(channelIterator.hasNext() && colorIterator.hasNext())) {
                            //load the last portalFamily currently in memory to the class level
                            for (Portal p : portalFamily) {
                                this.registerPortal(p);
                            }
                        }
                    }
                }
            }
        }
    }

    public int getMaxPortalsPlayerCanBuild(Player player){
        if(!plugin.getUsePerms())
            return -1;
        int buildPermissionNumber = -1;
        for(PermissionAttachmentInfo permInfo : player.getEffectivePermissions()){
            if(permInfo.getPermission().equals("colorportals.max.*"))
                return -1;
            if(permInfo.getPermission().contains("colorportals.max")){
                try {
                    int tempNum = Integer.parseInt(permInfo.getPermission().substring(permInfo.getPermission().lastIndexOf(".") + 1));
                    if(tempNum > buildPermissionNumber)
                        buildPermissionNumber = tempNum;
                } catch (Exception e) {}
            }
        }
        return buildPermissionNumber;
    }

    private String locationToString(Location loc) {
        return loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
    }

    private Location locationFromString(String loc) {
        String[] parts = loc.split(",");
        return new Location(plugin.getServer().getWorld(parts[0]), Double.parseDouble(parts[1]), Double.parseDouble(parts[2]), Double.parseDouble(parts[3]));
    }
}