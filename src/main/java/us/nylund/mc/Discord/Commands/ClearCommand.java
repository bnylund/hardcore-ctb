package us.nylund.mc.Discord.Commands;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Role;
import us.nylund.mc.CTB;
import us.nylund.mc.Discord.Command;
import us.nylund.mc.Team.Team;

public class ClearCommand implements Command {

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

    Optional<Team> team = CTB.tm.getTeamByRole(roles.get(0));
    if (!team.isPresent()) {
      event.getMessage().getChannel().block().createMessage("Team role not valid.")
          .withMessageReference(event.getMessage().getId()).block();
      return;
    }

    team.get().setPlayers(new ArrayList<String>());
    event.getMessage().getChannel().block().createMessage("Team cleared!")
        .withMessageReference(event.getMessage().getId()).block();
  }

}
