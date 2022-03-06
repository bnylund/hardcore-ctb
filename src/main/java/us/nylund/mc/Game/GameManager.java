package us.nylund.mc.Game;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

import com.wimbli.WorldBorder.BorderData;
import com.wimbli.WorldBorder.Config;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.FireworkEffect;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.Particle.DustOptions;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team.Option;
import org.bukkit.scoreboard.Team.OptionStatus;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import us.nylund.mc.CTB;
import us.nylund.mc.CustomItem;
import us.nylund.mc.Events.LifestoneBreakEvent;
import us.nylund.mc.Events.LifestonePlaceEvent;
import us.nylund.mc.Events.TeamEliminatedEvent;
import us.nylund.mc.Team.Team;
import us.nylund.mc.Team.TeamColor;
import us.nylund.mc.Util.PlayerInfo;
import us.nylund.mc.Util.SaveLocation;
import us.nylund.mc.Util.SetExpFix;

public class GameManager implements Listener {
  public List<Lifestone> lifestones;
  public transient List<String> inRespawn;
  public transient List<Bossbar> bars;
  public transient boolean started;
  private transient double amplifier;
  public boolean complete;
  private double nextPhase;
  public String world;

  public GameManager() {
    lifestones = new ArrayList<Lifestone>();
    inRespawn = new ArrayList<String>();
    this.bars = new ArrayList<Bossbar>();
    this.started = false;
    this.complete = false;
    this.amplifier = CTB.GAMEPLAY_TIMER_MULTIPLIER == 0 ? 1 : CTB.GAMEPLAY_TIMER_MULTIPLIER;
    this.nextPhase = 60 * 60 * this.amplifier; // 60 minutes
    this.world = "";

    // Just ignore
    if ("1".equals("2"))
      scheduleTestTasks();
  }

  public void stop() {
    stop(null);
  }

  public void stop(String message) {
    CTB.tm.teams.forEach(x -> {
      Bukkit.getOnlinePlayers().forEach(y -> x.sidebar.hideFrom(y));
    });

    for (Bossbar bar : bars)
      bar.bar.removeAll();
    bars.clear();

    inRespawn.clear();

    for (Lifestone ls : lifestones)
      ls.destroy();
    lifestones.clear();

    CTB.cl.setEnabled(false);
    sendAllToLobby(message);
    started = false;
  }

  public void start() {
    // Register timers
    if (!this.started) {

      this.nextPhase = 60 * 60 * 1 * CTB.GAMEPLAY_TIMER_MULTIPLIER;
      this.complete = false;

      String sub = ChatColor.YELLOW + "Cloning island...";
      Bukkit.getOnlinePlayers().forEach(x -> {
        x.sendTitle(ChatColor.DARK_PURPLE + ChatColor.BOLD.toString() + "Preparing World", sub, 10, 20 * 12, 20);
      });

      String cworld = CTB.pl.getConfig().getString("CloneWorld");
      this.world = cworld + "_live_" + (System.currentTimeMillis() / (60 * 1000));
      if (!CTB.mv.cloneWorld(cworld, this.world, "VoidGen")) {
        Bukkit.getOnlinePlayers().forEach(x -> {
          x.sendTitle(ChatColor.RED + ChatColor.BOLD.toString() + "Couldn't start game.",
              ChatColor.YELLOW + "World failed to clone.", 0, 70, 20);
        });
        return;
      }

      // Use WorldBorder plugin to fill in chunks
      BorderData bd = new BorderData(0, 0, 1024, 512);
      bd.setShape(false);
      bd.setWrapping(false);
      Config.setBorder(this.world, bd);

      CTB.mv.getMVWorldManager().getMVWorld(world).setAutoLoad(false);
      CTB.mv.getMVWorldManager().saveWorldsConfig();

      List<String> worlds = CTB.pl.getConfig().getStringList("Worlds");
      worlds.add(this.world);

      CTB.pl.getConfig().set("Worlds", worlds);
      CTB.pl.saveConfig();

      Bukkit.getScheduler().scheduleSyncDelayedTask(CTB.pl, new Runnable() {
        @Override
        public void run() {
          startTimers(true);
        }
      }, 20L);
    }
  }

