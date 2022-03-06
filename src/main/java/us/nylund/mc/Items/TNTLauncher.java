package us.nylund.mc.Items;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class TNTLauncher implements Listener {
  public static HashMap<TNTPrimed, Player> shots;

  static {
    if (shots == null)
      shots = new HashMap<TNTPrimed, Player>();
  }

  @EventHandler
  public void onInteract(PlayerInteractEvent e) {
    if (e.getAction().equals(Action.RIGHT_CLICK_AIR) || e.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
      if (isLauncher(e.getItem())) {
        shoot(e.getPlayer());

        int remaining = getRemaining(e.getItem());

        setRemaining(e.getItem(), remaining - 1);
        e.getPlayer().updateInventory();

        if (remaining - 1 <= 0) {
          if (e.getPlayer().getInventory().getItemInMainHand().equals(e.getItem()))
            e.getPlayer().getInventory().setItemInMainHand(null);
          if (e.getPlayer().getInventory().getItemInOffHand().equals(e.getItem()))
            e.getPlayer().getInventory().setItemInMainHand(null);
          e.getPlayer().playSound(e.getPlayer().getLocation(), Sound.ENTITY_ITEM_BREAK, 200F, 200F);
        }

        e.getPlayer().updateInventory();
        e.setCancelled(true);
      }
    }
  }

  @EventHandler
  public void onTNT(EntityExplodeEvent e) {
    if (e.getEntityType() == EntityType.PRIMED_TNT && shots.containsKey((TNTPrimed) e.getEntity())) {
      Collection<Entity> ents = e.getLocation().getWorld().getNearbyEntities(e.getLocation(), 7, 7, 7, x -> {
        return x instanceof Player;
      });

      Player shooter = shots.get((TNTPrimed) e.getEntity());

      ents.forEach(x -> {
        if (x.getEntityId() != shooter.getEntityId()) {
          Player p = (Player) x;
          p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20 * 10, 4, true, true));
          p.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 20 * 10, 0, true, true));
        }
      });

      e.getEntity().getWorld().playSound(e.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 200F, 200F);
      shots.remove((TNTPrimed) e.getEntity());
      e.getEntity().remove();

      e.setCancelled(true);
      e.setYield(0);
    }
  }

  public static void shoot(Player p) {
    TNTPrimed tnt = (TNTPrimed) p.getWorld().spawnEntity(p.getEyeLocation(), EntityType.PRIMED_TNT, false);
    tnt.setYield(5);
    tnt.setInvulnerable(true);
    tnt.setSource(p);
    tnt.setVelocity(p.getLocation().getDirection().clone().multiply(2));
    shots.put(tnt, p);
  }

  public static int getRemaining(ItemStack is) {
    if (isLauncher(is)) {
      String line = ChatColor.stripColor(is.getItemMeta().getLore().get(1));
      String remaining = line.split(":")[1].split("/")[0].trim();
      return Integer.parseInt(remaining);
    }
    return 0;
  }

  public static void setRemaining(ItemStack is, int remaining) {
    if (isLauncher(is)) {
      ItemMeta im = is.getItemMeta();
      List<String> lore = im.getLore();
      lore.set(1,
          ChatColor.GOLD + "  Shots left: "
              + (remaining >= 4 ? ChatColor.GREEN : remaining >= 2 ? ChatColor.YELLOW : ChatColor.DARK_RED)
              + ChatColor.BOLD.toString() + remaining + ChatColor.DARK_GRAY + " / 5");
      im.setLore(lore);
      is.setItemMeta(im);
    }
  }

  public static boolean isLauncher(ItemStack item) {
    if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasDisplayName() || !item.getItemMeta().hasLore())
      return false;

    ItemStack ls = getLauncher();
    if (!ls.getItemMeta().getDisplayName().equals(item.getItemMeta().getDisplayName()))
      return false;

    if (item.getItemMeta().getLore().size() != ls.getItemMeta().getLore().size())
      return false;

    List<String> lore = ls.getItemMeta().getLore();
    boolean match = true;
    for (int j = 0; j < lore.size(); j++)
      if (j != 1 && !item.getItemMeta().getLore().get(j).equals(lore.get(j)))
        match = false;

    return match;

  }

  public static ItemStack getLauncher() {
    ItemStack is = new ItemStack(Material.FIRE_CHARGE, 1);
    ItemMeta im = is.getItemMeta();
    im.setDisplayName(ChatColor.DARK_PURPLE + ChatColor.BOLD.toString() + "TNT Launcher " + ChatColor.GRAY.toString()
        + "(Right Click)");
    List<String> lore = new ArrayList<String>();
    lore.add("");
    lore.add(ChatColor.GOLD + "  Shots left: " + ChatColor.GREEN + ChatColor.BOLD.toString() + "5 "
        + ChatColor.DARK_GRAY + "/ 5");
    lore.add("");
    im.setLore(lore);

    im.addItemFlags(ItemFlag.HIDE_ENCHANTS);
    im.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 1, true);
    is.setItemMeta(im);
    return is;
  }
}
