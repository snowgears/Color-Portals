package com.snowgears.colorportals;


import com.snowgears.colorportals.listeners.EntityListener;
import com.snowgears.colorportals.listeners.PortalListener;
import com.snowgears.colorportals.utils.BukkitUtils;
import com.snowgears.colorportals.utils.ConfigUpdater;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

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
    private boolean allowMobs;
    private boolean allowItems;
    private boolean allowMinecarts;

    public static ColorPortals getPlugin() {
        return plugin;
    }

    public void onEnable() {
        plugin = this;
        getServer().getPluginManager().registerEvents(portalListener, this);
        getServer().getPluginManager().registerEvents(entityListener, this);

        File configFile = new File(this.getDataFolder() + "/config.yml");
        if (!configFile.exists()) {
            this.saveDefaultConfig();
        }

        try {
            ConfigUpdater.update(plugin, "config.yml", configFile, new ArrayList<>());
        } catch (IOException e) {
            e.printStackTrace();
        }

        config = YamlConfiguration.loadConfiguration(configFile);

        usePerms = config.getBoolean("usePermissions");
        maxPortalsPerGroup = config.getInt("maxPortalsPerGroup");
        //must be allowed at least 2 portals per group for plugin to work
        if (maxPortalsPerGroup < 2)
            maxPortalsPerGroup = 2;
        walkOnActivation = config.getBoolean("walkOnActivation");
        portalProtection = config.getBoolean("portalProtection");
        minDistance = config.getInt("minDistanceBetweenPortals");
        maxDistance = config.getInt("maxDistanceBetweenPortals");
        allowMobs = config.getBoolean("allowMobs");
        allowItems = config.getBoolean("allowItems");
        allowMinecarts = config.getBoolean("allowMinecarts");


        File fileDirectory = new File(this.getDataFolder(), "Data");
        if (!fileDirectory.exists()) {
            boolean success;
            success = (fileDirectory.mkdirs());
            if (!success) {
                getServer().getConsoleSender().sendMessage("[ColorPortals]" + ChatColor.RED + " Data folder could not be created.");
            }
        }

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

    public boolean getAllowMobs(){
        return allowMobs;
    }

    public boolean getAllowItems(){
        return allowItems;
    }

    public boolean getAllowMinecarts(){
        return allowMinecarts;
    }
}