  public void startTimers(boolean isNew) {
    BukkitRunnable br = new BukkitRunnable() {
      int cd = 10;

      @Override
      public void run() {
        Bukkit.getOnlinePlayers().forEach(x -> {
          x.sendTitle(ChatColor.GREEN + ChatColor.BOLD.toString() + "Starting Game",
              ChatColor.YELLOW + "Teleporting to the island in " + cd + " seconds.", 0, 70, 20);
        });
        cd--;
        if (cd < 0) {
          World w = Bukkit.getWorld(world);
          for (Team team : CTB.tm.teams) {
            if (team.playerCount() > 0) {

              Location spawn = spawnLocation(w);
              spawn.add(0.5, 0, 0.5);
              team.players().forEach(x -> {

                Optional<PlayerInfo> pi = CTB.gs.players.stream().filter(y -> y.uuid.equals(x.getUniqueId().toString()))
                    .findFirst();

                if (pi.isPresent() && !isNew) {
                  pi.get().setInventory(x);
                  SetExpFix.setTotalExperience(x, pi.get().xp);
                  SaveLocation l = pi.get().location;
                  Location loc = new Location(Bukkit.getWorld(l.world), l.x, l.y, l.z, l.yaw, l.pitch);
                  x.teleport(loc);
                  inRespawn.remove(x.getUniqueId().toString());
                  CTB.gs.players.removeIf(y -> y.uuid.equals(x.getUniqueId().toString()));
                } else {
                  x.teleport(spawn);
                  x.getInventory().clear();
                  x.updateInventory();
                  inRespawn.remove(x.getUniqueId().toString());
                  SetExpFix.setTotalExperience(x, 0);
                  CTB.gs.players.removeIf(y -> y.uuid.equals(x.getUniqueId().toString()));
                }

                if (!team.gaveLifestone) {
                  x.getInventory().setItem(0, CustomItem.getLifestone(team.color()));
                  x.updateInventory();
                  team.gaveLifestone = true;
                }
              });
            }
          }
          for (Player p : getSpectators()) {
            Location spawn = spawnLocation(w);
            p.teleport(spawn);
          }

          started = true;

          CTB.cl.setEnabled(true);
          CTB.wb.setEnabled(true);
          CTB.wb.world = world;

          this.cancel();
        }
      }
    };

    br.runTaskTimer(CTB.pl, 5 * 20, 20L);
  }

  public void startTimers() {
    this.startTimers(false);
  }

  public List<Player> getSpectators() {
    return Bukkit.getOnlinePlayers().stream().filter(x -> !CTB.tm.getPlayerTeam(x.getUniqueId().toString()).isPresent())
        .collect(Collectors.toList());
  }

  public List<Lifestone> lifestones() {
    return this.lifestones;
  }

  public Optional<Lifestone> lifestone(TeamColor team) {
    return lifestones.stream().filter(x -> x.team().equals(team)).findFirst();
  }

  public List<Team> remaining() {
    List<Team> remaining = new ArrayList<Team>();
    for (int i = 0; i < CTB.tm.teams.size(); i++)
      if (CTB.tm.teams.get(i).out.size() < CTB.tm.teams.get(i).players.size())
        remaining.add(CTB.tm.teams.get(i));
    return remaining;
  }

  // OFFLINE HOURS: 10pm -> 10am
  public boolean isOfflineHours() {
    Calendar cd = Calendar.getInstance();
    int hr = cd.get(Calendar.HOUR_OF_DAY);
    return hr < 10 || hr > 22;
  }

