package us.nylund.mc.Util;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class PlayerInfo {
  public String[] contents;
  public SaveLocation location;
  public String uuid;
  public int xp;

  public PlayerInfo(ItemStack[] contents, Location location, String uuid, int xp) {
    this.contents = new String[contents.length];
    for (int i = 0; i < contents.length; i++) {
      if (contents[i] != null)
        this.contents[i] = JsonItemStack.toJson(contents[i]);
      else
        this.contents[i] = null;
    }
    this.location = new SaveLocation(location);
    this.uuid = uuid;
    this.xp = xp;
  }

  public PlayerInfo() {}

  public void setInventory(Player p) {
    p.getInventory().clear();
    for (int i = 0; i < contents.length; i++) {
      if (contents[i] != null) {
        ItemStack is = JsonItemStack.fromJson(contents[i]);
        p.getInventory().setItem(i, is);
      }
    }
    p.updateInventory();
  }
}
