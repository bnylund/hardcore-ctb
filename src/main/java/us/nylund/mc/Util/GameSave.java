package us.nylund.mc.Util;

import java.util.ArrayList;
import java.util.List;

import us.nylund.mc.WorldBorder;
import us.nylund.mc.Game.GameManager;
import us.nylund.mc.Game.LootboxManager;
import us.nylund.mc.Team.TeamManager;

public class GameSave {
  public WorldBorder wb;
  public TeamManager tm;
  public LootboxManager lm;
  public GameManager gm;
  public List<PlayerInfo> players;

  public GameSave() {
    players = new ArrayList<PlayerInfo>();
  }
}
