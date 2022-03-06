package us.nylund.mc;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.server.ServerListPingEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import us.nylund.mc.Util.ReflectionUtil;

public class Listeners implements Listener {

  @EventHandler
  public void onPing(ServerListPingEvent e) {
    e.setMaxPlayers(Bukkit.getOnlinePlayers().size() + 1);
    e.setMotd(ChatColor.translateAlternateColorCodes('&', "   &a-= &b&lSkull Island PVP&r &a=-\n"));
  }

  @SuppressWarnings("deprecation")
  @EventHandler
  public void onChat(AsyncPlayerChatEvent e) {
    String msg = "";
    if (e.getPlayer().isOp()) {
      msg += ChatColor.RED.toString() + ChatColor.BOLD.toString() + "MOD ";
    } else {
      if (CTB.pl.getConfig().contains("verified")) {
        if (CTB.pl.getConfig().getStringList("verified").contains(e.getPlayer().getUniqueId().toString())) {
          msg += ChatColor.GREEN.toString() + ChatColor.BOLD.toString() + "VERIFIED ";
        }
      }
    }
    msg += ChatColor.AQUA.toString() + e.getPlayer().getDisplayName() + ChatColor.WHITE.toString() + ": "
        + ChatColor.translateAlternateColorCodes('&', e.getMessage());

    // Check for [item]. If present, replace with hover tooltip of current item.
    if (msg.contains("[item]") && e.getPlayer().getInventory().getItemInMainHand() != null
        && e.getPlayer().getInventory().getItemInMainHand().getType() != Material.AIR) {
      int itemIndex = msg.indexOf("[item]");
      TextComponent pre = new TextComponent(msg.substring(0, itemIndex) + " » ");
      String post = ChatColor.RESET.toString() + " « " + (msg.substring(itemIndex + 6));
      String itemJson = ReflectionUtil.convertItemStackToJsonRegular(e.getPlayer().getInventory().getItemInMainHand());

      BaseComponent[] hoverComponents = new BaseComponent[] { new TextComponent(itemJson) };

      HoverEvent ev = new HoverEvent(HoverEvent.Action.SHOW_ITEM, hoverComponents);
      String itemName = e.getPlayer().getInventory().getItemInMainHand().hasItemMeta()
          && e.getPlayer().getInventory().getItemInMainHand().getItemMeta().hasDisplayName()
              ? e.getPlayer().getInventory().getItemInMainHand().getItemMeta().getDisplayName()
              : getFormattedName(e.getPlayer().getInventory().getItemInMainHand().getType().toString());
      TextComponent item = new TextComponent(
          itemName + ChatColor.RESET.toString()
              + (e.getPlayer().getInventory().getItemInMainHand().getAmount() > 1 ? ChatColor.DARK_GRAY.toString()
                  + " (x" + e.getPlayer().getInventory().getItemInMainHand().getAmount() + ")" : "")
              + ChatColor.RESET.toString());
      item.setHoverEvent(ev);

      pre.addExtra(item);
      pre.addExtra(post);

      for (Player p : Bukkit.getOnlinePlayers())
        p.spigot().sendMessage(pre);
    } else
      Bukkit.broadcastMessage(msg);
    e.setCancelled(true);
  }

  public static String getFormattedName(String input) {
    String[] parts = input.split("_");
    String total = "";
    byte b;
    int i;
    String[] arrayOfString1;
    for (i = (arrayOfString1 = parts).length, b = 0; b < i;) {
      String s = arrayOfString1[b];
      for (int j = 0; j < s.length(); j++) {
        if (j == 0) {
          total = String.valueOf(total) + s.toUpperCase().charAt(j);
        } else {
          total = String.valueOf(total) + s.toLowerCase().charAt(j);
        }
      }
      total = String.valueOf(total) + " ";
      b++;
    }
    total = total.substring(0, total.length() - 1);
    return total;
  }