  public void tick() {
    if (this.started) {
      CTB.tm.teams.forEach(team -> {

        // Check for timer eliminations
        Optional<Lifestone> ls = lifestone(team.color());
        if (ls.isPresent()) {
          ls.get().location().getBlock().setType(team.color().material());
        } else {
          if (team.players().size() == 0 && team.out.size() < team.playerCount() && !isOfflineHours()) {
            team.timer--;
            if (team.timer <= 0) {
              team.players.forEach(plid -> {
                if (!team.out.contains(plid))
                  team.out.add(plid);
              });
              TeamEliminatedEvent te = new TeamEliminatedEvent(team, null);
              Bukkit.getPluginManager().callEvent(te);
            }
          }

          if (!team.gaveLifestone && team.players().size() > 0) {
            Player hasLS = team.players().get(0);
            hasLS.getInventory().setItem(0, CustomItem.getLifestone(team.color()));
            hasLS.updateInventory();
            team.gaveLifestone = true;
          }
        }

        Scoreboard score = team.sidebar.getTheScoreboard();

        org.bukkit.scoreboard.Team t = score.getTeam(team.color().name());

        if (t == null) {
          t = score.registerNewTeam(team.color().name());
          t.setPrefix(team.color().chatcolor() + "");
          t.setOption(Option.NAME_TAG_VISIBILITY, OptionStatus.FOR_OTHER_TEAMS);
        }

        for (Player p : Bukkit.getOnlinePlayers()) {
          if (team.players.contains(p.getUniqueId().toString())) {
            t.addEntry(p.getName());
          }
        }

        for (Team t1 : CTB.tm.teams) {
          if (t1.color().equals(team.color()))
            continue;
          t = score.getTeam(t1.color().name());

          if (t == null) {
            t = score.registerNewTeam(t1.color().name());
            t.setPrefix(t1.color().chatcolor() + "");
            t.setOption(Option.NAME_TAG_VISIBILITY, OptionStatus.FOR_OTHER_TEAMS);
          }

          for (Player p : Bukkit.getOnlinePlayers()) {
            if (!team.players.contains(p.getUniqueId().toString())) {
              t.addEntry(p.getName());
            }
          }
        }
      });

      Bukkit.getOnlinePlayers().forEach(x -> {

        CTB.tm.teams.forEach(tm -> {
          if (!this.bars.stream()
              .anyMatch(bar -> bar.color.equals(tm.color()) && bar.player.equals(x.getUniqueId().toString()))) {
            this.bars.add(new Bossbar(x.getUniqueId().toString(), tm.color()));
          }
        });

        this.bars.forEach(bar -> bar.tick());

        Optional<Team> team = CTB.tm.getPlayerTeam(x.getUniqueId().toString());
        if (team.isPresent()) {

          if (this.started) {
            team.get().sidebar.showTo(x);
          } else {
            team.get().sidebar.hideFrom(x);
            CTB.main.showTo(x);
          }

          team.get().updateScoreboard();

          if (team.get().out.contains(x.getUniqueId().toString()) || inRespawn.contains(x.getUniqueId().toString())) {
            x.setAllowFlight(true);
            x.setHealth(20);
            x.setFoodLevel(20);
            x.setArrowsInBody(0);
            x.setFireTicks(0);
            x.setAllowFlight(true);
            x.setInvisible(true);
            x.setInvulnerable(true);
            x.setCollidable(false);
            x.setCanPickupItems(false);
            x.setSilent(true);
            x.setSleepingIgnored(true);
            x.getActivePotionEffects().clear();
          } else {
            x.setCollidable(true);
            x.setCanPickupItems(true);
            x.setSilent(false);
            x.setSleepingIgnored(false);

            x.setAllowFlight(false);
            x.setFlying(false);
            x.setInvisible(false);
            x.setInvulnerable(false);
            x.setGameMode(GameMode.SURVIVAL);
          }

        } else {
          // If not on a team, put in spectator mode
          CTB.tm.teams.forEach(tm -> {
            tm.sidebar.hideFrom(x);
          });

          x.setFireTicks(0);
          x.setArrowsInBody(0);
          x.setHealth(20);
          x.setFoodLevel(20);
          x.setAllowFlight(true);
          x.setAllowFlight(true);
          x.setInvisible(true);
          x.setInvulnerable(true);
          x.setCollidable(false);
          x.setCanPickupItems(false);
          x.setSilent(true);
          x.setSleepingIgnored(true);
          x.getActivePotionEffects().clear();
        }

        String header = ChatColor.DARK_GRAY + "==========[ " + ChatColor.AQUA + ChatColor.BOLD.toString()
            + "Skull Island " + ChatColor.DARK_GRAY + "]==========\n\n";
        if (team.isPresent()) {
          header += team.get().color().full() + " Team\n\n" + ChatColor.YELLOW + "Lives: " + team.get().lives
              + "\nKills: " + team.get().kills + "\n";

          Optional<Lifestone> ls = lifestone(team.get().color());
          if (ls.isPresent() && ls.get().placed().equals(team.get().color())
              && ls.get().team().equals(team.get().color())) {
            double seconds = ls.get().timer;
            double minutes = seconds / 60;
            header += "\n" + ChatColor.YELLOW + "Time until shrink: "
                + (minutes > 0 ? ((int) Math.floor(minutes)) + "m " : "") + ((int) Math.floor(seconds % 60)) + "s\n";
          }
        }

        String footer = "\n" + ChatColor.YELLOW + "discord.gg/GHAU98QTex\n\n" + ChatColor.DARK_GRAY
            + "==================================";
        x.setPlayerListHeaderFooter(header, footer);

        x.setPlayerListName((x.isOp() ? ChatColor.RED + ChatColor.BOLD.toString() + "MOD " : "") + (team.isPresent()
            ? inRespawn.contains(x.getUniqueId().toString()) ? ChatColor.DARK_GRAY
                : team.get().color().chatcolor()
                    + (team.get().out.contains(x.getUniqueId().toString()) ? ChatColor.STRIKETHROUGH.toString() : "")
            : ChatColor.WHITE.toString()) + x.getName());

      });

      // Reset nextPhase
      if (nextPhase <= 0) {
        if (CTB.lm.tier < 4) {
          CTB.lm.tier += 1;
          CTB.lm.placed = (char) 0;
        }
        nextPhase = 60 * 60 * amplifier;
      } else if (!isOfflineHours())
        nextPhase--;

      if (nextPhase / amplifier == 0) {
        if (CTB.lm.placed == 0) {
          CTB.lm.doDrop((char) (CTB.lm.tier + 1));
          CTB.lm.placed = (char) 1;
        } else {
          CTB.lm.doDrop(CTB.lm.tier);
        }

        nextPhase--;
      } else if (nextPhase / amplifier == 60 * 20) {
        if (CTB.lm.placed == 1) {
          CTB.lm.doDrop();
        } else {
          double sd = new Random().nextDouble();
          if (sd <= 0.5) {
            CTB.lm.doDrop((char) (CTB.lm.tier + 1));
            CTB.lm.placed = (char) 1;
          } else {
            CTB.lm.doDrop();
          }
        }
        nextPhase--;
      } else if (nextPhase / amplifier == 60 * 40) {
        if (CTB.lm.placed == 1) {
          CTB.lm.doDrop();
        } else {
          double sd = new Random().nextDouble();
          if (sd >= 0.666) {
            CTB.lm.doDrop((char) (CTB.lm.tier + 1));
            CTB.lm.placed = (char) 1;
          } else {
            CTB.lm.doDrop();
          }
        }
        nextPhase--;
      }

    } else {
      Bukkit.getOnlinePlayers().forEach(x -> {
        x.setCollidable(true);
        x.setCanPickupItems(true);
        x.setSilent(false);
        x.setSleepingIgnored(false);

        x.setAllowFlight(false);
        x.setFlying(false);
        x.setInvisible(false);
        x.setInvulnerable(false);

        x.setFireTicks(0);
        x.setArrowsInBody(0);
        x.setHealth(20);
        x.setFoodLevel(20);
        x.setGameMode(GameMode.SURVIVAL);
      });
    }
  }

