package us.nylund.mc.Team;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import com.coloredcarrot.api.sidebar.Sidebar;
import com.coloredcarrot.api.sidebar.SidebarString;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import us.nylund.mc.CTB;
import us.nylund.mc.CustomItem;
import us.nylund.mc.Events.TeamEliminatedEvent;
import us.nylund.mc.Game.Lifestone;
import us.nylund.mc.Util.SetExpFix;

public class Team implements Listener {
  private TeamColor color;
  public List<String> players;
  public List<String> out;
  public int lives;
  public int kills;
  public int moves;
  public transient Sidebar sidebar;
  public int timer;
  public int full;
  public boolean gaveLifestone;

  public Team(TeamColor color) {
    players = new ArrayList<String>();
    out = new ArrayList<String>();
    this.color = color;
    this.lives = 15;
    this.kills = 0;
    this.moves = 2;
    this.gaveLifestone = false;

    initSidebar();
    this.full = 60 * 60 * 12; // 12 hours
    this.timer = full;
  }

  public Team() {
    players = new ArrayList<String>();
    out = new ArrayList<String>();
  }

  public void initSidebar() {
    if (this.sidebar == null)
      this.sidebar = new Sidebar("      " + color.full() + " Team      ", CTB.pl, 20, new SidebarString[0]);
  }

  public Integer playerCount() {
    return players.size();
  }

  public TeamColor color() {
    return color;
  }

  public Integer kills() {
    return this.kills;
  }

  public Integer lives() {
    if (!hasPossession())
      return 0;

    // Minimum of 5 or the lives in the opponent stone
    Optional<Lifestone> ls = CTB.gm.lifestone(color);
    int lifestoneLives = 0;
    if (ls.isPresent()) {
      List<Lifestone> connected = ls.get().connected();
      for (int i = 0; i < connected.size(); i++) {
        Optional<Team> team = CTB.tm.getTeam(connected.get(i).team());
        if (team.isPresent()) {
          if (team.get().lives <= 5)
            lifestoneLives += team.get().lives;
          else
            lifestoneLives += 5;
        }
      }
    }

    return lifestoneLives + lives;
  }

  public boolean hasPossession() {
    // Check block
    Optional<Lifestone> ls = CTB.gm.lifestone(color);
    if (ls.isPresent() && ls.get().team().equals(color) && ls.get().placed().equals(color))
      return true;

    return false;
  }

  public List<String> playerUUIDs() {
    return players;
  }

  public void setPlayers(List<String> players) {
    this.players = players;
  }

  public List<Player> players() {
    return Bukkit.getOnlinePlayers().stream().filter(x -> players.contains(x.getUniqueId().toString()))
        .collect(Collectors.toList());
  }

  public List<String> playerNames() {
    return players.stream().map(x -> CTB.pl.getConfig().getString(x + ".name")).collect(Collectors.toList());
  }

  public void sendMessage(String msg) {
    players().forEach(x -> x.sendMessage(msg));
  }

  public void updateScoreboard() {
    if (this.sidebar == null)
      return;
    List<SidebarString> entries = new ArrayList<SidebarString>();
    entries.add(new SidebarString(new String(new char[entries.size()]).replace("\0", " ")));
    entries.add(new SidebarString(ChatColor.YELLOW + ChatColor.BOLD.toString() + "Lifestone Location"));

    Optional<Lifestone> ls = CTB.gm.lifestone(this.color);
    if (ls.isPresent() && ls.get().placed().equals(this.color())) {
      Location loc = ls.get().location();

      // Check to see if any players have streamer mode on first
      boolean hide = false;
      for (int i = 0; i < players.size(); i++)
        hide = hide || CTB.pl.getConfig().getBoolean(players.get(i) + ".streamer", false);

      if (hide)
        entries.add(new SidebarString(ChatColor.WHITE + ChatColor.BOLD.toString() + "  Hidden"));
      else
        entries.add(new SidebarString(ChatColor.WHITE + ChatColor.BOLD.toString() + "  " + loc.getBlockX() + " "
            + loc.getBlockY() + " " + loc.getBlockZ()));

      entries.add(new SidebarString(new String(new char[entries.size()]).replace("\0", " ")));
      entries.add(new SidebarString(ChatColor.YELLOW + ChatColor.BOLD.toString() + "Lifestone Radius"));
      entries.add(
          new SidebarString(ChatColor.WHITE + ChatColor.BOLD.toString() + "  " + ls.get().area.radius + " Blocks"));
    } else {
      // Check player inventories
      Player poss = null;
      for (Player p : players()) {
        for (ItemStack is : p.getInventory().getContents()) {
          if (is != null && this.color.equals(CustomItem.isLifestone(is))) {
            poss = p;
          }
        }
      }

      entries.add(new SidebarString(ChatColor.GOLD + ChatColor.WHITE.toString() + ChatColor.BOLD.toString() + "  "
          + (poss == null ? "Unknown" : poss.getName())));
    }

    entries.add(new SidebarString(new String(new char[entries.size()]).replace("\0", " ")));
    entries.add(new SidebarString(ChatColor.YELLOW + ChatColor.BOLD.toString() + "Lives"));
    entries.add(new SidebarString(ChatColor.WHITE + ChatColor.BOLD.toString() + "  " + this.lives()));

    entries.add(new SidebarString(new String(new char[entries.size()]).replace("\0", " ")));
    entries.add(new SidebarString(ChatColor.YELLOW + ChatColor.BOLD.toString() + "Players"));
    for (int i = 0; i < this.playerUUIDs().size(); i++) {
      OfflinePlayer pl = Bukkit.getOfflinePlayer(UUID.fromString(this.playerUUIDs().get(i)));
      if (!pl.isOnline())
        entries.add(new SidebarString(
            "  " + (out.contains(pl.getUniqueId().toString()) ? ChatColor.DARK_GRAY + ChatColor.STRIKETHROUGH.toString()
                : ChatColor.DARK_GRAY) + ChatColor.BOLD.toString() + pl.getName()));
      else
        entries.add(new SidebarString("  "
            + (out.contains(pl.getUniqueId().toString()) ? ChatColor.RED + ChatColor.STRIKETHROUGH.toString()
                : CTB.gm.inRespawn.contains(pl.getUniqueId().toString()) ? ChatColor.YELLOW : ChatColor.GREEN)
            + ChatColor.BOLD.toString() + pl.getName()));
    }

    entries.add(new SidebarString(new String(new char[entries.size()]).replace("\0", " ")));
    this.sidebar.setEntries(entries);

    // this.sidebar.update();
  }

