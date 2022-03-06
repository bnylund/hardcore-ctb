package us.nylund.mc.Game;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Chest;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.util.Vector;

import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import us.nylund.mc.CTB;
import us.nylund.mc.Team.TeamColor;

public class Lootbox {
  public Location loc;
  public int timer;
  public int full;
  public TeamColor team;
  private int task;
  private Hologram hg;
  private char tier;
  public ArmorStand as;
  public BossBar bar;

  public List<ItemStack> drops;

  public Lootbox(Location location, char tier) {
    this.loc = location;
    this.timer = 30;

    // 1 minute
    if (tier == 2)
      this.timer = 60;

    // 2.5 minutes
    if (tier == 3)
      this.timer = 150;

    // 5 minutes
    if (tier == 4)
      this.timer = 300;

    this.full = this.timer;

    this.task = -1;

    loc.getBlock().setType(Material.CHEST);

    Firework fw = (Firework) loc.getWorld().spawnEntity(loc, EntityType.FIREWORK);
    FireworkMeta fwm = fw.getFireworkMeta();

    fwm.setPower(1);
    fwm.addEffect(FireworkEffect.builder().withColor(Color.RED).flicker(true).trail(true).build());

    fw.setFireworkMeta(fwm);
    fw.detonate();

    this.hg = DHAPI.createHologram(
        "lootbox-" + location.getBlockX() + "-" + location.getBlockY() + "-" + location.getBlockZ(),
        location.clone().add(0, 2, 0), false);

    this.tier = tier;
    this.drops = new ArrayList<ItemStack>();
  }

  public boolean running() {
    return this.task != -1;
  }

  public void setTeam(TeamColor team) {
    this.team = team;
  }

  public void startTimer() {
    if (task == -1) {

      String titleColor = "";
      if (tier == 1) {
        titleColor = ChatColor.GRAY + ChatColor.BOLD.toString();
      } else if (tier == 2) {
        titleColor = ChatColor.GREEN + ChatColor.BOLD.toString();
      } else if (tier == 3) {
        titleColor = ChatColor.DARK_BLUE + ChatColor.BOLD.toString();
      } else if (tier == 4) {
        titleColor = ChatColor.RED + ChatColor.BOLD.toString();
      }

      bar = Bukkit.createBossBar(titleColor + "Tier " + ((int) tier) + " Lootbox: " + team.full() + " Team",
          BarColor.GREEN, BarStyle.SEGMENTED_12);
      bar.setProgress(0D);

      this.loc.getWorld().getPlayers().forEach(x -> bar.addPlayer(x));

      int ticks = 60;
      String msg = ChatColor.RED + "";
      for (int i = 0; i < ticks; i++) {
        msg += "|";
      }
      List<String> lines = Arrays.asList(team.full() + " Team", msg);
      DHAPI.setHologramLines(hg, lines);
      this.task = Bukkit.getScheduler().scheduleSyncRepeatingTask(CTB.pl, new Runnable() {
        @Override
        public void run() {
          timer--;
          double ticks = 60;
          String msg = ChatColor.GREEN + "";

          for (double i = 1 / ticks; i <= 1; i += 1 / ticks) {
            double prog = 1 - ((double) timer / (double) full);

            String titleColor = "";
            if (tier == 1) {
              titleColor = ChatColor.GRAY + ChatColor.BOLD.toString();
            } else if (tier == 2) {
              titleColor = ChatColor.GREEN + ChatColor.BOLD.toString();
            } else if (tier == 3) {
              titleColor = ChatColor.DARK_BLUE + ChatColor.BOLD.toString();
            } else if (tier == 4) {
              titleColor = ChatColor.RED + ChatColor.BOLD.toString();
            }

            bar.setProgress(prog);
            bar.setTitle(titleColor + "Tier " + ((int) tier) + " Lootbox: " + team.full() + " Team");

            if (i >= prog && !msg.contains(ChatColor.RED + "") && timer > 0)
              msg += ChatColor.RED;
            msg += "|";
          }

          loc.getBlock().setType(Material.CHEST);

          if (timer <= 0) {
            stopTimer();
            hg.getLocation().getWorld().playSound(hg.getLocation(), Sound.BLOCK_NOTE_BLOCK_HARP, 200F, 200F);
            msg += ChatColor.GREEN + ChatColor.BOLD.toString() + " ✔";
            List<String> lines = Arrays.asList(team.full() + " Team", msg);
            DHAPI.setHologramLines(hg, lines);

            Chest chest = (Chest) loc.getBlock().getState();
            chest.update();
            Random rd = new Random();

            if (tier == 2) {
              chest.getInventory().setItem(rd.nextInt(4), getItem());
              chest.getInventory().setItem(rd.nextInt(4) + 4, getItem());
              chest.getInventory().setItem(rd.nextInt(4) + 8, getItem());
              chest.getInventory().setItem(rd.nextInt(4) + 12, getItem());
              chest.getInventory().setItem(rd.nextInt(4) + 16, getItem());
              chest.getInventory().setItem(rd.nextInt(4) + 20, getItem());
              chest.getInventory().setItem(rd.nextInt(3) + 24, getItem());
            } else if (tier == 3) {
              chest.getInventory().setItem(rd.nextInt(5), getItem());
              chest.getInventory().setItem(rd.nextInt(5) + 5, getItem());
              chest.getInventory().setItem(rd.nextInt(5) + 10, getItem());
              chest.getInventory().setItem(rd.nextInt(5) + 15, getItem());
              chest.getInventory().setItem(rd.nextInt(4) + 20, getItem());
              chest.getInventory().setItem(rd.nextInt(3) + 24, getItem());
            } else if (tier == 4) {
              chest.getInventory().setItem(rd.nextInt(5), getItem());
              chest.getInventory().setItem(rd.nextInt(5) + 5, getItem());
              chest.getInventory().setItem(rd.nextInt(5) + 10, getItem());
              chest.getInventory().setItem(rd.nextInt(6) + 15, getItem());
              chest.getInventory().setItem(rd.nextInt(6) + 21, getItem());
            } else {
              chest.getInventory().setItem(rd.nextInt(3), getItem());
              chest.getInventory().setItem(rd.nextInt(3) + 3, getItem());
              chest.getInventory().setItem(rd.nextInt(3) + 6, getItem());
              chest.getInventory().setItem(rd.nextInt(3) + 9, getItem());
              chest.getInventory().setItem(rd.nextInt(3) + 12, getItem());
              chest.getInventory().setItem(rd.nextInt(3) + 15, getItem());
              chest.getInventory().setItem(rd.nextInt(3) + 18, getItem());
              chest.getInventory().setItem(rd.nextInt(3) + 21, getItem());
              chest.getInventory().setItem(rd.nextInt(3) + 24, getItem());
            }
          } else {
            msg += ChatColor.YELLOW + " " + timer + "s";
            List<String> lines = Arrays.asList(team.full() + " Team", msg);
            DHAPI.setHologramLines(hg, lines);
          }
        }
      }, 20L, 20L);
    }
  }

