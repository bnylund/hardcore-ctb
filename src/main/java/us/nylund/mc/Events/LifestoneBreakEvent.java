package us.nylund.mc.Events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import us.nylund.mc.Game.Lifestone;
import us.nylund.mc.Team.TeamColor;

public class LifestoneBreakEvent extends Event {
  private static final HandlerList HANDLERS = new HandlerList();
  private final Lifestone ls;
  private final TeamColor team;

  public static HandlerList getHandlerList() {
    return HANDLERS;
  }

  public LifestoneBreakEvent(Lifestone lifestone, TeamColor team) {
    this.ls = lifestone;
    this.team = team;
  }

  @Override
  public HandlerList getHandlers() {
    return HANDLERS;
  }

  public Lifestone getLifestone() {
    return this.ls;
  }

  public TeamColor getTeam() {
    return this.team;
  }
}
