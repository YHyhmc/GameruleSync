package com.yuehuan.gameruleSync.listener;

import com.yuehuan.gameruleSync.manager.SyncManager;
import io.papermc.paper.event.world.WorldGameRuleChangeEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.world.WorldEvent;
import org.bukkit.event.Listener;

/**
 * 仅在 Paper 环境下通过反射加载注册，避免 Spigot 缺少该类导致 NoClassDefFoundError。
 */
public class PaperGameRuleListener implements Listener {

    private final SyncManager syncManager;

    public PaperGameRuleListener(SyncManager syncManager) {
        this.syncManager = syncManager;
    }

    @EventHandler
    public void onGameRuleChange(WorldGameRuleChangeEvent event) {
        syncManager.handleGameRuleChange(
                event.getWorld(),
                event.getGameRule(),
                event.getValue(),
                event.getCommandSender());
    }
}