  public void sendToLobby(Player p) {
    p.teleport(new Location(Bukkit.getWorld(CTB.pl.getConfig().getString("Lobby.world")),
        CTB.pl.getConfig().getDouble("Lobby.x"), CTB.pl.getConfig().getDouble("Lobby.y"),
        CTB.pl.getConfig().getDouble("Lobby.z"), (float) CTB.pl.getConfig().getDouble("Lobby.yaw"),
        (float) CTB.pl.getConfig().getDouble("Lobby.pitch")));
    p.getInventory().clear();
    SetExpFix.setTotalExperience(p, 0);
  }

  public void sendAllToLobby() {
    sendAllToLobby(null);
  }

  public void sendAllToLobby(String message) {
    CTB.cl.setEnabled(false);
    CTB.wb.setEnabled(false);
    Bukkit.getOnlinePlayers().forEach(x -> {
      if (message != null)
        x.sendMessage(message);
      x.sendMessage(ChatColor.YELLOW + "Teleporting to lobby...");
      sendToLobby(x);
    });
    this.started = false;
  }

  public Location spawnLocation(World world) {
    Random rd = new Random();
    int x = rd.nextInt(CTB.wb.getUpper().getBlockX() - CTB.wb.getLower().getBlockX()) + CTB.wb.getLower().getBlockX();
    int z = rd.nextInt(CTB.wb.getUpper().getBlockZ() - CTB.wb.getLower().getBlockZ()) + CTB.wb.getLower().getBlockZ();

    int y = 319;

    while (y >= world.getSeaLevel()) {
      if (world.getBlockAt(x, y, z).getType() != Material.AIR)
        break;
      y--;
    }

    if (!world.getBlockAt(x, y, z).getType().isSolid())
      return spawnLocation(world);

    Location loc = new Location(world, x, y + 1, z);
    Collection<Entity> ents = world.getNearbyEntities(loc, 100, 100, 100, ent -> {
      return ent.getType() == EntityType.PLAYER;
    });

    if (ents.size() > 0)
      return spawnLocation(world);

    return new Location(world, x, y, z);
  }

