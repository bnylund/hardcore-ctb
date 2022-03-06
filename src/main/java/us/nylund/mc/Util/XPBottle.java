package us.nylund.mc.Util;

import java.text.DecimalFormat;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ExpBottleEvent;
import org.bukkit.inventory.ItemStack;

import us.nylund.mc.CustomItem;

public class XPBottle implements Listener {
  @EventHandler
  public void onXPSplash(ExpBottleEvent e) {
    // Check for player
    if (!(e.getEntity().getShooter() instanceof Player) || !e.getEntity().getItem().hasItemMeta()) {
      return;
    }

    // Check for name
    ItemStack xpTest = CustomItem.getXPBottle(1, "test");
    if (!e.getEntity().getItem().getItemMeta().hasDisplayName()
        || !e.getEntity().getItem().getItemMeta().getDisplayName().equals(xpTest.getItemMeta().getDisplayName())) {
      return;
    }

    // Check for lore
    if (!e.getEntity().getItem().getItemMeta().hasLore()
        || e.getEntity().getItem().getItemMeta().getLore().size() != xpTest.getItemMeta().getLore().size()) {
      return;
    }

    Player p = (Player) e.getEntity().getShooter();

    // Parse lore
    String line = ChatColor.stripColor(e.getEntity().getItem().getItemMeta().getLore().get(0));
    if (line.contains("Value ") && line.contains("XP")) {
      int amount = -1;
      try {
        String xpString = line.substring(6, line.indexOf("XP") - 1);
        amount = Integer.parseInt(xpString);
      } catch (Exception ex) {
        return;
      }

      if (amount < 0) {
        return;
      }

      DecimalFormat df = new DecimalFormat("##,###");
      SetExpFix.setTotalExperience(p, SetExpFix.getTotalExperience(p) + amount);
      p.sendMessage(ChatColor.GREEN + ChatColor.BOLD.toString() + "+ " + df.format(amount) + "XP");

      e.setShowEffect(false);
      e.setExperience(0);
      e.setCancelled(true);
    }
  }
}