  @SuppressWarnings("deprecation")
  @EventHandler
  public void onItemDamage(PlayerItemDamageEvent e) {
    ItemStack is = e.getItem();
    ItemMeta im = is.getItemMeta();
    List<String> lore = (im.hasLore() ? im.getLore() : new ArrayList<String>());

    short durability = (short) (is.getType().getMaxDurability() - is.getDurability());
    float percentBefore = ((float) (durability + e.getDamage()) / is.getType().getMaxDurability()) * 100;
    float percentRemaining = ((float) durability / is.getType().getMaxDurability()) * 100;
    durability -= 1;

    String mainHand = is.getType().toString();
    if (mainHand.contains("AXE") || mainHand.contains("SHOVEL") || mainHand.contains("HOE")
        || mainHand.contains("FLINT_AND_STEEL") || mainHand.contains("SHEARS") || mainHand.contains("FISHING_ROD")
        || mainHand.contains("BOW") || mainHand.contains("CHESTPLATE") || mainHand.contains("HELMET")
        || mainHand.contains("LEGGINGS") || mainHand.contains("BOOTS") || mainHand.contains("SWORD")) {
      ChatColor chatColor;
      if (mainHand.contains("DIAMOND")) {
        chatColor = ChatColor.AQUA;
      } else if (mainHand.contains("GOLD")) {
        chatColor = ChatColor.YELLOW;
      } else if (mainHand.contains("IRON")) {
        chatColor = ChatColor.WHITE;
      } else if (mainHand.contains("STONE")) {
        chatColor = ChatColor.GRAY;
      } else if (mainHand.contains("NETHERITE")) {
        chatColor = ChatColor.DARK_GRAY;
      } else if (mainHand.contains("WOODEN")) {
        chatColor = ChatColor.GOLD;
      } else {
        chatColor = ChatColor.WHITE;
      }

      String str1 = String.valueOf(chatColor) + getFormattedName(is.getType().toString());
      String currentDur = "";
      if (percentRemaining < 33) {
        currentDur = String.valueOf(ChatColor.RED.toString()) + durability;
      } else if (percentRemaining < 66) {
        currentDur = String.valueOf(ChatColor.GOLD.toString()) + durability;
      } else {
        currentDur = String.valueOf(ChatColor.GREEN.toString()) + durability;
      }
      boolean containsDur = false;
      if (!mainHand.contains("CHESTPLATE") && !mainHand.contains("HELMET") && !mainHand.contains("LEGGINGS")
          && !mainHand.contains("BOOTS") && !mainHand.contains("SWORD")) {
        e.getPlayer().spigot().sendMessage(ChatMessageType.ACTION_BAR,
            TextComponent.fromLegacyText(String.valueOf(str1) + ChatColor.WHITE + " - " + currentDur + ChatColor.WHITE
                + " / " + ChatColor.GREEN + is.getType().getMaxDurability()));
      }

      List<String> newLore = new ArrayList<String>();
      for (String ss : lore) {
        if (ss.contains(ChatColor.AQUA + ChatColor.BOLD.toString() + "Durability: "))
          containsDur = true;
      }
      if (containsDur)
        for (String sss : lore)
          if (sss.contains(ChatColor.AQUA + ChatColor.BOLD.toString() + "Durability: "))
            newLore.add(ChatColor.AQUA + ChatColor.BOLD.toString() + "Durability: "
                + ((percentRemaining < 33 ? ChatColor.RED.toString() + durability
                    : (percentRemaining < 66 ? ChatColor.YELLOW.toString() + durability
                        : ChatColor.GREEN.toString() + durability)))
                + ChatColor.GOLD + " / " + ChatColor.GREEN + is.getType().getMaxDurability());
          else
            newLore.add(sss);
      else {
        for (String ss : lore)
          newLore.add(ss);
        newLore.add(" ");
        newLore.add(ChatColor.AQUA + ChatColor.BOLD.toString() + "Durability: "
            + ((percentRemaining < 33 ? ChatColor.RED.toString() + durability
                : (percentRemaining < 66 ? ChatColor.YELLOW.toString() + durability
                    : ChatColor.GREEN.toString() + durability)))
            + ChatColor.GOLD + " / " + ChatColor.GREEN + is.getType().getMaxDurability());
        newLore.add(" ");
      }

      if (percentBefore >= 20 && percentRemaining < 20)
        e.getPlayer().sendMessage(ChatColor.RED + "Item durability dropped below 20%! (" + str1 + ChatColor.RED + ")");
      im.setLore(newLore);
      is.setItemMeta(im);
      e.getPlayer().updateInventory();
    }
  }
}
