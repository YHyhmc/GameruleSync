package com.yuehuan.gameruleSync.listener;

import com.yuehuan.gameruleSync.manager.MuteManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.scoreboard.Team;

public class ChatListener implements Listener {

    private final MuteManager muteManager;

    public ChatListener(MuteManager muteManager) {
        this.muteManager = muteManager;
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();

        if (muteManager.isMutedFromSpeaking(player)) {
            event.setCancelled(true);
            if (muteManager.isMutedFromShouting(player)) {
                player.sendMessage(muteManager.msg("mute.cannot_speak_or_shout"));
            } else {
                player.sendMessage(muteManager.msg("mute.cannot_speak"));
                player.sendMessage(muteManager.msg("mute.try_sshout"));
            }
            return;
        }

        if (muteManager.isTeamChatEnabled()) {
            Team team = muteManager.getTeam(player);
            if (team != null) {
                event.setCancelled(true);
                String message = muteManager.formatTeamMessage(player.getName(), event.getMessage());
                muteManager.sendToTeam(team, message);
            }
        }
    }
}