  public void stopTimer() {
    if (task != -1)
      Bukkit.getScheduler().cancelTask(task);
    task = -1;

    Lootbox box = this;
    Bukkit.getScheduler().scheduleSyncDelayedTask(CTB.pl, new Runnable() {
      @Override
      public void run() {
        if (CTB.lm.chests.stream().anyMatch(x -> x.loc.getBlockX() == box.loc.getBlockX()
            && x.loc.getBlockY() == box.loc.getBlockY() && x.loc.getBlockZ() == box.loc.getBlockZ())) {
          if (box.loc.getBlock().getType() != Material.CHEST) {
            return;
          }

          Chest chest = (Chest) box.loc.getBlock().getState();

          box.drops.clear();
          for (ItemStack is : chest.getInventory().getContents())
            if (is != null)
              box.drops.add(is);

          chest.getInventory().clear();
          checkBreak(true);
        }
      }
    }, 20 * 60 * 5); // Break chest after 5 minutes
  }

  public boolean checkBreak() {
    return checkBreak(false);
  }

  public boolean checkBreak(boolean force) {
    if (this.loc.getBlock().getType() == Material.CHEST && this.timer <= 0) {
      Chest chest = (Chest) loc.getBlock().getState();

      if (!force) {
        ItemStack[] contents = chest.getInventory().getContents();
        for (int y = 0; y < contents.length; y++)
          if (contents[y] != null)
            return false;

        if (chest.getInventory().getViewers().size() > 1)
          return false;
      }
      Bukkit.getScheduler().scheduleSyncDelayedTask(CTB.pl, new Runnable() {
        @Override
        public void run() {
          if (hg != null) {
            hg.delete();
            hg = null;
          }

          if (as != null) {
            as.remove();
            as = null;
          }

          loc.getBlock().setType(Material.AIR);

          Location loc1 = loc.subtract(0, 1.35, 0);
          loc1.setYaw(180F);
          as = (ArmorStand) loc.getWorld().spawnEntity(loc1, EntityType.ARMOR_STAND, false);
          as.setInvisible(true);
          as.setAI(false);
          as.setGravity(false);
          as.setVelocity(new Vector(0D, 0, 0D));
          as.setInvulnerable(true);
          as.getEquipment().setHelmet(new ItemStack(Material.CHEST, 1), true);
          as.setRotation(180F, as.getLocation().getPitch());

          int task1 = Bukkit.getScheduler().scheduleSyncRepeatingTask(CTB.pl, new Runnable() {
            float yaw = 180F;

            @Override
            public void run() {
              as.setRotation(yaw, as.getLocation().getPitch());
              as.teleport(as.getLocation().add(0, 0.04, 0));
              yaw += 5;
              if (yaw >= 360)
                yaw = 0F;
            }
          }, 0L, 1L);

          Bukkit.getScheduler().scheduleSyncDelayedTask(CTB.pl, new Runnable() {
            @Override
            public void run() {
              Bukkit.getScheduler().cancelTask(task1);
              as.getWorld().strikeLightningEffect(as.getLocation());
              as.getWorld().strikeLightningEffect(as.getLocation());
              as.getWorld().strikeLightningEffect(as.getLocation());
              as.getWorld().spawnParticle(Particle.EXPLOSION_LARGE, as.getLocation(), 0, 0, 0, 0, 0);
              as.getWorld().playSound(as.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 100F, 100F);
              as.remove();

              drops.forEach(x -> {
                as.getWorld().dropItemNaturally(as.getLocation(), x);
              });

              drops.clear();

              List<Lootbox> remove = new ArrayList<Lootbox>();

              for (int i = 0; i < CTB.lm.chests.size(); i++)
                if (CTB.lm.chests.get(i).checkBreak())
                  remove.add(CTB.lm.chests.get(i));

              remove.forEach(x -> CTB.lm.chests.remove(x));

              if (bar != null)
                bar.removeAll();
              bar = null;
            }
          }, 80L);
        }
      }, 10L);

      return true;
    }
    return false;
  }