  public void doDeath(Player p, boolean drop, Team cause) {
    if (!drop)
      p.playSound(p.getLocation(), Sound.ENTITY_BLAZE_DEATH, 100.0F, 100.0F);
    else {
      p.closeInventory();
      for (int i = 0; i < p.getInventory().getContents().length; i++) {
        if (p.getInventory().getContents()[i] != null)
          p.getWorld().dropItemNaturally(p.getLocation(), p.getInventory().getContents()[i]);
      }
      p.getInventory().clear();
      p.updateInventory();
    }

    p.setAllowFlight(true);
    p.setHealth(20);
    p.setFoodLevel(15);
    p.setAllowFlight(true);
    p.setInvisible(true);
    p.setInvulnerable(true);
    p.setCollidable(false);
    p.setCanPickupItems(false);
    p.setSilent(true);
    p.setSleepingIgnored(true);
    p.setVelocity(new Vector(0, 1, 0));
    p.setFireTicks(0);
    p.setGameMode(GameMode.SURVIVAL);

    CTB.cl.remove(p.getUniqueId().toString());

    Bukkit.getScheduler().scheduleSyncDelayedTask(CTB.pl, new Runnable() {
      @Override
      public void run() {
        if (p.isOnline()) {
          try {
            p.setFlying(true);
          } catch (Exception ex) {
          }
        }
      }
    }, 10L);

    if (lives() <= 0) {
      out.add(p.getUniqueId().toString());

      if (out.size() == players.size()) {
        TeamEliminatedEvent event = new TeamEliminatedEvent(this, cause);
        Bukkit.getPluginManager().callEvent(event);
      } else {
        Bukkit.getOnlinePlayers().forEach(x -> x.sendTitle(ChatColor.RED + "Player Eliminated!",
            this.color().chatcolor() + p.getName() + ChatColor.GOLD + " was killed.", 10, 70, 20));
        for (Player pl : Bukkit.getOnlinePlayers())
          pl.playSound(pl.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 100.0F, 100.0F);
      }

    } else {
      CTB.gm.inRespawn.add(p.getUniqueId().toString());
      out.remove(p.getUniqueId().toString());

      final int task = Bukkit.getScheduler().scheduleSyncRepeatingTask(CTB.pl, new Runnable() {
        int respawn = 10;

        @Override
        public void run() {
          p.sendTitle(ChatColor.RED + ChatColor.BOLD.toString() + "You died!",
              ChatColor.YELLOW + ChatColor.BOLD.toString() + "Respawning in " + respawn + " seconds...",
              respawn == 10 ? 10 : 0, 70, 20);
          respawn--;
        }
      }, 0L, 20L);

      this.lives--;
      Bukkit.getScheduler().scheduleSyncDelayedTask(CTB.pl, new Runnable() {
        @Override
        public void run() {
          if (p.isOnline() && CTB.gm.inRespawn.contains(p.getUniqueId().toString())) {
            CTB.gm.respawn(p);
          }
          if (task != -1)
            Bukkit.getScheduler().cancelTask(task);
          if (lives == 0) {
            players().forEach(x -> x.sendTitle(ChatColor.RED + ChatColor.BOLD.toString() + "No more lives remaining!",
                ChatColor.GOLD + "Use your last life wisely!", 10, 70, 20));
          }
        }
      }, 20 * 10);

    }
  }

