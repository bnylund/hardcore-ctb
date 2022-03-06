package us.nylund.mc.Util;

import org.bukkit.Location;

public class SaveLocation {
  public String world;
  public double x, y, z;
  public float yaw, pitch;

  public SaveLocation(String world, double x, double y, double z, float yaw, float pitch) {
    this.world = world;
    this.x = x;
    this.y = y;
    this.z = z;
    this.yaw = yaw;
    this.pitch = pitch;
  }

  public SaveLocation(Location loc) {
    this.world = loc.getWorld().getName();
    this.x = loc.getX();
    this.y = loc.getY();
    this.z = loc.getZ();
    this.yaw = loc.getYaw();
    this.pitch = loc.getPitch();
  }
}
