package com.cavetale.afk;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.java.JavaPlugin;

public final class AFKPlugin extends JavaPlugin implements CommandExecutor, Listener {
    int idleThreshold = 20 * 60;

    @Override
    public void onEnable() {
        for (Player player : getServer().getOnlinePlayers()) {
            enter(player);
        }
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getScheduler().runTaskTimer(this, this::timer, 1, 1);
    }

    @Override
    public void onDisable() {
        for (Player player : getServer().getOnlinePlayers()) {
            exit(player);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command,
                             String alias, String[] args) {
        if (args.length != 0) return false;
        Player player = sender instanceof Player ? (Player) sender : null;
        if (player == null) return false;
        Session session = sessionOf(player);
        if (session.afk) return true;
        session.afk = true;
        setAfk(player, true);
        return true;
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
        for (Player player : getServer().getOnlinePlayers()) {
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
}
