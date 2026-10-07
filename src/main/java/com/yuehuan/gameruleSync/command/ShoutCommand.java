package com.yuehuan.gameruleSync.command;

import com.yuehuan.gameruleSync.manager.MuteManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class ShoutCommand implements CommandExecutor, TabCompleter {

    private final MuteManager muteManager;

    public ShoutCommand(MuteManager muteManager) {
        this.muteManager = muteManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(muteManager.msg("mute.player_only"));
            return true;
        }

        if (muteManager.isMutedFromShouting(player)) {
            player.sendMessage(muteManager.msg("mute.cannot_shout"));
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(muteManager.msg("mute.sshout_usage"));
            return true;
        }

        String content = String.join(" ", args);
        String message = muteManager.formatShoutMessage(player.getName(), content);
        Bukkit.broadcastMessage(message);

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                 @NotNull String alias, @NotNull String[] args) {
        return new ArrayList<>();
    }
}
