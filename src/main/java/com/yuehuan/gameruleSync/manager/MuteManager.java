package com.yuehuan.gameruleSync.manager;

import com.yuehuan.gameruleSync.GameruleSync;
import org.bukkit.Bukkit;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MuteManager {

    private static final String OBJECTIVE_NAME = "BAMBOO_MOD_SAYING";

    public static final int FLAG_SPEAK = 1;
    public static final int FLAG_SHOUT = 2;
    public static final int FLAG_ALL = FLAG_SPEAK | FLAG_SHOUT;

    private final GameruleSync plugin;
    private Scoreboard mainScoreboard;
    private Objective muteObjective;

    private boolean teamChatEnabled = true;

    public MuteManager(GameruleSync plugin) {
        this.plugin = plugin;
        loadConfig();
        initScoreboard();
    }

 

    private void loadConfig() {
        try {
            File configFile = new File(plugin.getDataFolder(), "config.yml");
            if (!configFile.exists()) {
                plugin.saveDefaultConfig();
            }
            FileConfiguration config = new YamlConfiguration();
            config.load(configFile);
            teamChatEnabled = config.getBoolean("mute.team-chat-enabled", true);
        } catch (InvalidConfigurationException e) {
            plugin.getLogger().warning("config.yml 禁言配置格式错误: " + e.getMessage());
            teamChatEnabled = true;
        } catch (Exception e) {
            plugin.getLogger().warning("加载禁言配置时发生错误，使用默认值: " + e.getMessage());
            teamChatEnabled = true;
        }
    }


    private void initScoreboard() {
        try {
            if (Bukkit.getScoreboardManager() == null) {
                plugin.getLogger().warning("ScoreboardManager 不可用，禁言功能暂不可用。请执行 /syncgamerules reload 重试。");
                return;
            }
            mainScoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
            muteObjective = mainScoreboard.getObjective(OBJECTIVE_NAME);
            if (muteObjective == null) {
                muteObjective = mainScoreboard.registerNewObjective(OBJECTIVE_NAME, "dummy", "Mute");
                plugin.getLogger().info("已创建禁言计分板目标: " + OBJECTIVE_NAME);
            }
        } catch (Exception e) {
            plugin.getLogger().severe("初始化禁言计分板失败: " + e.getMessage());
        }
    }


    public int getMuteFlags(String playerName) {
        if (muteObjective == null) return 0;
        try {
            return muteObjective.getScore(playerName).getScore();
        } catch (Exception e) {
            return 0;
        }
    }

    public boolean isMutedFromSpeaking(Player player) {
        return (getMuteFlags(player.getName()) & FLAG_SPEAK) != 0;
    }

    public boolean isMutedFromShouting(Player player) {
        return (getMuteFlags(player.getName()) & FLAG_SHOUT) != 0;
    }



    public void setMute(String playerName, int flags) {
        if (muteObjective == null) return;
        try {
            muteObjective.getScore(playerName).setScore(flags);
        } catch (Exception e) {
            plugin.getLogger().warning("设置禁言状态失败: " + playerName + " - " + e.getMessage());
        }
    }

    public void mute(String playerName, int flags) {
        setMute(playerName, getMuteFlags(playerName) | flags);
    }

    public void unmute(String playerName, int flags) {
        setMute(playerName, getMuteFlags(playerName) & ~flags);
    }

    public void muteTeam(String teamName, int flags) {
        Team team = getTeamByName(teamName);
        if (team == null) return;
        for (String entry : team.getEntries()) {
            mute(entry, flags);
        }
    }

    public void unmuteTeam(String teamName, int flags) {
        Team team = getTeamByName(teamName);
        if (team == null) return;
        for (String entry : team.getEntries()) {
            unmute(entry, flags);
        }
    }


    public Team getTeam(Player player) {
        if (mainScoreboard == null) return null;
        return mainScoreboard.getEntryTeam(player.getName());
    }

    public Team getTeamByName(String teamName) {
        if (mainScoreboard == null) return null;
        return mainScoreboard.getTeam(teamName);
    }

    public void sendToTeam(Team team, String message) {
        for (String entry : team.getEntries()) {
            Player member = Bukkit.getPlayerExact(entry);
            if (member != null) {
                member.sendMessage(message);
            }
        }
        Bukkit.getConsoleSender().sendMessage(message);
    }

    public List<String> getTeamNames() {
        if (mainScoreboard == null) return new ArrayList<>();
        List<String> names = new ArrayList<>();
        for (Team team : mainScoreboard.getTeams()) {
            names.add(team.getName());
        }
        return names;
    }

  

    public boolean isTeamChatEnabled() {
        return teamChatEnabled;
    }

 

    public String msg(String key, Object... args) {
        return plugin.getSyncManager().getMessage(key, args);
    }

    public String formatTeamMessage(String playerName, String content) {
        return msg("mute.team_chat_format", playerName, content);
    }

    public String formatShoutMessage(String playerName, String content) {
        return msg("mute.shout_format", playerName, content);
    }

    public String flagsToMessage(int flags) {
        if ((flags & FLAG_ALL) == FLAG_ALL) return msg("mute.status_all");
        if ((flags & FLAG_SPEAK) != 0) return msg("mute.status_speak");
        if ((flags & FLAG_SHOUT) != 0) return msg("mute.status_shout");
        return msg("mute.status_none");
    }


    public void reload() {
        loadConfig();
        initScoreboard();
    }


    public static int parseFlags(String type) {
        if (type == null) return FLAG_ALL;
        switch (type.toLowerCase()) {
            case "speak": return FLAG_SPEAK;
            case "shout": return FLAG_SHOUT;
            case "all": return FLAG_ALL;
            default: return FLAG_ALL;
        }
    }
}
