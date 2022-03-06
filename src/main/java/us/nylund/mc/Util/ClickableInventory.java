package us.nylund.mc.Util;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_18_R1.inventory.CraftInventoryCustom;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public abstract class ClickableInventory extends CraftInventoryCustom {
  public List<Click> clicks;

  public ClickableInventory(int rows, String title) {
    super(null, rows * 9, ChatColor.translateAlternateColorCodes('&', title));
    this.clicks = new ArrayList<>();
  }

  public void setItemWithClick(int index, ItemStack item, Click event) {
    this.clicks.add(event);
    setItem(index, item);
  }

  public void addItemWithClick(int minimumIndex, ItemStack item, Click event) {
    this.clicks.add(event);
    for (int i = minimumIndex; i < getSize(); i++) {
      if (getItem(i) == null || getItem(i).getType() == Material.AIR) {
        setItem(i, item);
        break;
      }
    }
  }

  public void onClick(InventoryClickEvent e) {
    e.setCancelled(true);
    if (e.getCurrentItem() != null)
      for (Click c : this.clicks) {
        if (e.getCurrentItem().equals(c._item))
          c.run(e);
      }
  }

  public abstract void refresh();
}
