package com.cavetale.afk;

import com.winthier.title.TitlePlugin;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
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
import org.bukkit.plugin.java.JavaPlugin;

public final class AFKPlugin extends JavaPlugin implements CommandExecutor, Listener {
    @Getter private static AFKPlugin instance;
    private AdminCommand adminCommand = new AdminCommand(this);
    private final Map<UUID, Session> sessionsMap = new HashMap<>();
    private final long deleteSessionsAfter = Duration.ofMinutes(5).toMillis();
    private int idleThreshold = 20 * 60 * 5;
    private int kickThreshold = 20 * 60 * 10;
    private boolean kickEnabled;
    private List<String> noKickWorlds;

    @Override
    public void onEnable() {
        instance = this;
        for (Player player : Bukkit.getOnlinePlayers()) {
            applyAfkEffects(player, false);
        }
        loadConf();
        Bukkit.getPluginManager().registerEvents(this, this);
        Bukkit.getScheduler().runTaskTimer(this, this::tick, 1, 1);
        adminCommand.enable();
    }

    @Override
    public void onDisable() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            applyAfkEffects(player, false);
        }
        sessionsMap.clear();
    }

    private void loadConf() {
        saveDefaultConfig();
        reloadConfig();
        kickEnabled = getConfig().getBoolean("Kick.Enabled");
        noKickWorlds = getConfig().getStringList("Kick.NoKickWorlds");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 0) return false;
        Player player = sender instanceof Player ? (Player) sender : null;
        if (player == null) {
            sender.sendMessage("[afk:afk] player expected");
            return true;
        }
        Session session = sessionOf(player);
        if (session.afk) return true;
        session.afk = true;
        applyAfkEffects(player, true);
        return true;
    }

    protected void applyAfkEffects(Player player, boolean afk) {
        if (afk) {
            TitlePlugin.getInstance().setPlayerListSuffix(player, Component.text("(afk)", NamedTextColor.GRAY));
        } else {
            TitlePlugin.getInstance().setPlayerListSuffix(player, (Component) null);
        }
        player.setAffectsSpawning(!afk);
        player.setSleepingIgnored(afk);
    }

    protected Session sessionOf(final Player player) {
        return sessionsMap.computeIfAbsent(player.getUniqueId(), u -> new Session(player.getName()));
    }

    protected void tick() {
        double tps = Bukkit.getTPS()[0];
        long now = System.currentTimeMillis();
        for (Player player : Bukkit.getOnlinePlayers()) {
            Session session = sessionOf(player);
            session.lastSeen = now;
            session.update(player);
            if (session.afk) {
                if (session.idleTicks == 0) {
                    applyAfkEffects(player, false);
                    session.afk = false;
                }
            } else {
                if (session.idleTicks >= idleThreshold) {
                    applyAfkEffects(player, true);
                    session.afk = true;
                }
            }
            if (kickEnabled && tps < 17.0 && session.idleTicks >= kickThreshold
                && !noKickWorlds.contains(player.getWorld().getName()) && !player.hasPermission("afk.nokick")) {
                getLogger().info("Kicking player: " + player.getName());
                player.kick(Component.text("AFK: Away from keyboard", NamedTextColor.YELLOW));
                sessionsMap.remove(player.getUniqueId());
            }
        }
        long then = now - deleteSessionsAfter;
        sessionsMap.values().removeIf(s -> s.lastSeen < then);
    }

    @EventHandler
    protected void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        sessionOf(player).afk = false;
        applyAfkEffects(player, false);
        sessionsMap.remove(player.getUniqueId());
    }

    @EventHandler
    protected void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        sessionOf(player).afk = false;
        applyAfkEffects(player, false);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    protected void onPlayerTeleport(PlayerTeleportEvent event) {
        sessionOf(event.getPlayer()).setLocation(event.getTo());
    }

    public static boolean isAfk(Player player) {
        return instance.sessionOf(player).afk;
    }
}
