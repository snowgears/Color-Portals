package com.snowgears.colorportals;


import com.snowgears.colorportals.listeners.EntityListener;
import com.snowgears.colorportals.listeners.PortalListener;
import com.snowgears.colorportals.utils.BukkitUtils;
import com.snowgears.colorportals.utils.Metrics;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;

public class ColorPortals extends JavaPlugin {

    private static ColorPortals plugin;
    private final PortalListener portalListener = new PortalListener(this);
    private final EntityListener entityListener = new EntityListener(this);
    protected FileConfiguration config;
    protected File portalFile;
    private PortalHandler portalHandler = new PortalHandler(this);
    private BukkitUtils bukkitUtils = new BukkitUtils();
    private boolean usePerms;
    private int maxPortalsPerGroup;
    private boolean walkOnActivation;
    private boolean portalProtection;
    private int minDistance;
    private int maxDistance;

    public static ColorPortals getPlugin() {
        return plugin;
    }

    public void onEnable() {
        plugin = this;
        getServer().getPluginManager().registerEvents(portalListener, this);
        getServer().getPluginManager().registerEvents(entityListener, this);

        try {
            Metrics metrics = new Metrics(this);
            metrics.start();
        } catch (IOException e) {
            // Failed to submit the stats
        }

        File configFile = new File(this.getDataFolder() + "/config.yml");
        if (!configFile.exists()) {
            this.saveDefaultConfig();
        }

        File fileDirectory = new File(this.getDataFolder(), "Data");
        if (!fileDirectory.exists()) {
            boolean success;
            success = (fileDirectory.mkdirs());
            if (!success) {
                getServer().getConsoleSender().sendMessage("[ColorPortals]" + ChatColor.RED + " Data folder could not be created.");
            }
        }

        usePerms = getConfig().getBoolean("usePermissions");
        maxPortalsPerGroup = getConfig().getInt("maxPortalsPerGroup");
        //must be allowed at least 2 portals per group for plugin to work
        if (maxPortalsPerGroup == 1)
            maxPortalsPerGroup = 2;
        walkOnActivation = getConfig().getBoolean("walkOnActivation");
        portalProtection = getConfig().getBoolean("portalProtection");
        minDistance = getConfig().getInt("minDistanceBetweenPortals");
        maxDistance = getConfig().getInt("maxDistanceBetweenPortals");


        portalFile = new File(fileDirectory + "/portals.yml");
        if (!portalFile.exists()) { // file doesn't exist
            try {
                portalFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else { //file does exist
            if (portalFile.length() > 0) { //file contains something
                portalHandler.loadPortals();
            }
        }
    }

    public void onDisable() {
        //portalHandler.savePortals(); //(this is already done when creating/destroying portals)
        plugin = null;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 1) {
            if ((cmd.getName().equalsIgnoreCase("portal") || cmd.getName().equalsIgnoreCase("cp")) && args[0].equalsIgnoreCase("list")) {
                sender.sendMessage(ChatColor.WHITE + "There are " + ChatColor.GOLD + portalHandler.getNumberOfPortals() + ChatColor.WHITE + " portals registered on this server.");
                return true;
            }
        }
        return false;
    }

    public PortalListener getPortalListener() {
        return portalListener;
    }

    public EntityListener getEntityListener() {
        return entityListener;
    }

    public PortalHandler getPortalHandler() {
        return portalHandler;
    }

    public BukkitUtils getBukkitUtils() {
        return bukkitUtils;
    }

    public boolean getUsePerms() {
        return usePerms;
    }

    public int getMaxPortalsPerGroup() {
        return maxPortalsPerGroup;
    }

    public boolean getWalkOnActivation() {
        return walkOnActivation;
    }

    public boolean getPortalProtection() {
        return portalProtection;
    }

    public int getMinDistance() {
        return minDistance;
    }

    public int getMaxDistance() {
        return maxDistance;
    }
}