  public void respawn(Player p) {
    if (p.getBedSpawnLocation() != null && p.getBedSpawnLocation().getWorld().getName().equals(world)) {
      p.teleport(p.getBedSpawnLocation());
    } else {
      p.teleport(spawnLocation(p.getWorld()));
    }

    if (inRespawn.contains(p.getUniqueId().toString())) {
      inRespawn.remove(p.getUniqueId().toString());
    }

    SetExpFix.setTotalExperience(p, 0);
    p.setHealth(20);
    p.setFoodLevel(20);
    p.setCollidable(true);
    p.setCanPickupItems(true);
    p.setSilent(false);
    p.setSleepingIgnored(false);

    p.setAllowFlight(false);
    p.setFlying(false);
    p.setInvisible(false);
    p.setInvulnerable(false);
    p.setGameMode(GameMode.SURVIVAL);
  }

  @EventHandler
  public void teamEliminated(TeamEliminatedEvent e) {
    if (e.getTeam() != null) {
      e.getTeam().lives += e.getEliminatedTeam().lives < 5 ? e.getEliminatedTeam().lives : 5;
      e.getEliminatedTeam().lives = 0;

      int lives = e.getTeam().lives();
      List<String> toRemove = new ArrayList<String>();
      for (int i = 0; i < e.getTeam().out.size(); i++) {
        Player p = Bukkit.getPlayer(UUID.fromString(e.getTeam().out.get(i)));
        if (lives > 0 && p != null && p.isOnline()) {
          e.getTeam().doDeath(p, true, null);
          lives--;
          toRemove.add(p.getUniqueId().toString());
        }
      }
      for (int i = 0; i < toRemove.size(); i++) {
        e.getTeam().out.remove(toRemove.get(i));
      }
    }

    List<Team> remaining = remaining();
    if (remaining.size() == 0) {
      stop(ChatColor.RED + "No more players remaining?");
      complete = true;
    } else if (remaining.size() == 1) {
      complete = true;

      // Do team win stuff
      final Team winner = remaining.get(0);
      Bukkit.getOnlinePlayers().forEach(x -> {
        x.playSound(x.getLocation(), Sound.ENTITY_ENDER_DRAGON_DEATH, 200F, 200F);
        x.sendTitle(winner.color().full() + " Team", ChatColor.YELLOW + "won the game!", 20, 4 * 20, 20);
      });

      BukkitRunnable br = new BukkitRunnable() {
        int runs = 10;

        @Override
        public void run() {
          winner.players().forEach(x -> {
            Firework fw = (Firework) x.getWorld().spawnEntity(x.getLocation(), EntityType.FIREWORK);
            FireworkMeta fwm = fw.getFireworkMeta();

            fwm.setPower(3);
            fwm.addEffect(FireworkEffect.builder().withColor(winner.color().color()).flicker(true).trail(true).build());

            fw.setFireworkMeta(fwm);
          });

          runs--;
          if (runs < 0) {
            stop();
            this.cancel();
          }
        }
      };

      br.runTaskTimer(CTB.pl, 20L, 20L);
    } else {
      Bukkit.getOnlinePlayers().forEach(x -> x.sendTitle(ChatColor.RED + "Team Eliminated!",
          e.getEliminatedTeam().color().full() + " Team" + ChatColor.YELLOW + " was knocked out!", 10, 70, 20));
      for (Player pl : Bukkit.getOnlinePlayers())
        pl.playSound(pl.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 100.0F, 100.0F);
    }
  }

