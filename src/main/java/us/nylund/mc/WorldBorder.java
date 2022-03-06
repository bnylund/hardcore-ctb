package us.nylund.mc;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.util.Vector;

public class WorldBorder implements Listener {
  private int _x1 = 0, _x2 = 0, _z1 = 0, _z2 = 0;
  private transient boolean enabled;
  public String world;

  public WorldBorder(String world, int lowX, int lowZ, int highX, int highZ, boolean enabled) {
    _x1 = lowX;
    _x2 = highX;
    _z1 = lowZ;
    _z2 = highZ;
    this.world = world;
    this.enabled = enabled;
  }

  public WorldBorder(String world, int lowX, int lowZ, int highX, int highZ) {
    this(world, lowX, lowZ, highX, highZ, true);
  }

  public WorldBorder() {
    this("", 0, 0, 0, 0);
  }

  public void setBoundaries(int lowX, int lowZ, int highX, int highZ) {
    _x1 = lowX;
    _x2 = highX;
    _z1 = lowZ;
    _z2 = highZ;
  }

  public boolean isInside(Location loc) {
    return loc.getBlockX() >= _x1 && loc.getBlockX() <= _x2 && loc.getBlockZ() >= _z1 && loc.getBlockZ() <= _z2;
  }

  public Location getLower() {
    return new Location(getWorld(), _x1, 0, _z1);
  }

  public Location getUpper() {
    return new Location(getWorld(), _x2, 0, _z2);
  }

  public World getWorld() {
    return Bukkit.getWorld(this.world);
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public boolean getEnabled() {
    return this.enabled;
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  public void onMove(PlayerMoveEvent e) {
    if (!this.enabled)
      return;

    if (!isInside(e.getTo())) {
      if (e.getPlayer().isInsideVehicle())
        e.getPlayer().leaveVehicle();
      Location loc = new Location(e.getPlayer().getWorld(), 0, e.getPlayer().getWorld().getSeaLevel(), 0);
      Vector dirToSpawn = loc.toVector().subtract(e.getPlayer().getLocation().toVector());
      e.getPlayer().setVelocity(dirToSpawn.normalize().multiply(5D));
      e.setCancelled(true);
      e.getPlayer().sendMessage(ChatColor.RED + "You can't leave this area!");
    }
  }

  @EventHandler
  public void interact(PlayerInteractEvent e) {
    if (e.getClickedBlock() == null)
      return;

    if (this.enabled && !isInside(e.getClickedBlock().getLocation())) {
      e.setCancelled(true);
      e.getPlayer().sendMessage(ChatColor.RED + "You can't interact with objects outside of the world border!");
    }
  }

  @EventHandler
  public void breakBlock(BlockBreakEvent e) {
    if (this.enabled && !isInside(e.getBlock().getLocation())) {
      e.setCancelled(true);
      e.getPlayer().sendMessage(ChatColor.RED + "You can't break blocks outside of the world border!");
    }
  }

  @EventHandler
  public void placeBlock(BlockPlaceEvent e) {
    if (this.enabled && !isInside(e.getBlockPlaced().getLocation())) {
      e.setCancelled(true);
      e.getPlayer().sendMessage(ChatColor.RED + "You can't place blocks outside of the world border!");
    }
  }
}
