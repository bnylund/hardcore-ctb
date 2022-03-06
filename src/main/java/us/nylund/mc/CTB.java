package us.nylund.mc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

import com.coloredcarrot.api.sidebar.Sidebar;
import com.coloredcarrot.api.sidebar.SidebarString;
import com.onarandombox.MultiverseCore.MultiverseCore;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Scoreboard;

import us.nylund.mc.Discord.Bot;
import us.nylund.mc.Game.GameManager;
import us.nylund.mc.Game.LifestoneListeners;
import us.nylund.mc.Game.LootboxManager;
import us.nylund.mc.Items.Flamethrower;
import us.nylund.mc.Items.TNTLauncher;
import us.nylund.mc.Team.Team;
import us.nylund.mc.Team.TeamColor;
import us.nylund.mc.Team.TeamManager;
import us.nylund.mc.Util.ClickableInventory;
import us.nylund.mc.Util.GameSave;
import us.nylund.mc.Util.SaveManager;
import us.nylund.mc.Util.XPBottle;

public class CTB extends JavaPlugin implements Listener {
  public static Plugin pl;
  public static PluginManager pm;
  public static String prefix = ChatColor.translateAlternateColorCodes('&', "&a[&b&lCTB&r&a] &9");
  public static Sidebar main;
  public static Bot bot;
  public static TeamManager tm;
  public static WorldBorder wb;
  public static GameManager gm;
  public static CombatLog cl;
  public static LootboxManager lm;
  public static MultiverseCore mv;
  public static GameSave gs;
  public static double GAMEPLAY_TIMER_MULTIPLIER;

  static {
    GAMEPLAY_TIMER_MULTIPLIER = 1D;
  }

