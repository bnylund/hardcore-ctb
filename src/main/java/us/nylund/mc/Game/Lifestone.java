package us.nylund.mc.Game;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Particle.DustOptions;
import org.bukkit.entity.Player;

import us.nylund.mc.CTB;
import us.nylund.mc.Team.TeamColor;
import us.nylund.mc.Util.SaveLocation;

public class Lifestone {
  private SaveLocation sl;
  private TeamColor team;
  private TeamColor placed;
  private char precision;
  public Circle area;
  public List<Circle> preciseAreas;
  public double timer;
  private transient int task;
  private transient int increaseTask;
  private transient double modifier;

  public Lifestone(Location location, TeamColor team, TeamColor placed) {
    this.sl = new SaveLocation(location);
    this.team = team;
    this.placed = placed;
    this.task = -1;
    this.increaseTask = -1;
    this.precision = 0;
    Random rd = new Random();
    double angle = rd.nextDouble() * 2 * Math.PI;
    this.area = new Circle(angle, rd.nextInt(100), 100, location);
    this.preciseAreas = new ArrayList<Circle>();
    this.preciseAreas.add(this.area);
    this.modifier = CTB.GAMEPLAY_TIMER_MULTIPLIER == 0 ? 1 : CTB.GAMEPLAY_TIMER_MULTIPLIER;

    this.timer = 60 * 60 * modifier;
  }

  public Lifestone() {
    this.task = -1;
    this.increaseTask = -1;
  }

  public Location location() {
    return new Location(Bukkit.getWorld(sl.world), sl.x, sl.y, sl.z, sl.yaw, sl.pitch);
  }

  public TeamColor team() {
    return this.team;
  }

  public TeamColor placed() {
    return this.placed;
  }

  // -0.6y^2+1
  private double multiplier(double y) {
    return -0.6 * y * y + 1;
  }

  public char precision() {
    return this.precision;
  }

  public void start() {
    if (this.task == -1)
      createHelix();

    if (this.increaseTask == -1) {
      // Gets more precise every hour, besides offline hours
      this.increaseTask = Bukkit.getScheduler().scheduleSyncRepeatingTask(CTB.pl, new Runnable() {
        @Override
        public void run() {
          if (!CTB.gm.isOfflineHours() && CTB.gm.started && !CTB.gm.complete) {
            timer -= 1;

            if (timer <= 0) {
              increasePrecision();
              timer = 60 * 60 * modifier;
            }
          }
        }
      }, 0L, 20L);
    }

  }

  public void increasePrecision() {
    if (precision < 10) {
      precision++;
      Random rd = new Random();
      double angle = rd.nextDouble() * 2 * Math.PI;
      int radius = (int) Math.round((0.7D * precision - 10D) * (0.7D * precision - 10D)); // (0.7x-10)^2
      this.area = new Circle(angle, rd.nextInt(radius), radius, location());
      this.preciseAreas.add(this.area);
    }
  }

  public boolean inRange(Location loc) {
    return this.area.inCircle(loc);
  }

  private void createHelix() {
    this.task = Bukkit.getScheduler().scheduleSyncRepeatingTask(CTB.pl, new Runnable() {
      int runs = 0;

      @Override
      public void run() {
        double radius = 0.9;
        for (double y = -1.1; y <= 1.1; y += 0.03) {
          double x = (radius * multiplier(y)) * Math.cos((-6) * (y + 1) + (runs / 9) * (Math.PI / 180)) + 0.5;
          double z = (radius * multiplier(y)) * Math.sin((-6) * (y + 1) + (runs / 9) * (Math.PI / 180)) + 0.5;
          runs++;
          if (runs == Integer.MAX_VALUE)
            runs = 0;
          DustOptions dt = new DustOptions(team.color(), 0.5F);
          location().getWorld().spawnParticle(Particle.REDSTONE, (float) (location().getX() + x),
              (float) (location().getY() + y + 0.5), (float) (location().getZ() + z), 0, 0, 0, 0, dt);
        }
      }
    }, 0L, 1L);

  }

  public void destroy() {
    if (task != -1) {
      Bukkit.getScheduler().cancelTask(task);
    }
    if (increaseTask != -1) {
      Bukkit.getScheduler().cancelTask(increaseTask);
    }
  }

  public boolean touching(Location l) {
    return l.getBlockX() >= location().getBlockX() - 1 && l.getBlockX() <= location().getBlockX() + 1
        && l.getBlockY() >= location().getBlockY() - 1 && l.getBlockY() <= location().getBlockY() + 1
        && l.getBlockZ() >= location().getBlockZ() - 1 && l.getBlockZ() <= location().getBlockZ() + 1;
  }

  public List<Lifestone> connected() {
    List<Lifestone> connected = new ArrayList<Lifestone>();

    for (int x = this.location().getBlockX() - 1; x <= this.location().getBlockX() + 1; x++) {
      for (int y = this.location().getBlockY() - 1; y <= this.location().getBlockY() + 1; y++) {
        for (int z = this.location().getBlockZ() - 1; z <= this.location().getBlockZ() + 1; z++) {
          if (x == this.location().getBlockX() && y == this.location().getBlockY() && z == this.location().getBlockZ())
            continue;
          else {
            for (int i = 0; i < CTB.gm.lifestones().size(); i++) {
              Lifestone ls = CTB.gm.lifestones().get(i);
              if (ls.location().getBlockX() == x && ls.location().getBlockY() == y && ls.location().getBlockZ() == z) {
                connected.add(ls);
              }
            }
          }
        }
      }
    }

    return connected;
  }

  public static Optional<Lifestone> getLifestone(Location loc) {
    List<Lifestone> stones = CTB.gm.lifestones();
    for (int i = 0; i < stones.size(); i++) {
      if (stones.get(i).location().equals(loc))
        return Optional.of(stones.get(i));
    }
    return Optional.empty();
  }

  public static List<TeamColor> lifestones(Player p) {
    return new ArrayList<TeamColor>();
  }
}
