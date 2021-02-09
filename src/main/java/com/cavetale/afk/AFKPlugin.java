package com.cavetale.afk;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.java.JavaPlugin;

public final class AFKPlugin extends JavaPlugin implements CommandExecutor, Listener {
    @Getter private static AFKPlugin instance;
    private int idleThreshold = 20 * 60 * 2;
    private int kickThreshold = 20 * 60 * 5;
    private boolean kickEnabled;
    private List<String> noKickWorlds;

    @Override
    public void onEnable() {
        instance = this;
        for (Player player : Bukkit.getOnlinePlayers()) {
            enter(player);
        }
        loadConf();
        Bukkit.getPluginManager().registerEvents(this, this);
        Bukkit.getScheduler().runTaskTimer(this, this::timer, 1, 1);
    }

    @Override
    public void onDisable() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            exit(player);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command,
                             String alias, String[] args) {
        if (args.length == 0) {
            Player player = sender instanceof Player ? (Player) sender : null;
            if (player == null) return false;
            Session session = sessionOf(player);
            if (session.afk) return true;
            session.afk = true;
            setAfk(player, true);
            return true;
        }
        switch (args[0]) {
        case "info": {
            if (args.length != 2) return false;
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage("Player not found: " + args[1]);
                return true;
            }
            Session session = sessionOf(target);
            sender.sendMessage(target.getName() + ":"
                               + " noMove=" + session.getNoMove()
                               + " noView=" + session.getNoView()
                               + " idle=" + session.idleTicks
                               + " afk=" + session.afk);
            return true;
        }
        case "list": {
            if (args.length != 1) return false;
            List<String> list = new ArrayList<>();
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (sessionOf(player).afk) {
                    list.add(player.getName());
                }
            }
            Collections.sort(list);
            sender.sendMessage(list.size() + " players are afk: " + String.join(" ", list));
            return true;
        }
        default: return false;
        }
    }

    void setAfk(Player player, boolean afk) {
        player.setPlayerListName(afk
                                 ? (ChatColor.GRAY + player.getName()
                                    + ChatColor.DARK_GRAY + "(afk)")
                                 : null);
        player.setAffectsSpawning(!afk);
        player.setSleepingIgnored(afk);
    }

    void enter(Player player) {
        setAfk(player, false);
    }

    void exit(Player player) {
        setAfk(player, false);
        clearSession(player);
    }

    Session sessionOf(final Player player) {
        for (MetadataValue meta : player.getMetadata(Session.KEY)) {
            if (meta.getOwningPlugin().equals(this)) {
                Object value = meta.value();
                if (value instanceof Session) {
                    return (Session) value;
                }
                break;
            }
        }
        Session session = new Session();
        player.setMetadata(Session.KEY, new FixedMetadataValue(this, session));
        return session;
    }

    void clearSession(final Player player) {
        player.removeMetadata(Session.KEY, this);
    }

    void timer() {
        double tps = Bukkit.getTPS()[0];
        for (Player player : Bukkit.getOnlinePlayers()) {
            Session session = sessionOf(player);
            session.update(player);
            if (session.afk) {
                if (session.idleTicks == 0) {
                    setAfk(player, false);
                    session.afk = false;
                }
            } else {
                if (session.idleTicks >= idleThreshold) {
                    setAfk(player, true);
                    session.afk = true;
                }
            }
            if (kickEnabled && tps < 17.0 && session.idleTicks >= kickThreshold
                && !noKickWorlds.contains(player.getWorld().getName()) && !player.hasPermission("afk.nokick")) {
                getLogger().info("Kicking player: " + player.getName());
                player.kickPlayer("AFK: Away from keyboard");
                clearSession(player);
            }
        }
    }

    @EventHandler
    void onPlayerQuit(PlayerQuitEvent event) {
        exit(event.getPlayer());
    }

    @EventHandler
    void onPlayerJoin(PlayerJoinEvent event) {
        enter(event.getPlayer());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    void onPlayerTeleport(PlayerTeleportEvent event) {
        sessionOf(event.getPlayer()).setLocation(event.getTo());
    }

    public static boolean isAfk(Player player) {
        return instance.sessionOf(player).afk;
    }

    public void loadConf() {
        saveDefaultConfig();
        reloadConfig();
        kickEnabled = getConfig().getBoolean("Kick.Enabled");
        noKickWorlds = getConfig().getStringList("Kick.NoKickWorlds");
    }
}