  @EventHandler
  public void lifestonePlace(LifestonePlaceEvent e) {
    if (this.started) {
      lifestones.add(e.getLifestone());
      e.getLifestone().start();

      Optional<Team> team = CTB.tm.getTeam(e.getLifestone().placed());

      if (team.isPresent()) {

        int lives = team.get().lives();
        for (int i = 0; i < team.get().out.size(); i++) {
          Player p = Bukkit.getPlayer(UUID.fromString(team.get().out.get(i)));
          if (lives > 0 && p != null && p.isOnline()) {
            team.get().doDeath(p, true, null);
            lives--;
          }
        }
      }
    }
  }

  @EventHandler
  public void lifestoneBreak(LifestoneBreakEvent e) {
    CTB.gm.lifestones.remove(e.getLifestone());
    e.getLifestone().destroy();

    if (this.started && !this.complete) {
      boolean doDrop = true;

      // If broken by team, decrement
      if (e.getLifestone().team().equals(e.getTeam())) {
        Optional<Team> team = CTB.tm.getTeam(e.getLifestone().team());
        if (team.isPresent()) {
          for (Player pl : Bukkit.getOnlinePlayers()) {
            pl.playSound(pl.getLocation(), Sound.ENTITY_WITHER_DEATH, 100.0F, 100.0F);
            pl.sendTitle(ChatColor.RED + "Lifestone Broken!",
                team.get().color().full() + " Team" + ChatColor.GOLD + " broke their lifestone!", 10, 70, 20);
          }
          Bukkit.broadcastMessage(team.get().color().full() + " Team" + ChatColor.YELLOW + " broke their lifestone!");

          if (team.get().moves > 0)
            team.get().moves--;
          else
            doDrop = false;
        }
      } else {
        Optional<Team> team = CTB.tm.getTeam(e.getLifestone().team());
        Optional<Team> tm = CTB.tm.getTeam(e.getTeam());
        if (team.isPresent() && tm.isPresent()) {
          for (Player pl : Bukkit.getOnlinePlayers()) {
            pl.playSound(pl.getLocation(), Sound.ENTITY_WITHER_DEATH, 100.0F, 100.0F);
            pl.sendMessage(tm.get().color().full() + " Team" + ChatColor.YELLOW + " stole " + team.get().color().full()
                + " Team" + ChatColor.YELLOW + "'s lifestone!");

            if (!team.get().players.contains(pl.getUniqueId().toString()))
              pl.sendTitle(ChatColor.RED + "Lifestone Broken!", tm.get().color().full() + " Team" + ChatColor.YELLOW
                  + " stole " + team.get().color().full() + " Team" + ChatColor.YELLOW + "'s lifestone!", 10, 70, 20);
          }

          team.get().players().forEach(x -> x.sendTitle(ChatColor.RED + "Lifestone Broken!",
              tm.get().color().full() + " Team" + ChatColor.GOLD + " stole your lifestone!", 10, 70, 20));
        }
      }

      if (doDrop) {
        ItemStack drop = CustomItem.getLifestone(e.getLifestone().team());
        e.getLifestone().location().getWorld().dropItemNaturally(e.getLifestone().location(), drop);
      }
    } else {
      ItemStack drop = CustomItem.getLifestone(e.getLifestone().team());
      e.getLifestone().location().getWorld().dropItemNaturally(e.getLifestone().location(), drop);
    }
  }