  public void onEnable() {
    pl = this;
    pm = getServer().getPluginManager();

    if (!pm.isPluginEnabled("Multiverse-Core")) {
      getLogger().log(Level.SEVERE, "Multiverse-Core not loaded! disabling.");
      pm.disablePlugin(this);
      return;
    }

    if (!pm.isPluginEnabled("WorldBorder")) {
      getLogger().log(Level.SEVERE, "Multiverse-Core not loaded! disabling.");
      pm.disablePlugin(this);
      return;
    }

    CTB.mv = (MultiverseCore) pm.getPlugin("Multiverse-Core");
    getLogger().log(Level.INFO, "Multiverse hooked!");
    for (final Player p : Bukkit.getOnlinePlayers()) {

      p.setCollidable(true);
      p.setCanPickupItems(true);
      p.setSilent(false);
      p.setSleepingIgnored(false);

      p.setAllowFlight(false);
      p.setFlying(false);
      p.setInvisible(false);
      p.setInvulnerable(false);
      p.setGameMode(GameMode.SURVIVAL);

      if (!pl.getConfig().contains(p.getUniqueId().toString() + ".name"))
        pl.getConfig().set(p.getUniqueId().toString() + ".name", p.getName());
    }

    if (!pl.getConfig().contains("WorldBorder")) {
      pl.getConfig().set("WorldBorder.world", "world");
      pl.getConfig().set("WorldBorder.lowX", -100);
      pl.getConfig().set("WorldBorder.lowZ", -100);
      pl.getConfig().set("WorldBorder.highX", 100);
      pl.getConfig().set("WorldBorder.highZ", 100);
    }

    if (!pl.getConfig().contains("Lobby")) {
      pl.getConfig().set("Lobby.world", "lobby");
      pl.getConfig().set("Lobby.x", 0.5D);
      pl.getConfig().set("Lobby.y", 67D);
      pl.getConfig().set("Lobby.z", 0.5D);
      pl.getConfig().set("Lobby.yaw", -90D);
      pl.getConfig().set("Lobby.pitch", 0D);
    }

    if (!pl.getConfig().contains("CloneWorld")) {
      pl.getConfig().set("CloneWorld", "survival");
    }

    if (!pl.getConfig().contains("TimerModifier")) {
      pl.getConfig().set("TimerModifier", 1.0D);
    }

    if (!pl.getConfig().contains("Discord")) {
      pl.getConfig().set("Discord.Guild", "947323811815104512");
      pl.getConfig().set("Discord.NotificationChannel", "947615526895636551");
      pl.getConfig().set("Discord.CommandChannels", Arrays.asList("947615526895636551"));
    }

    if (!pl.getConfig().contains("verified")) {
      pl.getConfig().set("verified", new ArrayList<String>());
    }
    if (!pl.getConfig().contains("BotToken")) {
      pl.getConfig().set("BotToken", "TOKEN_HERE");
    }

    pl.saveConfig();

    // CTB.GAMEPLAY_TIMER_MULTIPLIER = pl.getConfig().getDouble("TimerModifier");

    // getLogger().log(Level.INFO, "Timer multiplier: " +
    // (CTB.GAMEPLAY_TIMER_MULTIPLIER));

    getLogger().log(Level.INFO, "Initializing managers");

    wb = new WorldBorder(pl.getConfig().getString("WorldBorder.world"), pl.getConfig().getInt("WorldBorder.lowX"),
        pl.getConfig().getInt("WorldBorder.lowZ"), pl.getConfig().getInt("WorldBorder.highX"),
        pl.getConfig().getInt("WorldBorder.highZ"), false);
    gm = new GameManager();
    tm = new TeamManager();
    cl = new CombatLog(false);
    lm = new LootboxManager();
    gs = new GameSave();

    tm.register();

    pm.registerEvents(gm, this);
    pm.registerEvents(wb, this);
    pm.registerEvents(cl, this);
    pm.registerEvents(lm, this);
    pm.registerEvents(new XPBottle(), this);
    pm.registerEvents(new Listeners(), this);
    pm.registerEvents(new Flamethrower(), this);
    pm.registerEvents(new TNTLauncher(), this);
    CTB.pm.registerEvents(new LifestoneListeners(), CTB.pl);
    pm.registerEvents(this, this);

    Commands cmds = new Commands();
    getCommand("test").setExecutor(cmds);
    getCommand("setlobby").setExecutor(cmds);
    getCommand("setcloneworld").setExecutor(cmds);
    getCommand("setstreamermode").setExecutor(cmds);
    getCommand("setstreamermode").setTabCompleter(new TabCompleter());
    getCommand("load").setExecutor(cmds);
    getCommand("start").setExecutor(cmds);
    getCommand("save").setExecutor(cmds);
    getCommand("clean").setExecutor(cmds);
    getCommand("stopgame").setExecutor(cmds);
    getCommand("drop").setExecutor(cmds);

    getLogger().log(Level.INFO, "Loading Discord Bot...");
    bot = new Bot();

    for (final Player p : Bukkit.getOnlinePlayers()) {
      p.setDisplayName(pl.getConfig().getString(p.getUniqueId().toString() + ".name"));
      p.setPlayerListHeaderFooter(ChatColor.AQUA + ChatColor.BOLD.toString() + "Skull Island",
          ChatColor.YELLOW + "https://discord.gg/GHAU98QTex");
      p.setPlayerListName(p.isOp()
          ? ChatColor.RED + ChatColor.BOLD.toString() + "MOD " + ChatColor.RESET.toString() + p.getDisplayName()
          : ChatColor.RESET.toString() + p.getDisplayName());
    }

    getLogger().log(Level.INFO, "Loading scoreboard...");

    Scoreboard sb = Bukkit.getScoreboardManager().getMainScoreboard();
    org.bukkit.scoreboard.Team t = sb.getTeam("nhide");
    if (t != null)
      t.unregister();

    for (TeamColor col : TeamColor.values())
      if (sb.getTeam(col.name()) != null)
        sb.getTeam(col.name()).unregister();

    main = new Sidebar(ChatColor.AQUA + ChatColor.BOLD.toString() + "     Skull Island     ", (Plugin) this, 20,
        new SidebarString[0]);

    updateScoreboard();

    getLogger().log(Level.INFO, "Loading global timers...");
    Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
      @Override
      public void run() {
        // Clickable Inventory Refresh
        for (Player p : Bukkit.getOnlinePlayers()) {
          if (p.getOpenInventory() != null && p.getOpenInventory().getTopInventory() instanceof ClickableInventory) {
            ClickableInventory ci = (ClickableInventory) p.getOpenInventory().getTopInventory();
            ci.refresh();
          }
        }

        bot.tick();
        cl.tick();
        gm.tick();
      }
    }, 0L, 20L);

    getLogger().log(Level.INFO, "Loaded CTB!");
  }

  public void onDisable() {

    getLogger().log(Level.INFO, "Shutting down CTB...");

    for (Player p : Bukkit.getOnlinePlayers()) {
      main.hideFrom(p);
      for (Team t : CTB.tm.teams) {
        t.sidebar.hideFrom(p);
      }

      p.setPlayerListName(p.getName());
      p.setPlayerListHeaderFooter(null, null);
    }

    CTB.gm.bars.forEach(x -> x.shutdown());

    CTB.lm.chests.forEach(x -> {
      x.loc.getBlock().setType(Material.AIR);
      if (x.as != null)
        x.as.remove();
      if (x.bar != null)
        x.bar.removeAll();
    });

    CTB.lm.noInteract.forEach(x -> {
      if (x != null)
        x.remove();
    });

    try {
      if (CTB.gm.started && !CTB.gm.complete) {
        SaveManager.save();
      }
    } catch (Exception ex) {
      ex.printStackTrace();
    }

    CTB.gm.sendAllToLobby();

    Bukkit.getScheduler().cancelTasks(this);
    bot.stop();
  }

  @EventHandler
  public void onMVDisable(PluginDisableEvent e) {
    if (e.getPlugin().getName().equals("Multiverse-Core")) {
      Bukkit.broadcastMessage(ChatColor.RED + ChatColor.BOLD.toString() + "[ERROR] " + ChatColor.YELLOW
          + "Multiverse-Core was disabled! Plugin shutting down.");
      getLogger().log(Level.SEVERE, "Multiverse-Core disabled! Disabling CTB.");
      pm.disablePlugin(this);
    }
    if (e.getPlugin().getName().equals("WorldBorder")) {
      Bukkit.broadcastMessage(ChatColor.RED + ChatColor.BOLD.toString() + "[ERROR] " + ChatColor.YELLOW
          + "WorldBorder was disabled! Plugin shutting down.");
      getLogger().log(Level.SEVERE, "WorldBorder disabled! Disabling CTB.");
      pm.disablePlugin(this);
    }
  }

  public static void updateScoreboard() {
    List<SidebarString> entries = new ArrayList<SidebarString>();
    entries.add(new SidebarString(new String(new char[entries.size()]).replace("\0", " ")));
    entries.add(new SidebarString(new String(new char[entries.size()]).replace("\0", " ")));
    entries.add(new SidebarString(ChatColor.YELLOW + ChatColor.BOLD.toString() + "Status"));
    entries.add(new SidebarString(ChatColor.WHITE + ChatColor.BOLD.toString() + "  WAITING"));
    entries.add(new SidebarString(new String(new char[entries.size()]).replace("\0", " ")));
    entries.add(new SidebarString(new String(new char[entries.size()]).replace("\0", " ")));
    entries.add(new SidebarString(ChatColor.YELLOW + "discord.gg/GHAU98QTex"));
    CTB.main.setEntries(entries);
  }

  @EventHandler
  public void onPlayerJoin(PlayerJoinEvent e) {
    if (!pl.getConfig().contains(e.getPlayer().getUniqueId().toString() + ".name"))
      pl.getConfig().set(e.getPlayer().getUniqueId().toString() + ".name", e.getPlayer().getName());
    pl.saveConfig();
    e.getPlayer().setDisplayName(pl.getConfig().getString(e.getPlayer().getUniqueId().toString() + ".name"));

    for (Player p : Bukkit.getOnlinePlayers())
      p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_HARP, 200.0F, 100.0F);
    e.setJoinMessage(
        ChatColor.GOLD + e.getPlayer().getDisplayName().toString() + ChatColor.RED.toString() + " just joined.");
    // e.getPlayer().setPlayerListHeaderFooter(ChatColor.AQUA +
    // ChatColor.BOLD.toString() + "Skull Island",
    // ChatColor.YELLOW + "https://discord.gg/GHAU98QTex");
    e.getPlayer().setPlayerListName(
        e.getPlayer().isOp()
            ? ChatColor.RED + ChatColor.BOLD.toString() + "MOD " + ChatColor.RESET.toString()
                + e.getPlayer().getDisplayName()
            : ChatColor.RESET.toString() + e.getPlayer().getDisplayName());
    main.showTo(e.getPlayer());
  }

  @EventHandler
  public void onPlayerLeave(PlayerQuitEvent e) {
    for (Player p : Bukkit.getOnlinePlayers())
      p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_HARP, 200.0F, 100.0F);
    e.setQuitMessage(
        ChatColor.GOLD + e.getPlayer().getDisplayName().toString() + ChatColor.RED.toString() + " just disconnected.");
  }
}
