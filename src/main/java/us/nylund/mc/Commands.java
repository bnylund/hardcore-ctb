package us.nylund.mc;

import org.bukkit.command.CommandExecutor;

import java.io.File;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

import us.nylund.mc.Game.GameManager;
import us.nylund.mc.Game.LifestoneListeners;
import us.nylund.mc.Game.LootboxManager;
import us.nylund.mc.Items.Flamethrower;
import us.nylund.mc.Items.TNTLauncher;
import us.nylund.mc.Team.TeamColor;
import us.nylund.mc.Team.TeamManager;
import us.nylund.mc.Util.GameSave;
import us.nylund.mc.Util.SaveManager;
import us.nylund.mc.Util.XPBottle;

public class Commands implements CommandExecutor {
  @Override
  public boolean onCommand(CommandSender cs, Command cmd, String label, String[] args) {
    if (cs instanceof Player) {
      if (label.equalsIgnoreCase("test") && cs.isOp()) {
        Player p = (Player) cs;
        if (args.length > 0) {
          TeamColor team = TeamColor.valueOf(args[0]);
          p.getInventory().addItem(CustomItem.getLifestone(team));
        } else {
          // p.sendMessage("Cloning");
          // String name = CTB.pl.getConfig().getString("CloneWorld");
          // CTB.mv.cloneWorld(name, name + "_live", "VoidGen");
          p.getInventory().addItem(Flamethrower.getFlamethrower());
          p.getInventory().addItem(TNTLauncher.getLauncher());
        }
      } else if (label.equalsIgnoreCase("setlobby") && cs.isOp()) {
        Player p = (Player) cs;

        CTB.pl.getConfig().set("Lobby.world", p.getLocation().getWorld().getName());
        CTB.pl.getConfig().set("Lobby.x", p.getLocation().getX());
        CTB.pl.getConfig().set("Lobby.y", p.getLocation().getY());
        CTB.pl.getConfig().set("Lobby.z", p.getLocation().getZ());
        CTB.pl.getConfig().set("Lobby.yaw", p.getLocation().getYaw());
        CTB.pl.getConfig().set("Lobby.pitch", p.getLocation().getPitch());
        CTB.pl.saveConfig();

        p.sendMessage(ChatColor.GREEN + "Lobby location saved!");
      } else if (label.equalsIgnoreCase("setcloneworld") && cs.isOp()) {
        Player p = (Player) cs;

        CTB.pl.getConfig().set("CloneWorld", p.getLocation().getWorld().getName());
        CTB.pl.saveConfig();

        p.sendMessage(ChatColor.GREEN + "Clone world saved!");
      } else if (label.equalsIgnoreCase("setstreamermode")) {
        Player p = (Player) cs;

        if (args.length == 0) {
          p.sendMessage(ChatColor.RED + "Specify either true or false.");
          return true;
        }

        boolean state = false;
        try {
          state = Boolean.parseBoolean(args[0]);
        } catch (Exception ex) {
          p.sendMessage(ChatColor.RED + "Specify either true or false.");
          return true;
        }

        CTB.pl.getConfig().set(p.getUniqueId().toString() + ".streamer", state);
        CTB.pl.saveConfig();

        if (state)
          p.sendMessage(ChatColor.GREEN + "Streamer mode activated!");
        else
          p.sendMessage(ChatColor.RED + "Streamer mode deactivated.");
      } else if (label.equalsIgnoreCase("start") && cs.isOp()) {
        if (CTB.gm.started) {
          cs.sendMessage(ChatColor.RED + "Game already started!");
          return true;
        }

        CTB.gm.start();
      } else if (label.equalsIgnoreCase("load") && cs.isOp()) {
        if (CTB.gm.started) {
          cs.sendMessage(ChatColor.RED + "Game already started!");
          return true;
        }

        if (SaveManager.load()) {
          cs.sendMessage(ChatColor.GREEN + "Loaded!");
        } else {
          cs.sendMessage(ChatColor.RED + "Failed to load save.json.");
        }
      } else if (label.equalsIgnoreCase("stopgame") && cs.isOp()) {
        if (!CTB.gm.started) {
          cs.sendMessage(ChatColor.RED + "A game hasn't started yet!");
          return true;
        }

        CTB.gm.stop();
      } else if (label.equalsIgnoreCase("save") && cs.isOp()) {
        if (!CTB.gm.started) {
          cs.sendMessage(ChatColor.RED + "A game hasn't started yet!");
          return true;
        }

        if (SaveManager.save()) {
          cs.sendMessage(ChatColor.GREEN + "Saved!");
        } else {
          cs.sendMessage(ChatColor.RED + "Failed to save.");
        }
      } else if (label.equalsIgnoreCase("clean") && cs.isOp()) {
        if (CTB.gm.started) {
          cs.sendMessage(ChatColor.RED + "A game is currently running!");
          return true;
        }

        List<String> worlds = CTB.pl.getConfig().getStringList("Worlds");
        for (String s : worlds)
          CTB.mv.deleteWorld(s);

        CTB.pl.getConfig().set("Worlds", null);
        CTB.pl.saveConfig();

        File f = new File(CTB.pl.getDataFolder(), "save.json");
        if (f.exists()) {
          f.delete();
        }

        HandlerList.unregisterAll(CTB.pl);

        CTB.wb = new WorldBorder(CTB.pl.getConfig().getString("WorldBorder.world"),
            CTB.pl.getConfig().getInt("WorldBorder.lowX"), CTB.pl.getConfig().getInt("WorldBorder.lowZ"),
            CTB.pl.getConfig().getInt("WorldBorder.highX"), CTB.pl.getConfig().getInt("WorldBorder.highZ"), false);
        CTB.gm = new GameManager();
        CTB.tm = new TeamManager();
        CTB.cl = new CombatLog(false);
        CTB.lm = new LootboxManager();
        CTB.gs = new GameSave();

        CTB.tm.register();

        CTB.pm.registerEvents(CTB.gm, CTB.pl);
        CTB.pm.registerEvents(CTB.wb, CTB.pl);
        CTB.pm.registerEvents(CTB.cl, CTB.pl);
        CTB.pm.registerEvents(CTB.lm, CTB.pl);
        CTB.pm.registerEvents(new XPBottle(), CTB.pl);
        CTB.pm.registerEvents(new Listeners(), CTB.pl);
        CTB.pm.registerEvents(new Flamethrower(), CTB.pl);
        CTB.pm.registerEvents(new TNTLauncher(), CTB.pl);
        CTB.pm.registerEvents(new LifestoneListeners(), CTB.pl);
        CTB.pm.registerEvents((CTB) CTB.pl, CTB.pl);

        CTB.GAMEPLAY_TIMER_MULTIPLIER = CTB.pl.getConfig().getDouble("TimeModifier");

        cs.sendMessage(ChatColor.GREEN + "Old worlds and save deleted!");
      } else if (label.equalsIgnoreCase("drop") && cs.isOp()) {
        CTB.lm.doDrop((char) 2);
      }
    }
    return true;
  }

}
