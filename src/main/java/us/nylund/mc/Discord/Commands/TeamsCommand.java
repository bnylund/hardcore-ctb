package us.nylund.mc.Discord.Commands;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import discord4j.core.event.domain.message.MessageCreateEvent;
import us.nylund.mc.CTB;
import us.nylund.mc.Discord.Command;
import us.nylund.mc.Team.Team;

public class TeamsCommand implements Command {

  @Override
  public void execute(MessageCreateEvent event) {
    String msg = "Skull Island Teams\n\n";

    for (int i = 0; i < CTB.tm.teams.size(); i++) {
      Team team = CTB.tm.teams.get(i);

      if (team.playerCount() == 0)
        continue;

      msg += "<@&" + team.color().role().asString() + "> - ";

      if (team.playerNames().size() == 0)
        msg += "*Empty*";
      else {
        for (int y = 0; y < team.players.size(); y++) {
          OfflinePlayer pl = Bukkit.getOfflinePlayer(UUID.fromString(team.players.get(y)));
          if (team.out.contains(team.players.get(y))) {
            msg += "~~" + pl.getName() + "~~, ";
          } else {
            msg += pl.getName() + ", ";
          }
        }
        msg = msg.substring(0, msg.length() - 2);
      }

      msg += "\n    Kills: " + team.kills() + "\n    Lives: " + team.lives() + "\n";
    }

    if (!msg.contains(":")) {
      msg += "**No teams set.**";
    }

    event.getMessage().getChannel().block().createMessage(msg).withMessageReference(event.getMessage().getId()).block();
  }

}
