package us.nylund.mc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.entity.WitherSkeleton;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import us.nylund.mc.Team.Team;
import us.nylund.mc.Team.TeamColor;
import us.nylund.mc.Util.KeyValue;
import us.nylund.mc.Util.SetExpFix;

public class CombatLog implements Listener {
  private boolean enabled;
  private HashMap<String, Integer> log;
  private HashMap<String, KeyValue<Entity, KeyValue<ItemStack[], Integer>>> discon;
  private HashMap<String, TeamColor> killed;

  public CombatLog(boolean enabled) {
    this.enabled = enabled;
    this.log = new HashMap<String, Integer>();
    this.discon = new HashMap<String, KeyValue<Entity, KeyValue<ItemStack[], Integer>>>();
    killed = new HashMap<String, TeamColor>();
  }

  public CombatLog() {
    this(true);
  }

  public boolean inLog(Player p) {
    return log.containsKey(p.getUniqueId().toString());
  }

  public boolean inLog(String uuid) {
    return log.keySet().stream().anyMatch(x -> x.equals(uuid));
  }

  public Integer remove(String uuid) {
    return log.remove(uuid);
  }

  public boolean isEnabled() {
    return this.enabled;
  }

  public void setEnabled(boolean state) {
    this.enabled = state;
  }

  public void tick() {
    if (!enabled || !CTB.gm.started) {
      log.clear();
      discon.clear();
      killed.clear();
      return;
    }

    List<String> toRemove = new ArrayList<String>();
    log.keySet().forEach(x -> {
      OfflinePlayer pl = Bukkit.getOfflinePlayer(UUID.fromString(x));
      Player p = pl.isOnline() ? (Player) pl : null;
      if (p != null)
        p.spigot().sendMessage(ChatMessageType.ACTION_BAR,
            new TextComponent(ChatColor.RED + "Combat Log: " + ChatColor.YELLOW.toString() + log.get(x) + "s"));
      log.replace(x, log.get(x) - 1);
      if (log.get(x) < 0) {
        toRemove.add(x);

        // Remove entity from discon
        if (discon.containsKey(x)) {
          KeyValue<Entity, KeyValue<ItemStack[], Integer>> plog = discon.get(x);
          if (plog.getKey() != null && !plog.getKey().isDead()) {
            plog.getKey().remove();
          }
          this.discon.remove(x);
        }
      }
    });
    toRemove.forEach(x -> {
      log.remove(x);
      OfflinePlayer pl = Bukkit.getOfflinePlayer(UUID.fromString(x));
      Player p = pl.isOnline() ? (Player) pl : null;
      if (p != null)
        p.sendMessage(ChatColor.translateAlternateColorCodes('&', "&4[&c&l!&r&4] &aYou are no longer combat logged."));
    });
  }

  private boolean checkKill(OfflinePlayer p, EntityDamageByEntityEvent e, Team team) {
    if (p == null || e == null)
      return false;

    Optional<Team> pTeam = CTB.tm.getPlayerTeam(p.getUniqueId().toString());
    if (!pTeam.isPresent())
      return false;

    // Player damage
    if (e.getDamager().getType() == EntityType.PLAYER) {
      Player dmg = (Player) e.getDamager();
      Optional<Team> teamOpt = CTB.tm.getPlayerTeam(dmg.getUniqueId().toString());

      if (teamOpt.isPresent() && pTeam.isPresent()) {
        team = teamOpt.get();
        team.kills++;
        Bukkit.broadcastMessage(pTeam.get().color().chatcolor() + p.getName() + ChatColor.GOLD + " was slain by "
            + team.color().chatcolor() + dmg.getName() + ChatColor.GOLD + "!");

        for (Player pl : Bukkit.getOnlinePlayers())
          pl.playSound(pl.getLocation(), Sound.ENTITY_BLAZE_DEATH, 100.0F, 100.0F);

        return true;
      }
    }

    // Check for tnt launcher
    if (e.getDamager() instanceof TNTPrimed) {
      TNTPrimed pro = (TNTPrimed) e.getDamager();
      if (pro.getSource() instanceof Player) {
        Player dmg = (Player) pro.getSource();
        Optional<Team> teamOpt = CTB.tm.getPlayerTeam(dmg.getUniqueId().toString());
        if (teamOpt.isPresent()) {
          team = teamOpt.get();
          team.kills++;
          Bukkit.broadcastMessage(pTeam.get().color().chatcolor() + p.getName() + ChatColor.GOLD + " was slain by "
              + team.color().chatcolor() + dmg.getName() + ChatColor.GOLD + "!");

          for (Player pl : Bukkit.getOnlinePlayers())
            pl.playSound(pl.getLocation(), Sound.ENTITY_BLAZE_DEATH, 100.0F, 100.0F);

          return true;
        }
      }
    }

    // Check arrows, snowballs, etc
    if (e.getDamager() instanceof Projectile) {
      Projectile pro = (Projectile) e.getDamager();
      if (pro.getShooter() instanceof Player) {
        Player dmg = (Player) pro.getShooter();
        Optional<Team> teamOpt = CTB.tm.getPlayerTeam(dmg.getUniqueId().toString());
        if (teamOpt.isPresent()) {
          team = teamOpt.get();
          team.kills++;
          Bukkit.broadcastMessage(pTeam.get().color().chatcolor() + p.getName() + ChatColor.GOLD + " was slain by "
              + team.color().chatcolor() + dmg.getName() + ChatColor.GOLD + "!");

          for (Player pl : Bukkit.getOnlinePlayers())
            pl.playSound(pl.getLocation(), Sound.ENTITY_BLAZE_DEATH, 100.0F, 100.0F);

          return true;
        }
      }
    }
    return false;
  }

