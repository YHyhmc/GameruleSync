package com.yuehuan.gameruleSync.manager;

import com.yuehuan.gameruleSync.GameruleSync;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

public class SyncManager {

    private final GameruleSync plugin;
    private final AtomicInteger syncDepth = new AtomicInteger(0);
    private static final int MAX_SYNC_DEPTH = 3;

    private BukkitTask syncTask = null;

    private String sourceWorldName = null;
    private boolean eventSyncEnabled = true;
    private long syncIntervalTicks = 0;

    private FileConfiguration messages = null;

    public SyncManager(GameruleSync plugin) {
        this.plugin = plugin;
        loadMessages();
        loadAndApplyConfig();

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            plugin.getLogger().info(getMessage("log.startup.sync"));
            syncAllRules(null);
        }, 20L);

        scheduleSyncTask();
    }


    private void loadMessages() {
        File messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        messages = new YamlConfiguration();
        try {
            messages.load(messagesFile);
            if (messages.getKeys(false).isEmpty()) {
                plugin.getLogger().warning("messages.yml 加载后为空，可能格式错误，使用内置默认消息。");
                messages = null;
            }
        } catch (InvalidConfigurationException e) {
            plugin.getLogger().severe("messages.yml 格式错误，请检查 YAML 语法！错误信息: " + e.getMessage());
            plugin.getLogger().severe("将使用内置默认消息，请修复后执行 /syncgamerules reload 重载。");
            messages = null;
        } catch (Exception e) {
            plugin.getLogger().warning("加载 messages.yml 时发生未知错误: " + e.getMessage());
            messages = null;
        }
    }

    public String getMessage(String key, Object... args) {
        String msg = null;
        if (messages != null) {
            msg = messages.getString(key);
        }
        if (msg == null) {
            msg = fallbackMessage(key);
        }
        msg = msg.replace('&', '\u00a7');
        for (int i = 0; i < args.length; i++) {
            String placeholder = "{" + i + "}";
            String value = args[i] != null ? args[i].toString() : "null";
            msg = msg.replace(placeholder, value);
        }
        return msg;
    }

    private String fallbackMessage(String key) {
        switch (key) {
            case "command.sync.start": return "&a正在执行全量同步...";
            case "command.sync.success": return "&a[全量同步完成]";
            case "command.sync.success.worlds": return "&e- 成功同步: &f{0}";
            case "command.sync.failed": return "&c- 失败/跳过: &f{0}";
            case "command.sync.in_progress": return "&e同步任务正在进行中，请稍后...";
            case "command.sync.error.no_worlds": return "&c错误：服务器未加载任何世界。";
            case "command.sync.error.no_source": return "&c错误：无法获取权威源世界（请检查配置 source-world 或默认主世界）。";
            case "command.sync.error.source_no_rules": return "&e权威源世界无游戏规则可同步。";
            case "command.sync.error.recursion": return "&c错误：检测到递归深度异常 ({0})，本次同步已取消。";
            case "command.reload.success": return "&a[GameruleSync] 配置已重载，定时任务已更新。";
            case "command.reload.error": return "&c重载配置时发生错误，请检查日志。";
            case "log.startup.sync": return "执行启动全量同步...";
            case "log.sync.complete": return "全量同步结束。成功: {0}, 失败/跳过: {1}";
            case "log.sync.scheduled": return "定时全量同步已启动，间隔: {0} 秒";
            case "log.sync.scheduled.disabled": return "定时全量同步已禁用 (sync-interval-seconds <= 0)";
            case "log.sync.world_skipped": return "跳过未加载的世界: {0}";
            case "log.sync.world_success": return "全量同步成功: {0}";
            case "log.sync.world_failed": return "世界 {0} 同步存在部分失败。";
            case "log.sync.rule_failed": return "同步规则 '{0}' 到 '{1}' 失败: {2}";
            case "log.sync.rule_warning": return "实时同步规则 '{0}' 到 '{1}' 失败: {2}";
            case "log.sync.no_rules": return "权威源世界无游戏规则可同步。";
            case "log.event.ignored_recursion": return "检测到疑似递归循环！当前同步深度已达 {0}，超过最大允许值 {1}，本次事件将被忽略。";
            case "log.event.ignored_recursion_detail": return "事件来源世界: {0}, 规则: {1}, 执行者: {2}";
            case "log.warning.source_not_found": return "配置的权威源世界 '{0}' 不存在，回退到 worlds.get(0)。";
            case "log.warning.no_authority": return "无法获取权威源世界，事件同步中止。";
            case "log.warning.event_sync_unavailable": return "当前服务器不支持 Paper 的 WorldGameRuleChangeEvent（Spigot 环境），实时事件同步已禁用，仅使用定时/手动全量同步。";
            case "log.info.enabled": return "GameruleSync 已启用。使用 /syncgamerules reload 重载配置。";
            case "log.info.disabled": return "GameruleSync 已卸载。";
            case "log.info.reload": return "配置已重载。";
            case "log.info.paper_listener_registered": return "检测到 Paper，已注册 WorldGameRuleChangeEvent 实时同步监听器。";
            case "log.info.event_sync_disabled": return "配置已禁用事件实时同步 (event-sync-enabled: false)，仅使用定时/手动全量同步。";
            case "log.error.config_load": return "加载 config.yml 时发生错误，使用默认配置。错误: {0}";
            case "mute.cannot_speak": return "&c你现在不能说话。";
            case "mute.cannot_shout": return "&c你现在不能喊话。";
            case "mute.cannot_speak_or_shout": return "&c你现在不能说话或喊话。";
            case "mute.try_sshout": return "&7请改用命令: /sshout <内容>";
            case "mute.sshout_usage": return "&7用法: /sshout <内容>";
            case "mute.player_only": return "&c只有玩家才能使用此命令。";
            case "mute.mute_success": return "&a已禁言 &f{0} &a（{1}）";
            case "mute.unmute_success": return "&a已解除 &f{0} &a的禁言（{1}）";
            case "mute.team_mute_success": return "&a已禁言队伍 &f{0} &a的所有成员（{1}）";
            case "mute.team_unmute_success": return "&a已解除队伍 &f{0} &a的所有成员的禁言（{1}）";
            case "mute.status_format": return "&e{0} &f的禁言状态: {1}";
            case "mute.status_speak": return "&c禁言说话";
            case "mute.status_shout": return "&c禁言喊话";
            case "mute.status_all": return "&c全部禁言";
            case "mute.status_none": return "&a未禁言";
            case "mute.player_not_found": return "&c未找到玩家: {0}";
            case "mute.team_not_found": return "&c未找到队伍: {0}";
            case "mute.player_offline": return "&7注意: 玩家 {0} 当前不在线，已按名称设置禁言。";
            case "mute.no_muted_players": return "&e当前没有被禁言的在线玩家。";
            case "mute.muted_list_header": return "&e===== 被禁言的在线玩家 =====";
            case "mute.muted_list_entry": return "&f{0} &7- {1}";
            case "mute.usage": return "&7用法: /MuteByCmd <玩家> [speak|shout|all] | /MuteByCmd team <队伍> [speak|shout|all] | /MuteByCmd status [玩家]";
            case "mute.team_usage": return "&7用法: /MuteByCmd team <队伍> [speak|shout|all]";
            case "mute.team_chat_format": return "&e[TEAM] &f{0}&7: &r{1}";
            case "mute.shout_format": return "&6[SHOUT] &f{0}&7: &r{1}";
            default: return "Missing message: " + key;
        }
    }


    private void loadAndApplyConfig() {
        try {
            File configFile = new File(plugin.getDataFolder(), "config.yml");
            if (!configFile.exists()) {
                plugin.saveDefaultConfig();
            }
            FileConfiguration config = new YamlConfiguration();
            config.load(configFile);

            sourceWorldName = config.getString("source-world", null);
            eventSyncEnabled = config.getBoolean("event-sync-enabled", true);
            int seconds = config.getInt("sync-interval-seconds", 0);
            syncIntervalTicks = seconds * 20L;
            if (syncIntervalTicks < 0) syncIntervalTicks = 0;
        } catch (InvalidConfigurationException e) {
            plugin.getLogger().severe("config.yml 格式错误，请检查 YAML 语法！错误信息: " + e.getMessage());
            plugin.getLogger().severe("将使用默认配置值，请修复后执行 /syncgamerules reload 重载。");
            sourceWorldName = null;
            eventSyncEnabled = true;
            syncIntervalTicks = 0;
        } catch (Exception e) {
            plugin.getLogger().warning("加载 config.yml 时发生未知错误: " + e.getMessage());
            sourceWorldName = null;
            eventSyncEnabled = true;
            syncIntervalTicks = 0;
        }
    }

    public boolean isEventSyncEnabled() {
        return eventSyncEnabled;
    }


    public void reload(CommandSender sender) {
        try {
            loadMessages();
            loadAndApplyConfig();
            scheduleSyncTask();
            sender.sendMessage(getMessage("command.reload.success"));
            plugin.getLogger().info(getMessage("log.info.reload"));
        } catch (Exception e) {
            sender.sendMessage(getMessage("command.reload.error"));
            plugin.getLogger().warning("重载过程中发生异常: " + e.getMessage());
        }
    }


    private void scheduleSyncTask() {
        if (syncTask != null) {
            syncTask.cancel();
            syncTask = null;
        }
        if (syncIntervalTicks > 0) {
            syncTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
                Bukkit.getScheduler().runTask(plugin, () -> syncAllRules(null));
            }, syncIntervalTicks, syncIntervalTicks);
            plugin.getLogger().info(getMessage("log.sync.scheduled", syncIntervalTicks / 20));
        } else {
            plugin.getLogger().info(getMessage("log.sync.scheduled.disabled"));
        }
    }


    public void handleGameRuleChange(World sourceWorld, GameRule<?> rule, Object newValue, CommandSender sender) {
        if (!eventSyncEnabled) return;

        int currentDepth = syncDepth.incrementAndGet();
        try {
            if (currentDepth > MAX_SYNC_DEPTH) {
                plugin.getLogger().severe(getMessage("log.event.ignored_recursion", currentDepth, MAX_SYNC_DEPTH));
                plugin.getLogger().severe(getMessage("log.event.ignored_recursion_detail",
                        sourceWorld != null ? sourceWorld.getName() : "null",
                        rule != null ? rule.getKey().getKey() : "null",
                        sender != null ? sender.getName() : "unknown"));
                return;
            }

            if (sourceWorld == null || rule == null || newValue == null) return;

            World authorityWorld = getAuthorityWorld();
            if (authorityWorld == null) {
                plugin.getLogger().warning(getMessage("log.warning.no_authority"));
                return;
            }

            if (!sourceWorld.equals(authorityWorld)) {
                if (plugin.getLogger().isLoggable(Level.FINE)) {
                    plugin.getLogger().fine("忽略子世界规则变更: " + sourceWorld.getName() +
                            " -> " + rule.getKey().getKey() + " = " + newValue +
                            " (执行者: " + sender + ")");
                }
                return;
            }

            plugin.getLogger().info("世界规则变更: " + rule.getKey().getKey() + " = " + newValue +
                    " (执行者: " + sender + ")，正在广播...");

            for (World targetWorld : Bukkit.getWorlds()) {
                if (targetWorld.equals(authorityWorld)) continue;
                try {
                    setGameRuleValue(targetWorld, rule, newValue);
                } catch (Exception e) {
                    plugin.getLogger().warning(getMessage("log.sync.rule_warning",
                            rule.getKey().getKey(), targetWorld.getName(), e.getMessage()));
                }
            }
        } finally {
            int remaining = syncDepth.decrementAndGet();
            if (remaining < 0) syncDepth.set(0);
        }
    }


    public void syncAllRules(CommandSender sender) {
        int currentDepth = syncDepth.incrementAndGet();
        try {
            if (currentDepth > MAX_SYNC_DEPTH) {
                if (sender != null) {
                    sender.sendMessage(getMessage("command.sync.error.recursion", currentDepth));
                }
                plugin.getLogger().severe(getMessage("log.event.ignored_recursion", currentDepth, MAX_SYNC_DEPTH));
                return;
            }

            World authority = getAuthorityWorld();
            if (authority == null) {
                if (sender != null) sender.sendMessage(getMessage("command.sync.error.no_source"));
                plugin.getLogger().severe(getMessage("log.warning.no_authority"));
                return;
            }

            List<World> worlds = Bukkit.getWorlds();
            if (worlds.isEmpty()) {
                if (sender != null) sender.sendMessage(getMessage("command.sync.error.no_worlds"));
                return;
            }

            String[] ruleNames = authority.getGameRules();
            if (ruleNames == null || ruleNames.length == 0) {
                if (sender != null) sender.sendMessage(getMessage("command.sync.error.source_no_rules"));
                plugin.getLogger().warning(getMessage("log.sync.no_rules"));
                return;
            }

            List<GameRule<?>> rules = new ArrayList<>();
            for (String name : ruleNames) {
                GameRule<?> rule = GameRule.getByName(name);
                if (rule != null) {
                    rules.add(rule);
                } else {
                    plugin.getLogger().warning("忽略未知游戏规则: " + name);
                }
            }

            if (rules.isEmpty()) {
                if (sender != null) sender.sendMessage(getMessage("command.sync.error.source_no_rules"));
                plugin.getLogger().warning(getMessage("log.sync.no_rules"));
                return;
            }

            int synced = 0, failed = 0;
            for (World target : worlds) {
                if (target.equals(authority)) continue;

                boolean worldOk = true;
                for (GameRule<?> rule : rules) {
                    try {
                        Object value = authority.getGameRuleValue(rule);
                        if (value == null) continue;
                        setGameRuleValue(target, rule, value);
                    } catch (Exception e) {
                        plugin.getLogger().warning(getMessage("log.sync.rule_failed",
                                rule.getKey().getKey(), target.getName(), e.getMessage()));
                        worldOk = false;
                    }
                }
                if (worldOk) {
                    synced++;
                    plugin.getLogger().info(getMessage("log.sync.world_success", target.getName()));
                } else {
                    failed++;
                    plugin.getLogger().warning(getMessage("log.sync.world_failed", target.getName()));
                }
            }

            if (sender != null) {
                sender.sendMessage(getMessage("command.sync.success"));
                sender.sendMessage(getMessage("command.sync.success.worlds", synced));
                sender.sendMessage(getMessage("command.sync.failed", failed));
            }
            plugin.getLogger().info(getMessage("log.sync.complete", synced, failed));
        } finally {
            int remaining = syncDepth.decrementAndGet();
            if (remaining < 0) syncDepth.set(0);
        }
    }


    private World getAuthorityWorld() {
        if (sourceWorldName != null && !sourceWorldName.isEmpty()) {
            World w = Bukkit.getWorld(sourceWorldName);
            if (w != null) return w;
            plugin.getLogger().warning(getMessage("log.warning.source_not_found", sourceWorldName));
        }
        List<World> worlds = Bukkit.getWorlds();
        return worlds.isEmpty() ? null : worlds.get(0);
    }

    private <T> void setGameRuleValue(World world, GameRule<T> rule, Object value) {
        Class<T> type = rule.getType();
        T typedValue;
        if (type.isInstance(value)) {
            typedValue = type.cast(value);
        } else if (value instanceof String str) {
            Object converted;
            if (type == Boolean.class) {
                converted = Boolean.valueOf(str.trim());
            } else if (type == Integer.class) {
                try {
                    converted = Integer.valueOf(str.trim());
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("无法将 '" + str + "' 转换为整数", e);
                }
            } else {
                throw new IllegalArgumentException("不支持的规则类型: " + type.getSimpleName());
            }
            typedValue = type.cast(converted);
        } else {
            throw new IllegalArgumentException("值类型不匹配: 期望 " + type.getSimpleName()
                    + "，实际 " + (value != null ? value.getClass().getSimpleName() : "null"));
        }
        world.setGameRule(rule, typedValue);
    }


    public void shutdown() {
        if (syncTask != null) {
            syncTask.cancel();
            syncTask = null;
        }
    }
}