  @EventHandler
  public void lifestoneDamage(BlockDamageEvent e) {
    Optional<Lifestone> ls = Lifestone.getLifestone(e.getBlock().getLocation());
    if (ls.isPresent() && !this.complete) {
      Optional<Team> team = CTB.tm.getPlayerTeam(e.getPlayer().getUniqueId().toString());

      if (!team.isPresent()) {
        e.setCancelled(true);
        return;
      }

      if (ls.get().team().equals(team.get().color()) && team.get().moves <= 0) {
        e.getPlayer().spigot().sendMessage(ChatMessageType.ACTION_BAR,
            new TextComponent(ChatColor.RED + "You can't move your lifestone anymore!"));
        e.setCancelled(true);
        return;
      }
    }
  }

  @EventHandler
  public void leave(PlayerQuitEvent e) {
    Optional<Team> team = CTB.tm.getPlayerTeam(e.getPlayer().getUniqueId().toString());
    if (inRespawn.contains(e.getPlayer().getUniqueId().toString())) {
      // Check if team has > 0 lives. if so, respawn
      if (!team.isPresent() || team.get().lives() > 0) {
        respawn(e.getPlayer());
        inRespawn.remove(e.getPlayer().getUniqueId().toString());
        if (team.isPresent())
          team.get().lives--;
      }
    }

    if (this.started && team.isPresent() && !this.complete) {
      CTB.gs.players = CTB.gs.players.stream().filter(x -> !x.uuid.equals(e.getPlayer().getUniqueId().toString()))
          .collect(Collectors.toList());
      CTB.gs.players.add(new PlayerInfo(e.getPlayer().getInventory().getContents(), e.getPlayer().getLocation(),
          e.getPlayer().getUniqueId().toString(), SetExpFix.getTotalExperience(e.getPlayer())));
    }
  }

  @EventHandler
  public void join(PlayerJoinEvent e) {
    if (this.started && !e.getPlayer().getWorld().getName().equals(world)) {
      respawn(e.getPlayer());
    } else if (!this.started || this.complete) {
      e.getPlayer().sendMessage(ChatColor.YELLOW + "Teleporting to lobby...");
      e.getPlayer()
          .teleport(new Location(Bukkit.getWorld(CTB.pl.getConfig().getString("Lobby.world")),
              CTB.pl.getConfig().getDouble("Lobby.x"), CTB.pl.getConfig().getDouble("Lobby.y"),
              CTB.pl.getConfig().getDouble("Lobby.z"), (float) CTB.pl.getConfig().getDouble("Lobby.yaw"),
              (float) CTB.pl.getConfig().getDouble("Lobby.pitch")));
    }

    Optional<PlayerInfo> pi = CTB.gs.players.stream().filter(x -> x.uuid.equals(e.getPlayer().getUniqueId().toString()))
        .findFirst();

    if (pi.isPresent() && this.started && !this.complete) {
      pi.get().setInventory(e.getPlayer());
      e.getPlayer().updateInventory();
      SetExpFix.setTotalExperience(e.getPlayer(), pi.get().xp);
      SaveLocation l = pi.get().location;
      Location loc = new Location(Bukkit.getWorld(l.world), l.x, l.y, l.z, l.yaw, l.pitch);
      e.getPlayer().teleport(loc);
      CTB.gs.players.removeIf(y -> y.uuid.equals(e.getPlayer().getUniqueId().toString()));
    } else {
      if (pi.isPresent())
        CTB.gs.players.remove(pi.get());
      e.getPlayer().getInventory().clear();
      sendToLobby(e.getPlayer());
    }
  }

  @EventHandler
  public void onPortal(PlayerPortalEvent e) {
    e.setCanCreatePortal(false);
    e.setCancelled(true);
  }

