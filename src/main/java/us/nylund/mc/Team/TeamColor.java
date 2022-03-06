package us.nylund.mc.Team;

import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;

import discord4j.common.util.Snowflake;

public enum TeamColor {
  PURPLE, RED, GREEN, YELLOW, BLUE, PINK, ORANGE, BLACK, WHITE;

  public String string() {
    return this.toString().charAt(0) + this.toString().substring(1).toLowerCase();
  }

  public String full() {
    return this.chatcolor() + ChatColor.BOLD.toString() + string();
  }

  public Snowflake role() {
    if (this == TeamColor.PURPLE)
      return Snowflake.of(947325130990833715L);
    if (this == TeamColor.RED)
      return Snowflake.of(947325178063515698L);
    if (this == TeamColor.GREEN)
      return Snowflake.of(947325221961093231L);
    if (this == TeamColor.YELLOW)
      return Snowflake.of(947325247391141999L);
    if (this == TeamColor.BLUE)
      return Snowflake.of(947325345047150612L);
    if (this == TeamColor.PINK)
      return Snowflake.of(947325397782114395L);
    if (this == TeamColor.ORANGE)
      return Snowflake.of(947325458087833657L);
    if (this == TeamColor.BLACK)
      return Snowflake.of(947325799101521971L);
    return Snowflake.of(947325660634972240L);
  }

  public Material material() {
    if (this == TeamColor.PURPLE)
      return Material.PURPLE_WOOL;
    if (this == TeamColor.RED)
      return Material.RED_WOOL;
    if (this == TeamColor.GREEN)
      return Material.GREEN_WOOL;
    if (this == TeamColor.YELLOW)
      return Material.YELLOW_WOOL;
    if (this == TeamColor.BLUE)
      return Material.BLUE_WOOL;
    if (this == TeamColor.PINK)
      return Material.PINK_WOOL;
    if (this == TeamColor.ORANGE)
      return Material.ORANGE_WOOL;
    if (this == TeamColor.BLACK)
      return Material.BLACK_WOOL;
    return Material.WHITE_WOOL;
  }

  public ChatColor chatcolor() {
    if (this == TeamColor.PURPLE)
      return ChatColor.DARK_PURPLE;
    if (this == TeamColor.RED)
      return ChatColor.RED;
    if (this == TeamColor.GREEN)
      return ChatColor.GREEN;
    if (this == TeamColor.YELLOW)
      return ChatColor.YELLOW;
    if (this == TeamColor.BLUE)
      return ChatColor.BLUE;
    if (this == TeamColor.PINK)
      return ChatColor.LIGHT_PURPLE;
    if (this == TeamColor.ORANGE)
      return ChatColor.GOLD;
    if (this == TeamColor.BLACK)
      return ChatColor.DARK_GRAY;
    return ChatColor.WHITE;
  }

  public Color color() {
    if (this == TeamColor.PURPLE)
      return Color.PURPLE;
    if (this == TeamColor.RED)
      return Color.RED;
    if (this == TeamColor.GREEN)
      return Color.GREEN;
    if (this == TeamColor.YELLOW)
      return Color.YELLOW;
    if (this == TeamColor.BLUE)
      return Color.BLUE;
    if (this == TeamColor.PINK)
      return Color.fromRGB(255, 100, 240);
    if (this == TeamColor.ORANGE)
      return Color.ORANGE;
    if (this == TeamColor.BLACK)
      return Color.BLACK;
    return Color.WHITE;
  }
}
