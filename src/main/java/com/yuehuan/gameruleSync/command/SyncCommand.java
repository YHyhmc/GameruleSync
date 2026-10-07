package com.yuehuan.gameruleSync.command;

import com.yuehuan.gameruleSync.GameruleSync;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class SyncCommand implements CommandExecutor, TabCompleter {

    private final GameruleSync plugin;

    public SyncCommand(GameruleSync plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            plugin.getSyncManager().reload(sender);
            plugin.getMuteManager().reload();
            return true;
        }
        plugin.getSyncManager().syncAllRules(sender);
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            String currentArg = args[0].toLowerCase();
            List<String> completions = new ArrayList<>();
            if ("reload".startsWith(currentArg)) {
                completions.add("reload");
            }
            return completions;
        }
        return new ArrayList<>();
    }
}
