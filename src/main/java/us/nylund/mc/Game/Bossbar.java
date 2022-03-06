package us.nylund.mc.Game;

import java.util.Optional;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

import us.nylund.mc.CTB;
import us.nylund.mc.Team.TeamColor;

public class Bossbar {
  public BossBar bar;
  public TeamColor color;
  public String player;
  private boolean show;

  public Bossbar(String player, TeamColor color) {
    this.player = player;
    this.color = color;
    this.show = true;

    bar = Bukkit.createBossBar(color.full() + " Lifestone", BarColor.RED, BarStyle.SEGMENTED_10);
    bar.setProgress(0D);
  }

  public void shutdown() {
    this.show = false;
    bar.removeAll();
  }

  public void tick() {
    Player p = Bukkit.getPlayer(UUID.fromString(player));

    if (p == null || !p.isOnline())
      return;

    if (!show) {
      bar.removeAll();
      return;
    }

    Optional<Lifestone> ls = CTB.gm.lifestone(color);
    if (ls.isPresent()) {
      if (ls.get().preciseAreas.size() > 0) {
        double precision = -0.1;
        for (int i = 0; i < ls.get().preciseAreas.size(); i++) {
          if (ls.get().preciseAreas.get(i).inCircle(p.getLocation()))
            precision += 0.1;
        }

        if (precision >= 0) {
          bar.setProgress(precision >= 1 ? 1 : precision);
          bar.addPlayer(p);
        } else {
          bar.removeAll();
        }
      } else {
        bar.removeAll();
      }
    } else {
      bar.removeAll();
    }
  }
}
