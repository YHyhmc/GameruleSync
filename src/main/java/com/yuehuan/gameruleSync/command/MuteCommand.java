package com.yuehuan.gameruleSync.command;

import com.yuehuan.gameruleSync.manager.MuteManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class MuteCommand implements CommandExecutor, TabCompleter {

    private final MuteManager muteManager;

    public MuteCommand(MuteManager muteManager) {
        this.muteManager = muteManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sender.sendMessage(muteManager.msg("mute.usage"));
            return true;
        }

        if (args[0].equalsIgnoreCase("status")) {
            return handleStatus(sender, args);
        }

        if (args[0].equalsIgnoreCase("team")) {
            return handleTeam(sender, args);
        }

        return handlePlayer(sender, args);
    }

    private boolean handlePlayer(CommandSender sender, String[] args) {
        String playerName = args[0];
        String type = args.length >= 2 ? args[1] : "all";
        int flags = MuteManager.parseFlags(type);

        int current = muteManager.getMuteFlags(playerName);
        if ((current & flags) == flags) {
            muteManager.unmute(playerName, flags);
            sender.sendMessage(muteManager.msg("mute.unmute_success", playerName,
                    muteManager.flagsToMessage(flags)));
        } else {
            muteManager.mute(playerName, flags);
            sender.sendMessage(muteManager.msg("mute.mute_success", playerName,
                    muteManager.flagsToMessage(flags)));
        }

        if (Bukkit.getPlayerExact(playerName) == null) {
            sender.sendMessage(muteManager.msg("mute.player_offline", playerName));
        }
        return true;
    }

    private boolean handleTeam(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(muteManager.msg("mute.team_usage"));
            return true;
        }

        String teamName = args[1];
        Team team = muteManager.getTeamByName(teamName);
        if (team == null) {
            sender.sendMessage(muteManager.msg("mute.team_not_found", teamName));
            return true;
        }

        String type = args.length >= 3 ? args[2] : "all";
        int flags = MuteManager.parseFlags(type);

        boolean allMuted = true;
        for (String entry : team.getEntries()) {
            int current = muteManager.getMuteFlags(entry);
            if ((current & flags) != flags) {
                allMuted = false;
                break;
            }
        }

        if (allMuted) {
            muteManager.unmuteTeam(teamName, flags);
            sender.sendMessage(muteManager.msg("mute.team_unmute_success", teamName,
                    muteManager.flagsToMessage(flags)));
        } else {
            muteManager.muteTeam(teamName, flags);
            sender.sendMessage(muteManager.msg("mute.team_mute_success", teamName,
                    muteManager.flagsToMessage(flags)));
        }
        return true;
    }

    private boolean handleStatus(CommandSender sender, String[] args) {
        if (args.length >= 2) {
            String playerName = args[1];
            int flags = muteManager.getMuteFlags(playerName);
            sender.sendMessage(muteManager.msg("mute.status_format", playerName,
                    muteManager.flagsToMessage(flags)));
        } else {
            List<String> mutedNames = new ArrayList<>();
            for (Player player : Bukkit.getOnlinePlayers()) {
                int flags = muteManager.getMuteFlags(player.getName());
                if (flags > 0) {
                    mutedNames.add(player.getName());
                }
            }
            if (mutedNames.isEmpty()) {
                sender.sendMessage(muteManager.msg("mute.no_muted_players"));
            } else {
                sender.sendMessage(muteManager.msg("mute.muted_list_header"));
                for (String name : mutedNames) {
                    int flags = muteManager.getMuteFlags(name);
                    sender.sendMessage(muteManager.msg("mute.muted_list_entry", name,
                            muteManager.flagsToMessage(flags)));
                }
            }
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                 @NotNull String alias, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            String current = args[0].toLowerCase();
            if ("status".startsWith(current)) completions.add("status");
            if ("team".startsWith(current)) completions.add("team");
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getName().toLowerCase().startsWith(current)) {
                    completions.add(player.getName());
                }
            }
        } else if (args.length == 2) {
            String current = args[1].toLowerCase();
            if (args[0].equalsIgnoreCase("team")) {
                for (String teamName : muteManager.getTeamNames()) {
                    if (teamName.toLowerCase().startsWith(current)) {
                        completions.add(teamName);
                    }
                }
            } else if (args[0].equalsIgnoreCase("status")) {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.getName().toLowerCase().startsWith(current)) {
                        completions.add(player.getName());
                    }
                }
            } else {
                if ("speak".startsWith(current)) completions.add("speak");
                if ("shout".startsWith(current)) completions.add("shout");
                if ("all".startsWith(current)) completions.add("all");
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("team")) {
            String current = args[2].toLowerCase();
            if ("speak".startsWith(current)) completions.add("speak");
            if ("shout".startsWith(current)) completions.add("shout");
            if ("all".startsWith(current)) completions.add("all");
        }

        return completions;
    }
}