  public ItemStack getItem() {
    if (tier == 2) {
      return TIER_2[new Random().nextInt(TIER_2.length)];
    } else if (tier == 3) {
      return TIER_3[new Random().nextInt(TIER_3.length)];
    } else if (tier == 4) {
      return TIER_4[new Random().nextInt(TIER_4.length)];
    } else {
      return TIER_1[new Random().nextInt(TIER_1.length)];
    }
  }

  // private static ItemStack buildItem(Material mat, int amount, Map<Enchantment,
  // Integer> enchants) {
  //
  // }

  private static final ItemStack[] TIER_1 = { new ItemStack(Material.STONE, 64), new ItemStack(Material.STONE, 64),
      new ItemStack(Material.STONE, 64), new ItemStack(Material.COMPOSTER, 1), new ItemStack(Material.CAMPFIRE, 1),
      new ItemStack(Material.OAK_LOG, 32), new ItemStack(Material.ACACIA_LOG, 32),
      new ItemStack(Material.JUNGLE_LOG, 32), new ItemStack(Material.BIRCH_LOG, 32),
      new ItemStack(Material.SPRUCE_LOG, 32), new ItemStack(Material.GLOWSTONE, 16),
      new ItemStack(Material.IRON_INGOT, 8), new ItemStack(Material.COAL, 32), new ItemStack(Material.WHEAT, 16),
      new ItemStack(Material.BONE, 8), new ItemStack(Material.LEATHER, 32), new ItemStack(Material.STRING, 16),
      new ItemStack(Material.COOKED_PORKCHOP, 16), new ItemStack(Material.COOKED_CHICKEN, 16),
      new ItemStack(Material.COOKED_BEEF, 16), new ItemStack(Material.BREAD, 32), new ItemStack(Material.APPLE, 8),
      new ItemStack(Material.SHEARS, 1), new ItemStack(Material.STONE_SHOVEL, 1) };

  private static final ItemStack[] TIER_2 = { new ItemStack(Material.DIAMOND, 3) };

  private static final ItemStack[] TIER_3 = { new ItemStack(Material.NETHERITE_INGOT, 3) };

  private static final ItemStack[] TIER_4 = { new ItemStack(Material.WITHER_SKELETON_SKULL, 3) };

}
