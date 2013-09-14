package com.snowgears.colorportals.utils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import com.snowgears.colorportals.ColorPortals;
import com.snowgears.colorportals.Portal;

public class YamlHandler {
	
	public ColorPortals plugin = ColorPortals.plugin;
	
	public YamlHandler(ColorPortals instance)
    {
        plugin = instance;
    }
	
	public void savePortalsToFile(){
		File fileDirectory = new File(plugin.getDataFolder(), "Data");
		if(!fileDirectory.exists())
			fileDirectory.mkdir();
		File portalFile = new File(fileDirectory + "/newPortals.yml");
		if(! portalFile.exists()){ // file doesn't exist
			try {
				portalFile.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		ArrayList<Portal> portals = plugin.alisten.allPortals;
		YamlConfiguration config = YamlConfiguration.loadConfiguration(portalFile);
		for(Portal p : portals)
		{
			config.set(p.getName()+".Color", p.getColor());
			config.set(p.getName()+".Channel", p.getChannel());
			config.set(p.getName()+".Node", p.getNode());
			config.set(p.getName()+".Location", p.getLocation());
			config.set(p.getName()+".LinkedPortal", p.getLinkedPortal());
		}
		
		 try
		    {
		        config.save(portalFile);
		    }
		    catch(IOException ioe)
		    {
		        ioe.printStackTrace();
		    }
	}
	
//	String color = config.getString("GrassyWarp.Color");
//	int channel  = config.getInt("GrassyWarp.Channel");
//	int node = config.getInt("GrassyWarp.Node");
//	String linkedPortal = config.getString("GrassyWarp.LinkedPortal");
}
