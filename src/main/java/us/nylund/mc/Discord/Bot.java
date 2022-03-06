package us.nylund.mc.Discord;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.bukkit.Bukkit;

import discord4j.common.ReactorResources;
import discord4j.common.util.Snowflake;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.presence.ClientActivity;
import discord4j.core.object.presence.ClientPresence;
import reactor.core.scheduler.Schedulers;
import us.nylund.mc.CTB;
import us.nylund.mc.Discord.Commands.*;
import us.nylund.mc.Team.TeamColor;

public class Bot {
  private GatewayDiscordClient client;
  private List<Thread> threads = new ArrayList<>();
  private static final Map<String, Command> commands = new HashMap<>();

  static {
    commands.put("clear", new ClearCommand());
    commands.put("teams", new TeamsCommand());
    commands.put("team", new SetTeamCommand());
    commands.put("link", new LinkCommand());
  }

  public Bot() {
    CTB.pl.getLogger().log(Level.INFO, "Logging in");
    ReactorResources reactorResources = ReactorResources.builder()
        .timerTaskScheduler(Schedulers.newParallel("my-scheduler")).blockingTaskScheduler(Schedulers.boundedElastic())
        .build();

    // Don't block server thread
    Thread th = new Thread(() -> {
      client = DiscordClientBuilder.create(CTB.pl.getConfig().getString("BotToken"))
          .setReactorResources(reactorResources).build().login().block();

      registerListeners();

      client.updatePresence(ClientPresence.online(ClientActivity.competing("Skull Island"))).block();

      client.onDisconnect().block();
    });
    th.start();
    threads.add(th);
  }

  public void stop() {
    client.logout().block();
    try {
      for (int i = 0; i < threads.size(); i++)
        threads.get(i).join();
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  public GatewayDiscordClient getClient() {
    return client;
  }

  public synchronized void sendNotification(String message) {
    message += "\n\n";
    for (TeamColor val : TeamColor.values())
      message += "<@&" + val.role().asString() + "> ";

    if (this.client != null) {
      String guild = CTB.pl.getConfig().getString("Discord.Guild");
      String noti = CTB.pl.getConfig().getString("Discord.NotificationChannel");
      this.client.getGuildById(Snowflake.of(guild)).block().getChannelById(Snowflake.of(noti)).block().getRestChannel()
          .createMessage(message).block();
    }
  }

  private void registerListeners() {
    client.getEventDispatcher().on(MessageCreateEvent.class).subscribe(event -> {
      final String content = event.getMessage().getContent();

      boolean execute = false;
      List<String> channels = CTB.pl.getConfig().getStringList("Discord.CommandChannels");
      for (int i = 0; i < channels.size(); i++)
        if (channels.get(i).equals(event.getMessage().getChannelId().asString()))
          execute = true;

      if (execute)
        for (final Map.Entry<String, Command> entry : commands.entrySet()) {
          if (content.startsWith('!' + entry.getKey())) {
            entry.getValue().execute(event);
            break;
          }
        }
    });

    client.getEventDispatcher().on(ReadyEvent.class).subscribe(event -> {
      CTB.pl.getLogger().log(Level.INFO, "Bot logged in!");
    });
  }

  public synchronized void tick() {
    if (this.client != null)
      client
          .updatePresence(ClientPresence.online(ClientActivity.competing("Skull Island with "
              + Bukkit.getOnlinePlayers().size() + " player" + (Bukkit.getOnlinePlayers().size() == 1 ? "" : "s"))))
          .block();
  }
}
