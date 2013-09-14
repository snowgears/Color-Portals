package com.snowgears.colorportals.utils;

import java.io.Serializable;

import org.bukkit.Bukkit;
import org.bukkit.Location;

public class SerializableLocation implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2811064898616499901L;
	private final String world;
	private final double x, y, z;
	private final float yaw, pitch;
	
	public SerializableLocation(Location loc) {
	    world = loc.getWorld().getName();
	    x = loc.getX();
	    y = loc.getY();
	    z = loc.getZ();
	    yaw = loc.getYaw();
	    pitch = loc.getPitch();
	}
	
	public Location deserialize() {
        return new Location(Bukkit.getWorld(world), x, y, z, yaw, pitch);
    }
}