  private boolean isAlive(Player p) {
    Optional<Team> team = CTB.tm.getPlayerTeam(p.getUniqueId().toString());
    return !(inRespawn.contains(p.getUniqueId().toString())
        || (team.isPresent() ? team.get().out.contains(p.getUniqueId().toString()) : true));
  }

  @EventHandler
  public void interact(PlayerInteractEvent e) {
    if (!this.started || !isAlive(e.getPlayer())) {
      e.setCancelled(true);
    }
  }

  @EventHandler
  public void pickup(EntityPickupItemEvent e) {
    if (!this.started || (e.getEntityType().equals(EntityType.PLAYER) && !isAlive((Player) e.getEntity())))
      e.setCancelled(true);
  }

  @EventHandler
  public void exp(EntityTargetLivingEntityEvent e) {
    if (!this.started || (e.getTarget() != null && e.getTarget().getType() == EntityType.PLAYER
        && !isAlive((Player) e.getTarget()))) {
      e.setCancelled(true);
      e.setTarget(null);
    }
  }

  @EventHandler
  public void breakBlock(BlockBreakEvent e) {
    if (!this.started || !isAlive(e.getPlayer()))
      e.setCancelled(true);
  }

  @EventHandler
  public void placeBlock(BlockPlaceEvent e) {
    if (!this.started || !isAlive(e.getPlayer()))
      e.setCancelled(true);
  }

  @EventHandler
  public void entDamage(EntityDamageByEntityEvent e) {
    if (!this.started || (e.getDamager() != null && e.getDamager().getType() == EntityType.PLAYER
        && !isAlive((Player) e.getDamager())))
      e.setCancelled(true);
  }

  @EventHandler
  public void onTNT(EntityExplodeEvent e) {
    List<Block> toRemove = new ArrayList<>();
    for (Block b : e.blockList()) {
      for (Lootbox lb : CTB.lm.chests) {
        if (lb.loc.getBlockX() == b.getLocation().getBlockX() && lb.loc.getBlockY() == b.getLocation().getBlockY()
            && lb.loc.getBlockZ() == b.getLocation().getBlockZ()
            && b.getWorld().getName().equals(lb.loc.getWorld().getName())) {
          toRemove.add(b);
        }
      }
      for (Lifestone ls : lifestones) {
        if (ls.location().getBlockX() == b.getLocation().getBlockX()
            && ls.location().getBlockY() == b.getLocation().getBlockY()
            && ls.location().getBlockZ() == b.getLocation().getBlockZ()
            && b.getWorld().getName().equals(ls.location().getWorld().getName())) {
          toRemove.add(b);
        }
      }
    }
    e.blockList().removeAll(toRemove);
  }

  private void scheduleTestTasks() {
    Bukkit.getScheduler().scheduleSyncRepeatingTask(CTB.pl, new Runnable() {
      @Override
      public void run() {
        lifestones.forEach(x -> {
          x.increasePrecision();
        });
      }
    }, 0L, 20 * 10);

    Bukkit.getScheduler().scheduleSyncRepeatingTask(CTB.pl, new Runnable() {
      @Override
      public void run() {
        lifestones.forEach(ls -> {
          double particles = ls.area.radius * ls.area.radius;
          for (double i = 0; i <= 8 * Math.PI; i += ((2 * Math.PI) / particles)) {
            // i = angle
            double x = ls.area.radius * Math.cos(i);
            double z = ls.area.radius * Math.sin(i);

            int cx = ls.area.x;
            int cz = ls.area.z;
            if (i >= (Math.PI / 2) && i <= ((3 * Math.PI) / 2)) {
              cx -= x;
            } else {
              cx += x;
            }

            if (i >= 0 && i <= Math.PI) {
              cz += z;
            } else {
              cz -= z;
            }

            ls.location().getWorld().spawnParticle(Particle.REDSTONE, cx, 90, cz, 0, 0, 0, 0, 1,
                new DustOptions(ls.team().color(), 1.5F));

          }
        });
      }
    }, 0, 5L);
  }
}
