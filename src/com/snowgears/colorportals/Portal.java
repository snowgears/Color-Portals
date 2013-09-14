package com.snowgears.colorportals;

import java.io.Serializable;
import java.util.ArrayList;

import org.bukkit.Location;

import com.snowgears.colorportals.utils.SerializableLocation;

public class Portal implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = -3987723258912027903L;
	private SerializableLocation location = null;
	private SerializableLocation signLocation = null;
	
	private ArrayList<SerializableLocation> frameLocations = new ArrayList<SerializableLocation>();
	private String name = "";
	private Integer channel = -1;
	private Integer node = -1;
	private String color = "";
	private Portal linkedPortal = null;
	private boolean isServerPortal = false;
	
	//  NAME, CHANNEL, LOCATION, COLOR, SIGN-LOCATION
	public Portal(String n, Integer i, SerializableLocation loc, String col, SerializableLocation signLoc, ArrayList<SerializableLocation> frame, boolean server){ 
		name = n;
		channel = i;
		location = loc;
		color = col;
		signLocation = signLoc;
		frameLocations = frame;
		isServerPortal = server;
	}
	
	public Location getLocation(){
		return location.deserialize();
	}
	
	public Location getSignLocation(){
		return signLocation.deserialize();
	}
	
	public ArrayList<Location> getFrameLocations(){
		ArrayList<Location> frame = new ArrayList<Location>();
		for(SerializableLocation sl : frameLocations){
			frame.add(sl.deserialize());
		}
		return frame;
	}
	
	public String getName(){
		return name;
	}
	
	public Integer getChannel(){
		return channel;
	}
	
	public Integer getNode(){
		return node;
	}
	
	public String getColor(){
		return color;
	}
	
	public Portal getLinkedPortal(){
		return linkedPortal;
	}
	
	public boolean isServerPortal(){
		return isServerPortal;
	}
	
	public void setNode(int n){
		node = n;
	}
	
	public void setLinkedPortal(Portal p){
		linkedPortal = p;
	}
	
}
