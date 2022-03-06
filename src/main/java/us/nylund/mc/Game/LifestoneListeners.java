package us.nylund.mc.Game;

import java.util.List;
import java.util.Optional;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

import us.nylund.mc.CTB;
import us.nylund.mc.CustomItem;
import us.nylund.mc.Events.LifestoneBreakEvent;
import us.nylund.mc.Events.LifestonePlaceEvent;
import us.nylund.mc.Team.Team;
import us.nylund.mc.Team.TeamColor;

public class LifestoneListeners implements Listener {

  @EventHandler(priority = EventPriority.HIGHEST)
  public void onPlace(BlockPlaceEvent e) {
    if (e.isCancelled())
      return;

    TeamColor col = CustomItem.isLifestone(e.getItemInHand());
    if (col != null) {
      Optional<Team> team = CTB.tm.getPlayerTeam(e.getPlayer().getUniqueId().toString());
      if (team.isPresent()) {
        if (CTB.gm.lifestone(team.get().color()).isPresent()) {
          Lifestone cur = CTB.gm.lifestone(team.get().color()).get();
          if (!col.equals(team.get().color()) && !cur.touching(e.getBlock().getLocation())) {
            e.setCancelled(true);
            e.getPlayer().sendMessage(ChatColor.RED + "You can only place opponent lifestones next to yours!");
            return;
          }
        } else if (!col.equals(team.get().color())) {
          e.setCancelled(true);
          e.getPlayer().sendMessage(ChatColor.RED + "You can only place opponent lifestones next to yours!");
          return;
        }
        Lifestone ls = new Lifestone(e.getBlockPlaced().getLocation(), col, team.get().color());
        LifestonePlaceEvent event = new LifestonePlaceEvent(ls);
        Bukkit.getServer().getPluginManager().callEvent(event);
      }
    }
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  public void onBreak(BlockBreakEvent e) {
    if (e.isCancelled())
      return;

    List<Lifestone> lifestones = CTB.gm.lifestones();
    for (int i = 0; i < lifestones.size(); i++) {
      if (lifestones.get(i).location().equals(e.getBlock().getLocation())) {
        e.setDropItems(false);

        Optional<Team> team = CTB.tm.getPlayerTeam(e.getPlayer().getUniqueId().toString());
        if (team.isPresent()) {
          LifestoneBreakEvent event = new LifestoneBreakEvent(lifestones.get(i), team.get().color());
          Bukkit.getServer().getPluginManager().callEvent(event);
          return;
        }

      }
    }
  }
}
