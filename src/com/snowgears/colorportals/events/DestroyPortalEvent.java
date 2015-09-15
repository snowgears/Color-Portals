package com.snowgears.colorportals.events;

import com.snowgears.colorportals.Portal;
import org.bukkit.entity.Entity;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class DestroyPortalEvent extends Event {

    private static final HandlerList handlers = new HandlerList();
    private Portal portal;
    private Entity entity;

    public DestroyPortalEvent(Portal p, Entity e) {
        portal = p;
        entity = e;
    }

    public static HandlerList getHandlerList() {
        return handlers;
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

}
