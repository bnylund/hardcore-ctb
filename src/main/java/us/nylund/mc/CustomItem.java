package us.nylund.mc;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import us.nylund.mc.Team.TeamColor;

public class CustomItem {

  public static TeamColor isLifestone(ItemStack item) {
    if (!item.hasItemMeta() || !item.getItemMeta().hasDisplayName() || !item.getItemMeta().hasLore())
      return null;

    TeamColor[] cols = TeamColor.values();

    for (int i = 0; i < cols.length; i++) {
      ItemStack ls = getLifestone(cols[i]);
      if (!ls.getItemMeta().getDisplayName().equals(item.getItemMeta().getDisplayName()))
        continue;

      if (item.getItemMeta().getLore().size() != ls.getItemMeta().getLore().size())
        continue;

      List<String> lore = ls.getItemMeta().getLore();
      boolean match = true;
      for (int j = 0; j < lore.size(); j++)
        if (!item.getItemMeta().getLore().get(j).equals(lore.get(j)))
          match = false;

      if (match)
        return cols[i];
    }

    return null;
  }

  public static ItemStack getLifestone(TeamColor team) {
    ItemStack is = new ItemStack(team.material(), 1);
    ItemMeta im = is.getItemMeta();
    im.setDisplayName(team.chatcolor() + ChatColor.BOLD.toString() + team.string() + " Lifestone "
        + ChatColor.GRAY.toString() + "(Place)");
    List<String> lore = new ArrayList<String>();
    lore.add("");
    lore.add(ChatColor.YELLOW + "This lifestone is the heart of " + team.chatcolor() + ChatColor.BOLD.toString()
        + team.string() + " Team" + ChatColor.YELLOW + ".");
    lore.add(ChatColor.YELLOW + "Without having possession of your");
    lore.add(ChatColor.YELLOW + "team's lifestone, you will only have");
    lore.add(ChatColor.YELLOW + "1 life across your entire team. Keep");
    lore.add(ChatColor.YELLOW + "this lifestone safe!");
    lore.add("");
    lore.add(ChatColor.YELLOW + "If you have an opponent's lifestone,");
    lore.add(ChatColor.YELLOW + "place it connected to your lifestone to");
    lore.add(ChatColor.YELLOW + "gain an additional " + ChatColor.GOLD + ChatColor.BOLD.toString() + "5"
        + ChatColor.YELLOW + " lives!");
    im.setLore(lore);

    im.addItemFlags(ItemFlag.HIDE_ENCHANTS);
    im.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 1, true);
    is.setItemMeta(im);
    return is;
  }

  public static ItemStack getXPBottle(int amount, String author) {
    ItemStack is = new ItemStack(Material.EXPERIENCE_BOTTLE, 1);
    ItemMeta im = is.getItemMeta();
    im.setDisplayName(
        ChatColor.GREEN + "Experience Bottle " + ChatColor.RESET.toString() + ChatColor.GRAY.toString() + "(Throw)");
    List<String> lore = new ArrayList<String>();
    lore.add(ChatColor.LIGHT_PURPLE + "Value " + ChatColor.WHITE + amount + " XP");
    lore.add(ChatColor.LIGHT_PURPLE + "Owner " + ChatColor.WHITE + author);
    im.setLore(lore);
    is.setItemMeta(im);
    return is;
  }
}
