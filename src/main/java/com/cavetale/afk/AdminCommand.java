package com.cavetale.afk;

import com.cavetale.core.command.AbstractCommand;
import com.cavetale.core.command.CommandArgCompleter;
import com.cavetale.core.command.CommandWarn;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

final class AdminCommand extends AbstractCommand<AFKPlugin> {
    protected AdminCommand(final AFKPlugin plugin) {
        super(plugin, "afkadmin");
    }

    @Override
    protected void onEnable() {
        rootNode.addChild("info").arguments("[player]")
            .completers(CommandArgCompleter.NULL)
            .description("Player info")
            .senderCaller(this::info);
        rootNode.addChild("list").denyTabCompletion()
            .description("List afk players")
            .senderCaller(this::list);
        rootNode.addChild("rank").denyTabCompletion()
            .description("Rank afk players")
            .senderCaller(this::rank);
    }

    protected boolean info(CommandSender sender, String[] args) {
        if (args.length != 1) return false;
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage("Player not found: " + args[0]);
            return true;
        }
        Session session = plugin.sessionOf(target);
        sender.sendMessage(target.getName() + ":"
                           + " noMove=" + session.getNoMove()
                           + " noPitch=" + session.getNoPitch()
                           + " noYaw=" + session.getNoYaw()
                           + " idle=" + session.idleTicks
                           + " afk=" + session.afk);
        return true;
    }

    protected boolean list(CommandSender sender, String[] args) {
        if (args.length != 0) return false;
        List<String> list = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (plugin.sessionOf(player).afk) {
                list.add(player.getName());
            }
        }
        Collections.sort(list);
        sender.sendMessage(list.size() + " players are afk: " + String.join(" ", list));
        return true;
    }

    protected boolean rank(CommandSender sender, String[] args) {
        if (args.length != 0) return false;
        List<Session> sessions = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            sessions.add(plugin.sessionOf(player));
        }
        if (sessions.isEmpty()) throw new CommandWarn("No players!");
        Collections.sort(sessions, (b, a) -> Integer.compare(a.idleTicks, b.idleTicks));
        for (Session session : sessions) {
            sender.sendMessage(session.idleTicks + " " + session.name
                               + (Session.HANDLE_MOVEMENT ? (" noMove=" + session.getNoMove()) : "")
                               + " noPitch=" + session.getNoPitch()
                               + " noYaw=" + session.getNoYaw());
        }
        return true;
    }
}
