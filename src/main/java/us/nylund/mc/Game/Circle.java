package us.nylund.mc.Game;

import org.bukkit.Location;

public class Circle {

  public int radius;
  public int x;
  public int z;

  public Circle(double angle, int distance, int radius, Location loc) {
    this.radius = radius;
    this.x = loc.getBlockX();
    this.z = loc.getBlockZ();

    // Using angle and distance, calculate offset to center
    double zOffset = distance * Math.sin(angle);
    double xOffset = distance * Math.cos(angle);

    // Bukkit.broadcastMessage("xOffset: " + xOffset + ", zOffset: " + zOffset);

    if (angle >= (Math.PI / 2) && angle <= ((3 * Math.PI) / 2)) {
      this.x -= xOffset;
    } else {
      this.x += xOffset;
    }

    if (angle >= 0 && angle <= Math.PI) {
      this.z += zOffset;
    } else {
      this.z -= zOffset;
    }
  }

  // COSTLY. only check for inCircle every second, not on every move event
  public boolean inCircle(Location loc) {
    // Ignore y
    double distance = Math
        .sqrt((x - loc.getBlockX()) * (x - loc.getBlockX()) + (z - loc.getBlockZ()) * (z - loc.getBlockZ()));
    if (distance <= radius)
      return true;
    return false;
  }
}
