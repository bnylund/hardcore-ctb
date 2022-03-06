package us.nylund.mc.Events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import us.nylund.mc.Team.Team;

public class TeamEliminatedEvent extends Event {
  private static final HandlerList HANDLERS = new HandlerList();
  private final Team team;
  private final Team cause;

  public static HandlerList getHandlerList() {
    return HANDLERS;
  }

  public TeamEliminatedEvent(Team team, Team cause) {
    this.team = team;
    this.cause = cause;
  }

  @Override
  public HandlerList getHandlers() {
    return HANDLERS;
  }

  public Team getEliminatedTeam() {
    return this.team;
  }

  // Returns the team that knocked out the eliminated team, otherwise null
  public Team getTeam() {
    return this.cause;
  }
}
