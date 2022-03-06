package us.nylund.mc;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

public class TabCompleter implements org.bukkit.command.TabCompleter {

  private static final String[] VALUES = { "true", "false" };

  @Override
  public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
    List<String> completions = new ArrayList<String>();

    if (args.length > 0)
      for (String s : VALUES)
        if (s.startsWith(args[0]))
          completions.add(s);

    return completions;
  }

}
