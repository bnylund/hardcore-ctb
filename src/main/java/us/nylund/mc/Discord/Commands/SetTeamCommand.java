package us.nylund.mc.Discord.Commands;

import java.util.List;
import java.util.Optional;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Role;
import us.nylund.mc.CTB;
import us.nylund.mc.Discord.Command;
import us.nylund.mc.Team.Team;

public class SetTeamCommand implements Command {

  @Override
  public void execute(MessageCreateEvent event) {
    Role highest = event.getMember().get().getHighestRole().block();
    if (!(highest != null & highest.getId().asString().equals("947324882427998228"))) {
      return;
    }

    List<Snowflake> roles = event.getMessage().getRoleMentionIds();
    if (roles.size() == 0) {
      event.getMessage().getChannel().block().createMessage("No team role specified.")
          .withMessageReference(event.getMessage().getId()).block();
      return;
    }

    List<Snowflake> players = event.getMessage().getUserMentionIds();
    if (players.size() == 0) {
      event.getMessage().getChannel().block().createMessage("No players specified.")
          .withMessageReference(event.getMessage().getId()).block();
      return;
    }

    char ret = CTB.tm.setTeam(players, roles.get(0));
    if (ret == 0) {
      Optional<Team> team = CTB.tm.getTeamByRole(roles.get(0));
      event.getMessage().getChannel().block()
          .createMessage(team.get().color().string() + " Team set! [" + team.get().playerCount() + " verified players]")
          .withMessageReference(event.getMessage().getId()).block();
      return;
    } else if (ret == 1) {
      event.getMessage().getChannel().block().createMessage("Team not valid.")
          .withMessageReference(event.getMessage().getId()).block();
      return;
    } else if (ret == 2) {
      event.getMessage().getChannel().block()
          .createMessage("All players specified are either already on a team or aren't verified.")
          .withMessageReference(event.getMessage().getId()).block();
      return;
    }
  }

}
