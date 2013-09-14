package com.snowgears.colorportals.events;

import org.bukkit.entity.Entity;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.snowgears.colorportals.Portal;

public class DestroyPortalEvent extends Event{

	private static final HandlerList handlers = new HandlerList();
    private Portal portal;
    private Entity entity;
 
    public DestroyPortalEvent(Portal p, Entity e) {
        portal = p;
        entity = e;
    }
 
    public Portal getPortal() {
        return portal;
    }
    
    public Entity getEntity() {
        return entity;
    }
 
    public HandlerList getHandlers() {
        return handlers;
    }
 
    public static HandlerList getHandlerList() {
        return handlers;
    }
    
}