  private void checkAddOrReplace(Player p) {
    if (CTB.gm.inRespawn.contains(p.getUniqueId().toString())) {
      log.remove(p.getUniqueId().toString());
      return;
    }

    if (!enabled) {
      log.clear();
      discon.clear();
      killed.clear();
      return;
    }

    Optional<Team> team = CTB.tm.getPlayerTeam(p.getUniqueId().toString());
    if (team.isPresent() && team.get().out.contains(p.getUniqueId().toString())) {
      log.remove(p.getUniqueId().toString());
      return;
    }

    if (log.containsKey(p.getUniqueId().toString())) {
      p.spigot().sendMessage(ChatMessageType.ACTION_BAR,
          new TextComponent(ChatColor.RED + "Combat Log: " + ChatColor.YELLOW.toString() + "20s"));
      log.replace(p.getUniqueId().toString(), 20);
    } else {
      p.sendMessage(
          ChatColor.translateAlternateColorCodes('&', "&4[&c&l!&r&4] &cYou are now combat logged for 20 seconds."));
      p.spigot().sendMessage(ChatMessageType.ACTION_BAR,
          new TextComponent(ChatColor.RED + "Combat Log: " + ChatColor.YELLOW.toString() + "20s"));
      log.put(p.getUniqueId().toString(), 20);
    }
  }

  @EventHandler
  public void damage(EntityDamageEvent e) {
    if (e.getEntityType() == EntityType.PLAYER) {
      Player p = (Player) e.getEntity();
      if (enabled) {
        if (p.getHealth() - e.getFinalDamage() <= 0)
          return;

        if (e instanceof EntityDamageByEntityEvent) {
          if (((EntityDamageByEntityEvent) e).getDamager() instanceof Player) {
            Player pl = (Player) ((EntityDamageByEntityEvent) e).getDamager();
            Optional<Team> team = CTB.tm.getPlayerTeam(pl.getUniqueId().toString());
            if (!CTB.gm.inRespawn.contains(pl.getUniqueId().toString()) && team.isPresent()
                && !team.get().out.contains(pl.getUniqueId().toString()))
              checkAddOrReplace(p);
          } else {
            checkAddOrReplace(p);
          }
        } else
          checkAddOrReplace(p);
      }
    } else if (e.getEntityType() == EntityType.WITHER_SKELETON) {
      if (enabled) {
        // List of players that belong to this wither skeleton (0 or 1)
        List<String> players = discon.keySet().stream()
            .filter(x -> discon.get(x).getKey().getEntityId() == e.getEntity().getEntityId())
            .collect(Collectors.toList());

        WitherSkeleton ent = (WitherSkeleton) e.getEntity();
        if (players.size() > 0) {
          String p = players.get(0);
          if (ent.getHealth() - e.getFinalDamage() <= 0) {
            e.setCancelled(true);

            // Remove from log, drop inventory, remove entity, add to killed
            log.remove(p);
            for (int i = 0; i < discon.get(p).getValue().getKey().length; i++) {
              if (discon.get(p).getValue().getKey()[i] != null)
                ent.getWorld().dropItemNaturally(ent.getLocation(), discon.get(p).getValue().getKey()[i]);
            }

            int xp = discon.get(p).getValue().getValue();
            if (xp > 5) {
              ItemStack bottle = CustomItem.getXPBottle(discon.get(p).getValue().getValue(),
                  CTB.pl.getConfig().getString(p + ".name"));
              ent.getWorld().dropItem(ent.getLocation(), bottle);
            }

            ent.remove();
            discon.remove(p);

            Team team = null;

            // Reward kill (if applicable)
            if (!(e instanceof EntityDamageByEntityEvent
                && checkKill(Bukkit.getOfflinePlayer(UUID.fromString(p)), (EntityDamageByEntityEvent) e, team))) {

              // If no player kill present
              Optional<Team> pTeam = CTB.tm.getPlayerTeam(p);
              if (pTeam.isPresent()) {
                Bukkit.broadcastMessage(pTeam.get().color().chatcolor() + CTB.pl.getConfig().getString(p + ".name")
                    + ChatColor.GOLD + " died!");

                for (Player pl : Bukkit.getOnlinePlayers())
                  pl.playSound(pl.getLocation(), Sound.ENTITY_BLAZE_DEATH, 100.0F, 100.0F);
              }
            }

            killed.put(p, team == null ? null : team.color());
          } else {
            if (log.containsKey(p)) {
              log.replace(p, 20);
            } else {
              log.put(p, 20);
            }
          }
        }
      }
    }

    // Player who attacked also gets logged
    if (e instanceof EntityDamageByEntityEvent && enabled) {
      EntityDamageByEntityEvent ed = (EntityDamageByEntityEvent) e;
      if (ed.getDamager().getType() == EntityType.PLAYER
          && !CTB.gm.inRespawn.contains(((Player) ed.getDamager()).getUniqueId().toString())) {
        checkAddOrReplace((Player) ed.getDamager());
      }
    }
  }

