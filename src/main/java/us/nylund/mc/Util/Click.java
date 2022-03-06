package us.nylund.mc.Util;

import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public abstract class Click {
  public ItemStack _item;

  public Click(ItemStack item) {
    this._item = item;
  }

  public abstract void run(InventoryClickEvent paramInventoryClickEvent);
}