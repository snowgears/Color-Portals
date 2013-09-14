package com.snowgears.colorportals;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.snowgears.colorportals.listeners.ColorListener;
import com.snowgears.colorportals.utils.Metrics;
import com.snowgears.colorportals.utils.YamlHandler;

public class ColorPortals extends JavaPlugin{
	
	public final ColorListener alisten = new ColorListener(this);
	public static ColorPortals plugin;
	protected FileConfiguration config; 
	protected File portalFile = null;
	
	public static boolean usePerms = false;
	public static int maxPortalsPerGroup = 0;
	public static int minDistance = 0;
	public static int maxDistance = 0;
	
	public static HashMap<String, ArrayList<Portal>> portalMap = new HashMap<String, ArrayList<Portal>>();//string, all portals
	
	public void onEnable(){
		getServer().getPluginManager().registerEvents(alisten, this);

		try {
		    Metrics metrics = new Metrics(this);
		    metrics.start();
		} catch (IOException e) {
		    // Failed to submit the stats
		}
		
		File configFile = new File(this.getDataFolder() + "/config.yml");
		if(!configFile.exists())
		{
		  this.saveDefaultConfig();
		}
		
		File fileDirectory = new File(this.getDataFolder(), "Data");
		if(!fileDirectory.exists())
		{
			boolean success = false;
			success = (fileDirectory.mkdirs());
			if (!success) {
				getServer().getConsoleSender().sendMessage("[ColorPortals]"+ChatColor.RED+" Data folder could not be created.");
			}
		}
		
		portalFile = new File(fileDirectory + "/portals.yml");
		
		if(! portalFile.exists()){ // file doesn't exist
			try {
				portalFile.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		else{ //file does exist
			if (portalFile.length() > 0 ) { //file contains something
			portalMap = loadHashMapFrom(portalFile);
			System.out.println("[ColorPortals] Loading all portals from file... ");
			}
		}
		alisten.allPortals = alisten.getAllPortals();
		
		usePerms = getConfig().getBoolean("usePermissions");
		maxPortalsPerGroup = getConfig().getInt("maxPortalsPerGroup");
		//must be allowed at least 2 portals per group for plugin to work
		if(maxPortalsPerGroup < 2)
			maxPortalsPerGroup = 2;
		minDistance = getConfig().getInt("minDistanceBetweenPortals");
		maxDistance = getConfig().getInt("maxDistanceBetweenPortals");
	}
	
	public void onDisable(){
		portalMap.put("load", alisten.allPortals);
		saveHashMapTo(portalMap, portalFile);
		YamlHandler yh = new YamlHandler(this);
		yh.savePortalsToFile();
		System.out.println("[ColorPortals] Saving all portals to file... ");
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if(args.length == 1){
			if (cmd.getName().equalsIgnoreCase("color") || cmd.getName().equalsIgnoreCase("cp") && args[0].equalsIgnoreCase("list")) {
				if (sender instanceof Player) {
					Player player = (Player)sender;
					player.sendMessage("There are "+ChatColor.GOLD+alisten.allPortals.size()+ChatColor.WHITE+" portals registered.");
				}
			}
			return true;
		}
        return false;
    }
	
	public <K, V> void saveHashMapTo(HashMap<K, V> hashmap, File file) {
        ObjectOutputStream oos = null;
        try {
            oos = new ObjectOutputStream(new FileOutputStream(file));
            oos.writeObject(hashmap);
            oos.flush();
            oos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                oos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
   
    /**
    * Loads a HashMap<K, V> from a file.
    * @param file : The file from which the HashMap will be loaded.
    * @return Returns a HashMap that was saved in the file.
    */
    @SuppressWarnings("unchecked")
    public <K, V> HashMap<K, V> loadHashMapFrom(File file) {
        HashMap<K, V> result = null;
        ObjectInputStream ois = null;
       
        try {
            ois = new ObjectInputStream(new FileInputStream(file));
            result = (HashMap<K, V>) ois.readObject();
            ois.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            try {
                ois.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
       
        return result;
    }
}