  private void entDamage(EntityDamageByEntityEvent e) {
    if (e.getEntityType() == EntityType.PLAYER) {
      Player p = (Player) e.getEntity();
      if (p.getHealth() - e.getFinalDamage() <= 0 && players.contains(p.getUniqueId().toString())) {
        e.setCancelled(true);

        int xp = SetExpFix.getTotalExperience(p);
        if (xp > 5) {
          ItemStack bottle = CustomItem.getXPBottle(xp / 3, p.getDisplayName());
          p.getWorld().dropItem(p.getLocation(), bottle);
        }

        SetExpFix.setTotalExperience(p, 0);

        Team team = null;

        // Reward kill if applicable
        if (e.getDamager().getType() == EntityType.PLAYER) {
          Player dmg = (Player) e.getDamager();
          Optional<Team> teamOpt = CTB.tm.getPlayerTeam(dmg.getUniqueId().toString());
          if (teamOpt.isPresent()) {
            team = teamOpt.get();
            team.kills++;
            Bukkit.broadcastMessage(this.color().chatcolor() + p.getName() + ChatColor.GOLD + " was slain by "
                + team.color().chatcolor() + dmg.getName() + ChatColor.GOLD + "!");

            for (Player pl : Bukkit.getOnlinePlayers())
              pl.playSound(pl.getLocation(), Sound.ENTITY_BLAZE_DEATH, 100.0F, 100.0F);

            doDeath(p, true, team);
            return;
          }
        }

        // Check for tnt launcher
        if (e.getDamager() instanceof TNTPrimed) {
          TNTPrimed pro = (TNTPrimed) e.getDamager();
          if (pro.getSource() instanceof Player) {
            Player dmg = (Player) pro.getSource();
            Optional<Team> teamOpt = CTB.tm.getPlayerTeam(dmg.getUniqueId().toString());
            if (teamOpt.isPresent()) {
              team = teamOpt.get();
              team.kills++;
              Bukkit.broadcastMessage(this.color().chatcolor() + p.getName() + ChatColor.GOLD + " was slain by "
                  + team.color().chatcolor() + dmg.getName() + ChatColor.GOLD + "!");

              for (Player pl : Bukkit.getOnlinePlayers())
                pl.playSound(pl.getLocation(), Sound.ENTITY_BLAZE_DEATH, 100.0F, 100.0F);

              doDeath(p, true, team);
              return;
            }
          }
        }

        // Check arrows, snowballs, etc
        if (e.getDamager() instanceof Projectile) {
          Projectile pro = (Projectile) e.getDamager();
          if (pro.getShooter() instanceof Player) {
            Player dmg = (Player) pro.getShooter();
            Optional<Team> teamOpt = CTB.tm.getPlayerTeam(dmg.getUniqueId().toString());
            if (teamOpt.isPresent()) {
              team = teamOpt.get();
              team.kills++;
              Bukkit.broadcastMessage(this.color().chatcolor() + p.getName() + ChatColor.GOLD + " was slain by "
                  + team.color().chatcolor() + dmg.getName() + ChatColor.GOLD + "!");

              for (Player pl : Bukkit.getOnlinePlayers())
                pl.playSound(pl.getLocation(), Sound.ENTITY_BLAZE_DEATH, 100.0F, 100.0F);

              doDeath(p, true, team);
              return;
            }
          }
        }

        Bukkit.broadcastMessage(this.color().chatcolor() + p.getName() + ChatColor.GOLD + " died!");

        for (Player pl : Bukkit.getOnlinePlayers())
          pl.playSound(pl.getLocation(), Sound.ENTITY_BLAZE_DEATH, 100.0F, 100.0F);

        doDeath(p, true, team);
      }
    }
  }

  @EventHandler
  public void damage(EntityDamageEvent e) {
    if (e instanceof EntityDamageByEntityEvent) {
      entDamage((EntityDamageByEntityEvent) e);
    } else {
      if (e.getEntityType() == EntityType.PLAYER) {
        Player p = (Player) e.getEntity();
        if (p.getHealth() - e.getFinalDamage() <= 0 && players.contains(p.getUniqueId().toString())) {
          e.setCancelled(true);

          int xp = SetExpFix.getTotalExperience(p);
          if (xp > 5) {
            ItemStack bottle = CustomItem.getXPBottle(xp / 3, p.getDisplayName());
            p.getWorld().dropItem(p.getLocation(), bottle);
          }

          SetExpFix.setTotalExperience(p, 0);
          doDeath(p, true, null);

          Bukkit.broadcastMessage(this.color().chatcolor() + p.getName() + ChatColor.GOLD + " died!");
          for (Player pl : Bukkit.getOnlinePlayers())
            pl.playSound(pl.getLocation(), Sound.ENTITY_BLAZE_DEATH, 100.0F, 100.0F);
        }
      }
    }
  }
}
