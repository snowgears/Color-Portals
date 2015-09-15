package com.snowgears.colorportals.events;

import com.snowgears.colorportals.Portal;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class CreatePortalEvent extends Event {

    private static final HandlerList handlers = new HandlerList();
    private Portal portal;
    private Player player;

    public CreatePortalEvent(Portal p, Player player) {
        portal = p;
        this.player = player;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    public Portal getPortal() {
        return portal;
    }

    public Player getPlayer() {
        return player;
    }

    public HandlerList getHandlers() {
        return handlers;
    }

}
