package us.nylund.mc.Items;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import us.nylund.mc.CTB;
import us.nylund.mc.Game.Lifestone;
import us.nylund.mc.Game.Lootbox;

public class Flamethrower implements Listener {

  @EventHandler
  public void onInteract(PlayerInteractEvent e) {
    if (e.getAction().equals(Action.RIGHT_CLICK_AIR) || e.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
      if (isFlamethrower(e.getItem())) {
        Flamethrower.shootParticles(e.getPlayer());

        int remaining = Flamethrower.getRemaining(e.getItem());

        Flamethrower.setRemaining(e.getItem(), remaining - 1);
        e.getPlayer().updateInventory();

        if (remaining - 1 <= 0) {
          if (e.getPlayer().getInventory().getItemInMainHand().equals(e.getItem()))
            e.getPlayer().getInventory().setItemInMainHand(null);
          if (e.getPlayer().getInventory().getItemInOffHand().equals(e.getItem()))
            e.getPlayer().getInventory().setItemInMainHand(null);
          e.getPlayer().playSound(e.getPlayer().getLocation(), Sound.ENTITY_ITEM_BREAK, 200F, 200F);
        }
        e.getPlayer().updateInventory();

        String text = ChatColor.RED + ChatColor.BOLD.toString() + "Flamethrower "
            + (remaining > 55 ? ChatColor.GREEN : remaining > 25 ? ChatColor.YELLOW : ChatColor.DARK_RED)
            + ChatColor.BOLD.toString();
        for (int i = 1; i <= 100; i += 2) {
          if (remaining < i && !text.split(" ")[1].contains(ChatColor.RED.toString()))
            text += ChatColor.RED + ChatColor.BOLD.toString();
          text += "|";
        }

        e.getPlayer().spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(text));
        e.setCancelled(true);
      }
    }
  }

  public static void shootParticles(Player p) {
    BukkitRunnable br = new BukkitRunnable() {
      int runs = 0;

      @Override
      public void run() {
        if (runs > 2) {
          this.cancel();
          return;
        }
        runs++;

        boolean broke = true;
        for (double d = 0; d < 7; d += 0.05) {
          Vector ray = p.getLocation().getDirection().clone().multiply(d);
          Location loc = ray.toLocation(p.getWorld()).add(p.getEyeLocation());

          if (loc.getBlock() != null && loc.getBlock().getType().isSolid()) {

            boolean canFire = true;
            for (Lifestone ls : CTB.gm.lifestones)
              if (ls.location().getBlockX() == loc.getBlockX() && ls.location().getBlockY() == loc.getBlockY() + 1
                  && ls.location().getBlockZ() == loc.getBlockZ()
                  && loc.getWorld().getName().equals(ls.location().getWorld().getName()))
                canFire = false;

            for (Lootbox ls : CTB.lm.chests)
              if (ls.loc.getBlockX() == loc.getBlockX() && ls.loc.getBlockY() == loc.getBlockY() + 1
                  && ls.loc.getBlockZ() == loc.getBlockZ()
                  && loc.getWorld().getName().equals(ls.loc.getWorld().getName()))
                canFire = false;

            if (canFire)
              loc.getWorld().setBlockData((int) loc.getBlockX(), (int) loc.getBlockY() + 1, (int) loc.getBlockZ(),
                  Material.FIRE.createBlockData());
            break;
          }

          if (loc.getBlock() != null && loc.getBlock().isLiquid()) {
            if (loc.getBlock().getType() == Material.WATER) {

              loc.getWorld().spawnParticle(Particle.SMOKE_LARGE, loc, 0, 0, 0, 0, 0);
            }
            break;
          }

          loc.getWorld().spawnParticle(Particle.FLAME, loc, 0, 0, 0, 0, 0);

          Collection<Entity> nearby = loc.getWorld().getNearbyEntities(loc, 0.3, 0.3, 0.3, x -> {
            return x instanceof LivingEntity && x.getEntityId() != p.getEntityId();
          });
          boolean sBreak = false;
          if (nearby.size() > 0) {
            LivingEntity ent = (LivingEntity) nearby.iterator().next();
            ent.damage(2, p);
            ent.setFireTicks(6 * 20);
            ent.setVelocity(ent.getVelocity().add(p.getLocation().getDirection().clone().multiply(0.1)));
            sBreak = true;
            break;
          }
          if (sBreak)
            break;

          broke = false;
        }
        if (!broke)
          p.getWorld().playSound(p.getLocation(), Sound.BLOCK_BLASTFURNACE_FIRE_CRACKLE, 200F, 200F);
      }
    };
    br.runTaskTimer(CTB.pl, 0L, 1L);
  }

  public static int getRemaining(ItemStack is) {
    if (isFlamethrower(is)) {
      String line = ChatColor.stripColor(is.getItemMeta().getLore().get(1));
      String remaining = line.split(":")[1].split("/")[0].trim();
      return Integer.parseInt(remaining);
    }
    return 0;
  }

  public static void setRemaining(ItemStack is, int remaining) {
    if (isFlamethrower(is)) {
      ItemMeta im = is.getItemMeta();
      List<String> lore = im.getLore();
      lore.set(1,
          ChatColor.GOLD + "  Charge: "
              + (remaining > 55 ? ChatColor.GREEN : remaining > 25 ? ChatColor.YELLOW : ChatColor.DARK_RED)
              + ChatColor.BOLD.toString() + remaining + ChatColor.DARK_GRAY + " / 100");
      im.setLore(lore);
      is.setItemMeta(im);
    }
  }

  public static boolean isFlamethrower(ItemStack item) {
    if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasDisplayName() || !item.getItemMeta().hasLore())
      return false;

    ItemStack ls = getFlamethrower();
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

  public static ItemStack getFlamethrower() {
    ItemStack is = new ItemStack(Material.BLAZE_ROD, 1);
    ItemMeta im = is.getItemMeta();
    im.setDisplayName(
        ChatColor.RED + ChatColor.BOLD.toString() + "Flamethrower " + ChatColor.GRAY.toString() + "(Right Click)");
    List<String> lore = new ArrayList<String>();
    lore.add("");
    lore.add(ChatColor.GOLD + "  Charge: " + ChatColor.GREEN + ChatColor.BOLD.toString() + "100 " + ChatColor.DARK_GRAY
        + "/ 100");
    lore.add("");
    im.setLore(lore);

    im.addItemFlags(ItemFlag.HIDE_ENCHANTS);
    im.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 1, true);
    is.setItemMeta(im);
    return is;
  }
}
