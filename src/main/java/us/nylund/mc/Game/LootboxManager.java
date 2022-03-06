package us.nylund.mc.Game;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Chicken;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.util.Vector;

import us.nylund.mc.CTB;
import us.nylund.mc.Team.Team;

public class LootboxManager implements Listener {

  public transient List<Lootbox> chests;
  public transient List<Entity> noInteract;
  public char tier;
  public char placed;

  public LootboxManager() {
    this.chests = new ArrayList<Lootbox>();
    this.noInteract = new ArrayList<Entity>();
    this.tier = (char) 1;
    this.placed = (char) 0;
  }

  public void doDrop() {
    doDrop(this.tier);
  }

  public void doDrop(char tier) {
    World w = Bukkit.getWorld(CTB.gm.world);
    Location loc = CTB.gm.spawnLocation(w);
    for (Player p : Bukkit.getOnlinePlayers()) {
      p.sendMessage(ChatColor.AQUA + "A lootbox will be dropping at " + ChatColor.GOLD + ChatColor.BOLD.toString()
          + loc.getBlockX() + "x " + loc.getBlockY() + "y " + loc.getBlockZ() + "z" + ChatColor.AQUA
          + " in 5 minutes!");
      p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_GUITAR, 200F, 200F);
    }

    // Announce in discord

    Bukkit.getScheduler().scheduleSyncDelayedTask(CTB.pl, new Runnable() {
      @Override
      public void run() {
        drop(loc, tier);
      }
    }, 20 * 60 * 4 + (8 * 20 - 10));
  }

  @EventHandler
  public void interactEntity(PlayerInteractAtEntityEvent e) {
    if (e.getRightClicked() == null)
      return;

    if (noInteract.contains(e.getRightClicked())) {
      e.setCancelled(true);
      return;
    }

    if (e.getRightClicked().getType() == EntityType.ARMOR_STAND)
      for (int i = 0; i < chests.size(); i++)
        if (chests.get(i).as != null && chests.get(i).as.getEntityId() == e.getRightClicked().getEntityId())
          e.setCancelled(true);
  }

  @EventHandler
  public void onInteract(PlayerInteractEvent e) {
    if (e.getClickedBlock() == null)
      return;

    for (int i = 0; i < chests.size(); i++) {
      Location l = chests.get(i).loc;
      Location l2 = e.getClickedBlock().getLocation();
      if (l.getBlockX() == l2.getBlockX() && l.getBlockY() == l2.getBlockY() && l.getBlockZ() == l2.getBlockZ()) {
        Optional<Team> team = CTB.tm.getPlayerTeam(e.getPlayer().getUniqueId().toString());
        if (team.isPresent()) {

          if (team.get().out.contains(e.getPlayer().getUniqueId().toString())) {
            e.setCancelled(true);
            return;
          }

          if (!chests.get(i).running() && chests.get(i).timer > 0) {
            chests.get(i).setTeam(team.get().color());
            chests.get(i).startTimer();
            Bukkit.getOnlinePlayers().forEach(x -> x.playSound(x.getLocation(), Sound.ENTITY_WITHER_DEATH, 200F, 200F));
            Bukkit.broadcastMessage(
                team.get().color().full() + " Team " + ChatColor.YELLOW + "started opening up the lootbox!");
            e.setCancelled(true);
          } else if (!chests.get(i).running() && chests.get(i).timer <= 0
              && chests.get(i).team.equals(team.get().color())) {
            if (e.getAction().equals(Action.RIGHT_CLICK_BLOCK))
              e.setCancelled(false);
          } else {
            if (!chests.get(i).team.equals(team.get().color()) && chests.get(i).running()) {
              chests.get(i).setTeam(team.get().color());
              chests.get(i).timer = chests.get(i).full;
              Bukkit.getOnlinePlayers()
                  .forEach(x -> x.playSound(x.getLocation(), Sound.ENTITY_WITHER_DEATH, 200F, 200F));
              Bukkit.broadcastMessage(
                  team.get().color().full() + " Team " + ChatColor.YELLOW + "has took over the lootbox!");
            }
            e.setCancelled(true);
          }
        }
      }
    }
  }

  @EventHandler
  public void onClose(InventoryCloseEvent e) {
    for (int i = 0; i < chests.size(); i++)
      chests.get(i).checkBreak();
  }

  public void drop(Location loc, char tier) {
    World w = loc.getWorld();
    loc.add(0.5, 120, 0.5);

    Chicken ent = (Chicken) w.spawnEntity(loc, EntityType.CHICKEN, false);
    ent.setInvulnerable(true);
    ent.setRemoveWhenFarAway(false);
    ent.setAware(false);
    ent.setCollidable(false);

    ArmorStand as = (ArmorStand) w.spawnEntity(loc, EntityType.ARMOR_STAND, false);
    as.setInvisible(true);
    as.setAI(false);
    as.setGravity(false);
    as.setInvulnerable(true);
    as.setRemoveWhenFarAway(false);
    as.getEquipment().setHelmet(new ItemStack(Material.CHEST, 1), true);
    as.addPassenger(ent);

    as.setVelocity(new Vector(0, 0, 0));

    noInteract.add(as);
    noInteract.add(ent);

    int task2 = Bukkit.getScheduler().scheduleSyncRepeatingTask(CTB.pl, new Runnable() {
      @Override
      public void run() {
        Location loc = as.getLocation().subtract(0D, 0.11417476267513D, 0D);
        if (!loc.getBlock().getType().isSolid()) {
          ent.teleport(as);
          as.teleport(loc);
          as.addPassenger(ent);
        }
      }
    }, 0L, 1L);

    int task = Bukkit.getScheduler().scheduleSyncRepeatingTask(CTB.pl, new Runnable() {
      @Override
      public void run() {
        Firework fw = (Firework) ent.getWorld().spawnEntity(ent.getLocation(), EntityType.FIREWORK);
        FireworkMeta fwm = fw.getFireworkMeta();

        fwm.setPower(1);
        fwm.addEffect(FireworkEffect.builder().withColor(Color.LIME).flicker(true).trail(true).build());

        fw.setFireworkMeta(fwm);
        fw.detonate();
      }
    }, 20 * 5, 20 * 5);

    Bukkit.getScheduler().scheduleSyncDelayedTask(CTB.pl, new Runnable() {
      @Override
      public void run() {
        chests.add(new Lootbox(ent.getLocation(), tier));
        Bukkit.getScheduler().cancelTask(task);
        Bukkit.getScheduler().cancelTask(task2);
        noInteract.remove(as);
        noInteract.remove(ent);
        ent.remove();
        as.remove();
      }
    }, 52 * 20 + 10);
  }
}
