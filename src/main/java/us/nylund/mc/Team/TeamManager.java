package us.nylund.mc.Team;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import discord4j.common.util.Snowflake;
import us.nylund.mc.CTB;

public class TeamManager {
  public List<Team> teams;

  public TeamManager() {
    teams = new ArrayList<Team>();

    teams.add(new Team(TeamColor.PURPLE));
    teams.add(new Team(TeamColor.RED));
    teams.add(new Team(TeamColor.GREEN));
    teams.add(new Team(TeamColor.YELLOW));
    teams.add(new Team(TeamColor.BLUE));
    teams.add(new Team(TeamColor.PINK));
    teams.add(new Team(TeamColor.ORANGE));
    teams.add(new Team(TeamColor.BLACK));
    teams.add(new Team(TeamColor.WHITE));
  }

  public void register() {
    teams.forEach(x -> CTB.pm.registerEvents(x, CTB.pl));
  }

  public boolean teamSet(Snowflake role) {
    Integer teamSize = CTB.pl.getConfig().getInt("TeamSize");
    return teams.stream().anyMatch(x -> {
      return x.color().role().equals(role) && x.playerCount() == teamSize;
    });
  }

  public Optional<Team> getTeamByRole(Snowflake role) {
    return teams.stream().filter(x -> x.color().role().equals(role)).findFirst();
  }

  public Optional<Team> getTeam(TeamColor color) {
    return teams.stream().filter(x -> x.color().equals(color)).findFirst();
  }

  public char setTeam(List<Snowflake> users, Snowflake teamRole) {
    Optional<Team> teamOpt = teams.stream().filter(x -> x.color().role().equals(teamRole)).findFirst();
    if (!teamOpt.isPresent())
      return 1;

    Team team = teamOpt.get();

    List<String> known = CTB.pl.getConfig().getStringList("verified");
    List<String> uuids = users.stream().map(x -> {
      for (int i = 0; i < known.size(); i++) {
        String discord = CTB.pl.getConfig().getString(known.get(i) + ".discord");
        if (discord != null && discord.equals(x.asString())) {
          // Check if player is already on a team
          if (!getPlayerTeam(known.get(i)).isPresent())
            return known.get(i);
        }
      }
      return null;
    }).filter(x -> x != null).collect(Collectors.toList());

    if (uuids.size() == 0) {
      return 2;
    }

    team.setPlayers(uuids);
    return 0;
  }

  public List<Team> getTeams() {
    return this.teams;
  }

  public Optional<Team> getPlayerTeam(String uuid) {
    return teams.stream().filter(x -> x.playerUUIDs().contains(uuid)).findFirst();
  }

  public List<Team> getActiveTeams() {
    Integer teamSize = CTB.pl.getConfig().getInt("TeamSize");
    return teams.stream().filter(x -> x.playerCount() == teamSize).collect(Collectors.toList());
  }

  public List<Team> getEmptyTeams() {
    return teams.stream().filter(x -> x.playerCount() == 0).collect(Collectors.toList());
  }
}
