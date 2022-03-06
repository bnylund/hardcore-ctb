package us.nylund.mc.Util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

import com.google.gson.Gson;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

import us.nylund.mc.CTB;

public class SaveManager {
  public static boolean save() {
    GameSave gs = new GameSave();
    gs.wb = CTB.wb;
    gs.tm = CTB.tm;
    gs.lm = CTB.lm;
    gs.gm = CTB.gm;
    gs.players = CTB.gs.players;

    for (Player p : Bukkit.getOnlinePlayers()) {
      p.closeInventory();
      gs.players = gs.players.stream().filter(x -> !x.uuid.equals(p.getUniqueId().toString()))
          .collect(Collectors.toList());
      gs.players.add(new PlayerInfo(p.getInventory().getContents(), p.getLocation(), p.getUniqueId().toString(),
          SetExpFix.getTotalExperience(p)));
    }

    Gson gson = new Gson();
    String json = gson.toJson(gs);

    File f = new File(CTB.pl.getDataFolder(), "save.json");
    try {
      f.createNewFile();
      FileWriter fw = new FileWriter(f);
      fw.write(json);
      fw.close();
    } catch (IOException e) {
      e.printStackTrace();
      return false;
    }

    return true;
  }

  public static boolean load() {
    File f = new File(CTB.pl.getDataFolder(), "save.json");
    if (!f.exists() || !f.isFile())
      return false;

    try {
      BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(f.getAbsolutePath()), "utf-8"));
      String json = br.readLine();
      br.close();
      if (json == null)
        return false;
      Gson gson = new Gson();
      GameSave gs = gson.fromJson(json, GameSave.class);

      // Unregister event handlers
      HandlerList.unregisterAll(CTB.gm);
      HandlerList.unregisterAll(CTB.lm);
      HandlerList.unregisterAll(CTB.wb);
      CTB.tm.teams.forEach(x -> HandlerList.unregisterAll(x));

      CTB.gm = gs.gm;
      CTB.lm = gs.lm;
      CTB.tm = gs.tm;
      CTB.wb = gs.wb;
      CTB.gs = gs;

      CTB.pm.registerEvents(CTB.gm, CTB.pl);
      CTB.pm.registerEvents(CTB.lm, CTB.pl);
      CTB.pm.registerEvents(CTB.wb, CTB.pl);

      CTB.tm.register();
      CTB.tm.teams.forEach(x -> x.initSidebar());

      CTB.gm.lifestones.forEach(x -> x.start());

      CTB.cl.setEnabled(false);

      CTB.gm.startTimers();
    } catch (IOException e) {
      e.printStackTrace();
      return false;
    }

    return true;
  }
}