  @EventHandler
  public void onShoot(ProjectileLaunchEvent e) {
    if (e.getEntity().getShooter() instanceof Player) {
      Player pl = (Player) e.getEntity().getShooter();
      Optional<Team> team = CTB.tm.getPlayerTeam(pl.getUniqueId().toString());
      if (!CTB.gm.inRespawn.contains(pl.getUniqueId().toString()) && team.isPresent()
          && !team.get().out.contains(pl.getUniqueId().toString()))
        checkAddOrReplace(pl);
    }
  }

  @EventHandler
  public void join(PlayerJoinEvent e) {
    if (killed.containsKey(e.getPlayer().getUniqueId().toString())) {
      e.getPlayer().getInventory().clear();
      e.getPlayer().updateInventory();
      SetExpFix.setTotalExperience(e.getPlayer(), 0);
      e.getPlayer().setHealth(20);
      e.getPlayer().playSound(e.getPlayer().getLocation(), Sound.ENTITY_BLAZE_DEATH, 100.0F, 100.0F);
      e.getPlayer().sendMessage(ChatColor.translateAlternateColorCodes('&', "&4[&c&l!&r&4] &eYour guardian died!"));
      Optional<Team> team = CTB.tm.getPlayerTeam(e.getPlayer().getUniqueId().toString());
      Optional<Team> other = CTB.tm.getTeam(killed.get(e.getPlayer().getUniqueId().toString()));
      if (team.isPresent()) {
        team.get().doDeath(e.getPlayer(), false, other.isPresent() ? other.get() : null);
      } else {
        CTB.gm.respawn(e.getPlayer());
      }
      killed.remove(e.getPlayer().getUniqueId().toString());
    }

    if (discon.containsKey(e.getPlayer().getUniqueId().toString())) {
      WitherSkeleton ent = (WitherSkeleton) discon.get(e.getPlayer().getUniqueId().toString()).getKey();
      e.getPlayer().teleport(ent.getLocation());
      ent.remove();
      discon.remove(e.getPlayer().getUniqueId().toString());
    }
  }

  @EventHandler
  public void disconnect(PlayerQuitEvent e) {
    if (log.containsKey(e.getPlayer().getUniqueId().toString())) {
      e.getPlayer().closeInventory();

      WitherSkeleton ent = (WitherSkeleton) e.getPlayer().getWorld().spawnEntity(e.getPlayer().getLocation(),
          EntityType.WITHER_SKELETON, false);
      ent.setRemoveWhenFarAway(false);
      ent.setCustomName(ChatColor.YELLOW + e.getPlayer().getName());
      ent.setCustomNameVisible(true);
      ent.setHealth(ent.getAttribute(Attribute.GENERIC_MAX_HEALTH).getDefaultValue());
      ent.getEquipment().setItemInMainHand(e.getPlayer().getInventory().getItemInMainHand());
      ent.setVelocity(e.getPlayer().getVelocity());
      ent.setFallDistance(e.getPlayer().getFallDistance());

      List<Entity> targets = ent.getNearbyEntities(25, 25, 25).stream().filter(x -> x.getType() == EntityType.PLAYER)
          .collect(Collectors.toList());

      if (targets.size() > 0) {
        // FIGHT FIGHT FIGHT (but not cows, only players)
        ent.setTarget((LivingEntity) targets.get(0));
      }

      ent.getEquipment().setArmorContents(e.getPlayer().getInventory().getArmorContents());

      discon.put(e.getPlayer().getUniqueId().toString(),
          new KeyValue<Entity, KeyValue<ItemStack[], Integer>>(ent, new KeyValue<ItemStack[], Integer>(
              e.getPlayer().getInventory().getContents(), SetExpFix.getTotalExperience(e.getPlayer()) / 3)));
    }
  }
}
