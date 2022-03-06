package us.nylund.mc.Discord.Commands;

import java.util.List;
import java.util.UUID;

import com.james090500.APIManager.UserInfo;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.spec.MessageEditSpec;
import us.nylund.mc.CTB;
import us.nylund.mc.Discord.Command;

public class LinkCommand implements Command {

  @Override
  public void execute(MessageCreateEvent event) {
    String arg = event.getMessage().getContent().replaceFirst("!link", "").trim();
    if (arg.length() > 1) {
      String[] args = arg.split(" ");
      String username = args[0];
      Message msg = event.getMessage().getChannel().block().createMessage("Checking username '" + username + "'")
          .withMessageReference(event.getMessage().getId()).block();

      List<String> knownUUIDs = CTB.pl.getConfig().getStringList("verified");
      for (int i = 0; i < knownUUIDs.size(); i++) {
        String discord = CTB.pl.getConfig().getString(knownUUIDs.get(i) + ".discord");
        String name = CTB.pl.getConfig().getString(knownUUIDs.get(i) + ".name");

        if (discord != null && discord.equals(event.getMember().get().getId().asString())) {
          CTB.pl.getConfig().set(knownUUIDs.get(i) + ".discord", null);
        }

        if (name != null && name.equals(username) && discord != null) {
          msg.edit(MessageEditSpec.builder().contentOrNull("Username already verified!").build()).block();
          return;
        }
      }

      try {
        OfflinePlayer p = Bukkit.getOfflinePlayer(UUID.fromString(UserInfo.getParsedUUID(args[0])));
        if (p != null && p.getUniqueId() != null) {
          List<String> verified = CTB.pl.getConfig().getStringList("verified");
          if (!verified.contains(p.getUniqueId().toString()))
            verified.add(p.getUniqueId().toString());
          CTB.pl.getConfig().set("verified", verified);

          CTB.pl.getConfig().set(p.getUniqueId().toString() + ".name", username);
          CTB.pl.getConfig().set(p.getUniqueId().toString() + ".discord", event.getMember().get().getId().asString());

          if (p.isOnline()) {
            Player pl = (Player) p;
            pl.setDisplayName(username);
            pl.setPlayerListHeaderFooter(ChatColor.AQUA + ChatColor.BOLD.toString() + "Skull Island",
                ChatColor.YELLOW + "https://discord.gg/GHAU98QTex");
            pl.setPlayerListName(p.isOp()
                ? ChatColor.RED + ChatColor.BOLD.toString() + "MOD " + ChatColor.RESET.toString() + pl.getDisplayName()
                : ChatColor.RESET.toString() + pl.getDisplayName());
          }

          CTB.pl.saveConfig();
          msg.edit(MessageEditSpec.builder().contentOrNull("Verified " + username + "!").build()).block();
        } else {
          msg.edit(MessageEditSpec.builder().contentOrNull("Failed to fetch user: " + username + "!").build()).block();
        }
      } catch (Exception ex) {
        msg.edit(MessageEditSpec.builder().contentOrNull("Failed to fetch user: " + username + "!").build()).block();
      }
    } else {
      event.getMessage().getChannel().block().createMessage("Please specify a username.").block();
    }
  }

}
