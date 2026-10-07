package com.yuehuan.gameruleSync;

import com.yuehuan.gameruleSync.command.MuteCommand;
import com.yuehuan.gameruleSync.command.ShoutCommand;
import com.yuehuan.gameruleSync.command.SyncCommand;
import com.yuehuan.gameruleSync.listener.ChatListener;
import com.yuehuan.gameruleSync.manager.MuteManager;
import com.yuehuan.gameruleSync.manager.SyncManager;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public final class GameruleSync extends JavaPlugin {

    private SyncManager syncManager;
    private MuteManager muteManager;

    @Override
    public void onEnable() {
        syncManager = new SyncManager(this);
        muteManager = new MuteManager(this);

        getCommand("syncgamerules").setExecutor(new SyncCommand(this));
        getCommand("MuteByCmd").setExecutor(new MuteCommand(muteManager));
        getCommand("sshout").setExecutor(new ShoutCommand(muteManager));

        registerEventSync();
        getServer().getPluginManager().registerEvents(new ChatListener(muteManager), this);

        getLogger().info(syncManager.getMessage("log.info.enabled"));
    }

    private void registerEventSync() {
        if (!syncManager.isEventSyncEnabled()) {
            getLogger().info(syncManager.getMessage("log.info.event_sync_disabled"));
            return;
        }
        try {
            Class.forName("io.papermc.paper.event.world.WorldGameRuleChangeEvent");
            Class<?> listenerClass = Class.forName("com.yuehuan.gameruleSync.listener.PaperGameRuleListener");
            Object listener = listenerClass.getConstructor(SyncManager.class).newInstance(syncManager);
            getServer().getPluginManager().registerEvents((Listener) listener, this);
            getLogger().info(syncManager.getMessage("log.info.paper_listener_registered"));
        } catch (ClassNotFoundException e) {
            getLogger().warning(syncManager.getMessage("log.warning.event_sync_unavailable"));
        } catch (Exception e) {
            getLogger().warning("注册 Paper 事件监听器失败: " + e.getMessage() + "，实时事件同步已禁用。");
        }
    }

    @Override
    public void onDisable() {
        if (syncManager != null) {
            syncManager.shutdown();
        }
        getLogger().info("GameruleSync 已卸载。");
    }

    public SyncManager getSyncManager() {
        return syncManager;
    }

    public MuteManager getMuteManager() {
        return muteManager;
    }
}
