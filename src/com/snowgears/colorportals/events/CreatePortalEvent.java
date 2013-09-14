package com.snowgears.colorportals.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.material.Sign;

import com.snowgears.colorportals.Portal;

public class CreatePortalEvent extends Event{

	private static final HandlerList handlers = new HandlerList();
    private Portal portal;
    private Player player;
    private Sign sign;
 
    public CreatePortalEvent(Portal p, Player play, Sign s) {
        portal = p;
        player = play;
        sign = s;
    }
 
    public Portal getPortal() {
        return portal;
    }
    
    public Player getPlayer() {
        return player;
    }
    
    public Sign getSign() {
        return sign;
    }
 
    public HandlerList getHandlers() {
        return handlers;
    }
 
    public static HandlerList getHandlerList() {
        return handlers;
    }
    
}
