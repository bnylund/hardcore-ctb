package us.nylund.mc.Events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import us.nylund.mc.Game.Lifestone;

public class LifestonePlaceEvent extends Event {
  private static final HandlerList HANDLERS = new HandlerList();
  private final Lifestone ls;

  public static HandlerList getHandlerList() {
    return HANDLERS;
  }

  public LifestonePlaceEvent(Lifestone lifestone) {
    this.ls = lifestone;
  }

  @Override
  public HandlerList getHandlers() {
    return HANDLERS;
  }

  public Lifestone getLifestone() {
    return this.ls;
  }
}
